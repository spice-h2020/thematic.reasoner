package com.github.spiceh2020.thematicreasoner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThematicReasoner {

	private String input, baseURI;
	private String[] ontologiesToLoad = { "https://w3id.org/arco/ontology/arco" };
	public final static String THEME_ONTOLOGY_NS = "https://w3id.org/spice/SON/theme/";
	private Property score = ModelFactory.createDefaultModel().createProperty(THEME_ONTOLOGY_NS + "score");
	private Property hasWeightedTheme = ModelFactory.createDefaultModel()
			.createProperty(THEME_ONTOLOGY_NS + "hasWeightedTheme");
	private Property hasTheme = ModelFactory.createDefaultModel().createProperty(THEME_ONTOLOGY_NS + "hasTheme");
	private String serialization = "TTL";

	public enum OutputStrategy {
		PRINT, RDF
	}

	private String outputFile = "out.ttl";

	private OutputStrategy outputStrategy = OutputStrategy.PRINT;

	private static Logger logger = LoggerFactory.getLogger(ThematicReasoner.class);

	//@f:off
	private static final String SELECT_CP = ""
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> "
			+ "SELECT DISTINCT ?ass ?cp {"
			+ " ?cp a/rdfs:subClassOf* <https://w3id.org/arco/ontology/arco/CulturalProperty> ; "
			+ "   DUL:associatedWith ?ass . "
			+ "FILTER (strStarts(str(?ass), \"http://dbpedia\")) }";
	//@f:on

	//@f:off
	private static final String SELECT_LOC = ""
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> "
				+ "SELECT DISTINCT ?loc (GROUP_CONCAT(?cp) AS ?cps) {"
				+ " ?loc DUL:isLocationOf ?cp  } GROUP BY ?loc";
	//@f:on

	//@f:off
	private static final String SELECT_LOC_BY_ZONE = ""
				+ "PREFIX bot: <https://w3id.org/bot#> "
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> "
				+ "SELECT DISTINCT ?zone (GROUP_CONCAT(?loc) AS ?locs) {"
				+ " ?zone bot:containsZone+ ?loc . "
				+ " ?loc DUL:isLocationOf ?cp  } GROUP BY ?zone";
	//@f:on

	public ThematicReasoner(String input) {
		super();
		this.input = input;
	}

	public OutputStrategy getOutputStrategy() {
		return outputStrategy;
	}

	public void setOutputStrategy(OutputStrategy outputStrategy) {
		this.outputStrategy = outputStrategy;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public String getSerialization() {
		return serialization;
	}

	public void setSerialization(String serialization) {
		this.serialization = serialization;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	public void run() throws FileNotFoundException {

		Model m = ModelFactory.createDefaultModel();
		RDFDataMgr.read(m, input);
		for (String ontology : ontologiesToLoad) {
			RDFDataMgr.read(m, ontology, Lang.RDFXML);
		}

		logger.info("Loaded Model Size {}", m.size());

		QueryExecution qexec = QueryExecutionFactory.create(SELECT_CP, m);

		ResultSet rs = qexec.execSelect();

		Set<String> maximalSetOfTopics = new HashSet<>();

		Map<String, Set<String>> entityToTopics = new HashMap<>();

		DBPediaBasedThemeDetector td = new DBPediaBasedThemeDetector();
		Map<String, Set<String>> associations = new HashMap<>();

		while (rs.hasNext()) {
			QuerySolution qs = (QuerySolution) rs.next();
			String ass = qs.get("ass").asResource().getURI();
			String cp = qs.get("cp").asResource().getURI();

			Set<String> cpAssociations = associations.get(cp);
			if (cpAssociations == null) {
				cpAssociations = new HashSet<>();
			}

			cpAssociations.add(ass);
			associations.put(cp, cpAssociations);

			Set<String> r = td.detectTopics(ass);
			maximalSetOfTopics.addAll(r);
			entityToTopics.put(ass, r);
		}
		qexec.close();

		Map<String, Integer> allEntitiesWeight = getTopicsMap(entityToTopics, entityToTopics.keySet());

		switch (outputStrategy) {

		case PRINT:
			logger.info("Printing");
			allEntitiesWeight.entrySet().stream().sorted(Entry.comparingByValue()).forEach(e -> {
				System.out.println(e.getKey() + " " + e.getValue());
			});
			break;
		case RDF:
			logger.info("Out RDF");
			Model outmodel = ModelFactory.createDefaultModel();

			Map<String, Map<String, Integer>> cp2topicMap = new HashMap<>();

			for (String cp : associations.keySet()) {
				Map<String, Integer> topicMap = new HashMap<>();
				for (String dbr : associations.get(cp)) {
					for (String topic : entityToTopics.get(dbr)) {
						Integer score = topicMap.get(topic);
						if (score == null) {
							score = 0;
						}
						score++;
						topicMap.put(topic, score);
					}
				}
				cp2topicMap.put(cp, topicMap);
			}
			// Write topics associated with CPs
			for (String cp : associations.keySet()) {
				Map<String, Integer> topicMap = cp2topicMap.get(cp);
				writeTopicMapForEntity(outmodel, cp, topicMap);

			}

			// Write topics associated with sets of CPs (grouped by location)
			ResultSet rsLocGroups = QueryExecutionFactory.create(SELECT_LOC, m).execSelect();
			Map<String, Map<String, Integer>> location2topicMap = new HashMap<>();

			while (rsLocGroups.hasNext()) {
				QuerySolution qs = rsLocGroups.next();
				String loc = qs.get("loc").asResource().getURI();
				String cps = qs.get("cps").asLiteral().getValue().toString();

				Map<String, Integer> topic2scoreForLocation = new HashMap<>();
				for (String cp : cps.split(" ")) {
					Map<String, Integer> topicMap = cp2topicMap.get(cp);
					if (topicMap != null) {
						incrementMapBy(topic2scoreForLocation, topicMap);
					}
				}

				writeTopicMapForEntity(outmodel, loc, topic2scoreForLocation);

				location2topicMap.put(loc, topic2scoreForLocation);

			}

			// Write theme associated with zones
			ResultSet locsByZones = QueryExecutionFactory.create(SELECT_LOC_BY_ZONE, m).execSelect();
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
				writeTopicMapForEntity(outmodel, zone, topicMapForZone);
			}

			outmodel.write(new FileOutputStream(new File(outputFile)), serialization);

			break;
		}

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

	void writeTopicMapForEntity(Model outmodel, String entity, Map<String, Integer> topic2scoreForLocation) {
		for (String topic : topic2scoreForLocation.keySet()) {
			String weightedThemeURI = baseURI + DigestUtils.md5Hex(entity + topic);
			outmodel.add(outmodel.createResource(entity), hasTheme, outmodel.createResource(topic));
			outmodel.add(outmodel.createResource(entity), hasWeightedTheme, outmodel.createResource(weightedThemeURI));
			outmodel.add(outmodel.createResource(weightedThemeURI), hasTheme, outmodel.createResource(topic));
			outmodel.add(outmodel.createResource(weightedThemeURI), score,
					outmodel.createTypedLiteral(topic2scoreForLocation.get(topic)));
		}
	}

	Map<String, Integer> getTopicsMap(Map<String, Set<String>> entityToTopics, Set<String> entities) {
		Map<String, Integer> totalWeight = new HashMap<>();
		Iterator<String> resIterator = entities.iterator();
		while (resIterator.hasNext()) {
			String string = (String) resIterator.next();
			Set<String> entityTopics = entityToTopics.get(string);
			for (String topic : entityTopics) {
				Integer w = totalWeight.get(topic);
				if (w == null) {
					w = 0;
				}
				w++;
				totalWeight.put(topic, w);
			}
		}
		return totalWeight;
	}

}
