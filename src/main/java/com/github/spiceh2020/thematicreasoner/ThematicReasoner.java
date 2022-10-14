package com.github.spiceh2020.thematicreasoner;

import it.cnr.istc.stlab.rocksmap.RocksMultiMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.*;

public class ThematicReasoner {

    public final static String THEME_ONTOLOGY_NS = "https://w3id.org/spice/SON/theme/";
    private static final Logger logger = LoggerFactory.getLogger(ThematicReasoner.class);
    //@f:off
    private static final String SELECT_LOC = ""
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
            + "PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> "
            + "SELECT DISTINCT ?loc (GROUP_CONCAT(?cp) AS ?cps) {"
            + " ?loc DUL:isLocationOf ?cp  } GROUP BY ?loc";
    //@f:off
    private static final String SELECT_LOC_BY_ZONE = ""
            + "PREFIX bot: <https://w3id.org/bot#> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
            + "PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> "
            + "SELECT DISTINCT ?zone (GROUP_CONCAT(?loc) AS ?locs) {"
            + " ?zone bot:containsZone+ ?loc . "
            + " ?loc DUL:isLocationOf ?cp  } GROUP BY ?zone";
    private final String[] ontologiesToLoad = {"https://w3id.org/arco/ontology/arco"};
    private final Property score = ModelFactory.createDefaultModel().createProperty(THEME_ONTOLOGY_NS + "score");
    private final Property hasWeightedTheme = ModelFactory.createDefaultModel()
            .createProperty(THEME_ONTOLOGY_NS + "hasWeightedTheme");
    private final Property hasTheme = ModelFactory.createDefaultModel().createProperty(THEME_ONTOLOGY_NS + "hasTheme");
    private final Model model;
    private final Query selectEntitiesQuery;
    private final ThemeDetector themeDetector;
    private Map<String, Map<String, Integer>> location2topicMap;
    private Map<String, Map<String, Integer>> zone2topicMap;
    private Map<String, Map<String, Integer>> entityToTopicMap;
    private String baseURI = "https://w3id.org/spice/";
    private Map<String, Set<String>> entityToTopics;
    private Map<String, Set<String>> entityToAssociatedEntity;

    public ThematicReasoner(Model input, Query query, ThemeDetector themeDetector) {
        super();
        this.model = input;
        this.selectEntitiesQuery = query;
        this.themeDetector = themeDetector;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public void run() throws FileNotFoundException {

        for (String ontology : ontologiesToLoad) {
            RDFDataMgr.read(model, ontology, Lang.RDFXML);
        }

        logger.info("Loaded Model Size {}", model.size());
        logger.trace("Query for selecting entities and associated entities {}", selectEntitiesQuery.toString(Syntax.defaultSyntax));
        computeEntityToAssociatedEntity();
        computeEntityToTopicMap();
        computeLocationToTopicMap();
        computeZoneToTopicMap();
    }

    public Model getOutModel(RocksMultiMap categoryLabelMap) {
        logger.info("Out RDF");
        Model outmodel = ModelFactory.createDefaultModel();

        // Write topics associated with CPs
        for (String cp : entityToAssociatedEntity.keySet()) {
            Map<String, Integer> topicMap = entityToTopicMap.get(cp);
            writeTopicMapForEntity(outmodel, cp, topicMap, categoryLabelMap);

        }

        location2topicMap.forEach((loc, tm) -> {
            writeTopicMapForEntity(outmodel, loc, tm, categoryLabelMap);
        });


        zone2topicMap.forEach((zone, tm) -> {
            writeTopicMapForEntity(outmodel, zone, tm, categoryLabelMap);
        });

        return outmodel;

    }

    private void computeZoneToTopicMap() {
        zone2topicMap = new HashMap<>();
        ResultSet locsByZones = QueryExecutionFactory.create(SELECT_LOC_BY_ZONE, model).execSelect();
        while (locsByZones.hasNext()) {
            QuerySolution qs = locsByZones.next();
            Map<String, Integer> topicMapForZone = new HashMap<>();
            String zone = qs.get("zone").asResource().getURI();
            for (String loc : qs.get("locs").asLiteral().getValue().toString().split(" ")) {
                Map<String, Integer> topicMap = location2topicMap.get(loc);
                if (topicMap != null) {
                    incrementMapBy(topicMapForZone, topicMap);
                }
            }
//			writeTopicMapForEntity(outmodel, zone, topicMapForZone);
            zone2topicMap.put(zone, topicMapForZone);

        }
    }

    private void computeLocationToTopicMap() {
        ResultSet rsLocGroups = QueryExecutionFactory.create(SELECT_LOC, model).execSelect();
        location2topicMap = new HashMap<>();

        while (rsLocGroups.hasNext()) {
            QuerySolution qs = rsLocGroups.next();
            String loc = qs.get("loc").asResource().getURI();
            String cps = qs.get("cps").asLiteral().getValue().toString();

            Map<String, Integer> topic2scoreForLocation = new HashMap<>();
            for (String cp : cps.split(" ")) {
                Map<String, Integer> topicMap = entityToTopicMap.get(cp);
                if (topicMap != null) {
                    incrementMapBy(topic2scoreForLocation, topicMap);
                }
            }

//			writeTopicMapForEntity(outmodel, loc, topic2scoreForLocation);

            location2topicMap.put(loc, topic2scoreForLocation);

        }
    }

    private void computeEntityToTopicMap() {
        entityToTopicMap = new HashMap<>();
        for (String cp : entityToAssociatedEntity.keySet()) {
            Map<String, Integer> topicMap = new HashMap<>();
            for (String associatedEntity : entityToAssociatedEntity.get(cp)) {
                for (String topic : entityToTopics.get(associatedEntity)) {
                    Integer score = topicMap.get(topic);
                    if (score == null) {
                        score = 0;
                    }
                    score++;
                    topicMap.put(topic, score);
                }
            }
            entityToTopicMap.put(cp, topicMap);
        }
    }

    private void computeEntityToAssociatedEntity() {
//        maximalSetOfTopics = new HashSet<>();
        entityToTopics = new HashMap<>();
        QueryExecution qexec = QueryExecutionFactory.create(selectEntitiesQuery, model);
        ResultSet rs = qexec.execSelect();
        entityToAssociatedEntity = new HashMap<>();

        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String associatedEntity = qs.get("associatedEntity").asResource().getURI();
            String entity = qs.get("entity").asResource().getURI();

            logger.trace("Entity {} Associated Entity {} ", entity, associatedEntity);

            Set<String> entityAssociations = entityToAssociatedEntity.get(entity);
            if (entityAssociations == null) {
                entityAssociations = new HashSet<>();
            }

            entityAssociations.add(associatedEntity);
            entityToAssociatedEntity.put(entity, entityAssociations);

            Set<String> topics = themeDetector.detectTopics(associatedEntity);
//            maximalSetOfTopics.addAll(topics);
            entityToTopics.put(associatedEntity, topics);
        }
        qexec.close();
    }

    void incrementMapBy(Map<String, Integer> topic2scoreForLocation, Map<String, Integer> topicMap) {
        topicMap.forEach((t, s) -> {
            Integer score = topic2scoreForLocation.get(t);
            if (score == null) {
                score = s;
            } else {
                score += s;
            }
            topic2scoreForLocation.put(t, score);
        });
    }

    void writeTopicMapForEntity(Model outmodel, String entity, Map<String, Integer> topic2scoreForLocation, RocksMultiMap categoryLabelMap) {
        for (String topic : topic2scoreForLocation.keySet()) {
            String weightedThemeURI = baseURI + DigestUtils.md5Hex(entity + topic);
            outmodel.add(outmodel.createResource(entity), hasTheme, outmodel.createResource(topic));
            outmodel.add(outmodel.createResource(entity), hasWeightedTheme, outmodel.createResource(weightedThemeURI));
            outmodel.add(outmodel.createResource(weightedThemeURI), hasTheme, outmodel.createResource(topic));
            outmodel.add(outmodel.createResource(weightedThemeURI), score,
                    outmodel.createTypedLiteral(topic2scoreForLocation.get(topic)));
            Collection<String> labels = categoryLabelMap.get(topic);
            if(labels!=null){
                for(String label: labels){
                    outmodel.add(outmodel.createResource(topic), RDFS.label, label);
                }
            }
        }
    }

    public static void printTopicMap(Map<String, Integer> topicMap, String prefix) {
        if (prefix != null) {
            topicMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
                System.out.println(prefix + e.getKey() + " " + e.getValue());
            });
        } else {
            topicMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
                System.out.println(e.getKey() + " " + e.getValue());
            });
        }
    }


    public static void printEntityToTopicMap(Map<String, Map<String, Integer>> entityToTopicMap) {
        entityToTopicMap.forEach((k,v)-> {
            System.out.println(k);
            printTopicMap(v, "\t");
        });
    }

    public static String entityToTopicMapToString(Map<String, Map<String, Integer>> entityToTopicMap) {
        StringBuffer sb = new StringBuffer();
        entityToTopicMap.forEach((k,v)-> {
            sb.append(k);
            sb.append("\n");
            sb.append(topicMapToString(v, "\t"));
            sb.append("\n");
        });
        return sb.toString();
    }

    public static String topicMapToString(Map<String, Integer> topicMap, String prefix) {
        StringBuffer sb = new StringBuffer();
        if (prefix != null) {
            topicMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
                sb.append(prefix + e.getKey() + " " + e.getValue());
                sb.append("\n");
            });
        } else {
            topicMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e -> {
                sb.append(e.getKey() + " " + e.getValue());
                sb.append("\n");
            });
        }
        return  sb.toString();
    }

    public Map<String, Map<String, Integer>> getEntityToTopicMap() {
        return entityToTopicMap;
    }

    public enum OutputStrategy {
        PRINT, RDF
    }
}