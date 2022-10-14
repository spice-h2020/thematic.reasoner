package com.github.spiceh2020.thematicreasoner;

import it.cnr.istc.stlab.rocksmap.RocksMultiMap;
import it.cnr.istc.stlab.rocksmap.transformer.StringRocksTransformer;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DMHExperiment {

    private final static Logger logger = LoggerFactory.getLogger(DMHExperiment.class);

    public static void main(String[] args) throws RocksDBException, IOException {
        // /Users/lgu/Desktop/NOTime/SPICE/dmh /Users/lgu/Desktop/NOTime/SPICE/ESGs/esg_dbc /Users/lgu/Desktop/NOTime/SPICE/DBPediaCategories/dbrSubjects
        File folder = new File(args[0]);
        String esgFolderPath = args[1];
        String subjectDBPath = args[2];
        FileOutputStream fos = new FileOutputStream(new File(args[3]));
        String categoriseLabelMapPath = args[4];
        String baseURI = "http://localhost/spice/dmh/";
        String queryString = "prefix semiotics: <http://ontologydesignpatterns.org/cp/owl/semiotics.owl#> " + "prefix earmark: <http://www.essepuntato.it/2008/12/earmark#> " + "SELECT ?entity ?associatedEntity { " + " ?pr semiotics:denotes ?associatedEntity . FILTER (STRSTARTS(STR(?associatedEntity), \"http://dbpedia.org/\"))  " + " ?pr earmark:refersTo ?entity . }";
        String selectText = "prefix earmark: <http://www.essepuntato.it/2008/12/earmark#>  SELECT ?t ?d {?d a earmark:StringDocuverse ; earmark:hasContent ?t}";
        String selectConnectedStories = "PREFIX earmark: <http://www.essepuntato.it/2008/12/earmark#> PREFIX theme: <https://w3id.org/spice/SON/theme/> CONSTRUCT { ?t1 theme:isTopicallyAssociatedWith ?t2 . ?t2 theme:isTopicallyAssociatedWith ?t1 . } WHERE {?t1 theme:hasTheme ?theme ; a earmark:StringDocuverse .  ?t2 theme:hasTheme ?theme ; a earmark:StringDocuverse . FILTER(STR(?t1) < STR(?t2)) } ";
        int distance = 2;

        RocksMultiMap<String, String> categoryLabelMap = new RocksMultiMap<>(categoriseLabelMapPath, new StringRocksTransformer(), new StringRocksTransformer());

        ESGThemeDetector d = new ESGThemeDetector(esgFolderPath, subjectDBPath);
        d.setDistance(distance);

        Model outModel = ModelFactory.createDefaultModel();

        for (File file : folder.listFiles()) {
            if (FileNameUtils.getExtension(file.getName()).equals("json")) {
                logger.info("Processing {}", file);
                Model m = ModelFactory.createDefaultModel();
                RDFDataMgr.read(m, file.getAbsolutePath(), Lang.JSONLD11);

                QueryExecution qExec = QueryExecutionFactory.create(selectText, m);

                ResultSet rs = qExec.execSelect();
                if (rs.hasNext()) {
                    QuerySolution qs = rs.next();
                    String text = qs.getLiteral("t").getValue().toString();
                    String docuverse = qs.getResource("d").getURI();
                    docuverse = docuverse.replace("https://w3id.org/spice/resource/", "http://localhost/spice/");
                    fixDocuverseThing(m, docuverse);

                    ThematicReasoner tr = new ThematicReasoner(m, QueryFactory.create(queryString), d);
                    tr.run();
                    tr.setBaseURI(baseURI + FileNameUtils.getBaseName(file.getName()) + "/");

                    fos.write(text.getBytes());
                    fos.write('\n');
                    fos.write(ThematicReasoner.entityToTopicMapToString(tr.getEntityToTopicMap()).getBytes());
                    fos.write('\n');

                    outModel.add(m.createResource(docuverse), m.createProperty("http://www.essepuntato.it/2008/12/earmark#", "hasContent"), text);
                    outModel.add(m.createResource(docuverse), RDF.type, m.createResource("http://www.essepuntato.it/2008/12/earmark#StringDocuverse"));
                    outModel.add(tr.getOutModel(categoryLabelMap));
                }


            }
        }




        QueryExecution qExec = QueryExecutionFactory.create(QueryFactory.create(selectConnectedStories), outModel);

//        fos.write('\n');
//        fos.write(ResultSetFormatter.asText(qExec.execSelect()).getBytes());

        outModel.add(qExec.execConstruct());

        fos.flush();
        fos.close();

        outModel.write(new FileOutputStream(args[3] + ".ttl"), "TTL");
    }

    private static void fixDocuverseThing(Model m, String docuverseURI) {

        List<Statement> statementList = new ArrayList<>();
        m.listStatements(null, null, m.createResource("ex:docuverse")).forEach(statementList::add);
        statementList.forEach(statement -> {
            Statement newStatement = m.createStatement(statement.getSubject(), statement.getPredicate(), m.createResource(docuverseURI));
            logger.trace("Replacing {} with {}", statement, newStatement);
            m.remove(statement);
            m.add(newStatement);
        });

    }
}
