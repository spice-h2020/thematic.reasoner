package com.github.spiceh2020.topicreasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicalReasoner {

	private static Logger logger = LoggerFactory.getLogger(TopicalReasoner.class);

	private static final String SELECT_CP = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX DUL: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#> SELECT DISTINCT ?ass {?cp a/rdfs:subClassOf* <https://w3id.org/arco/ontology/arco/CulturalProperty> ; DUL:associatedWith ?ass . FILTER (strStarts(str(?ass), \"http://dbpedia\")) }";

	public static void main(String[] args) {
		try {
			Configurations configs = new Configurations();
			Configuration config = configs.properties("config.properties");

			Model m = ModelFactory.createDefaultModel();
			RDFDataMgr.read(m, "https://raw.githubusercontent.com/spice-h2020/SON/main/issues/31/example.ttl");
			RDFDataMgr.read(m, "https://w3id.org/arco/ontology/arco", Lang.RDFXML);

			logger.info("Size {}", m.size());

			QueryExecution qexec = QueryExecutionFactory.create(SELECT_CP, m);

			ResultSet rs = qexec.execSelect();

			Set<String> maximalSetOfTopics = new HashSet<>();

			Map<String, Set<String>> topics = new HashMap<>();

			DBPediaBasedTopicDetector td = new DBPediaBasedTopicDetector();

			while (rs.hasNext()) {
				QuerySolution qs = (QuerySolution) rs.next();
				String iri = qs.get("ass").asResource().getURI();
				Set<String> r = td.detectTopics(iri);
				maximalSetOfTopics.addAll(r);
				topics.put(iri, r);
			}
			qexec.close();

			System.out.println("\nMaximal set");
			printSet(maximalSetOfTopics);

			System.out.println("\nIntersection");
			Iterator<String> resIterator = topics.keySet().iterator();
			Set<String> intersection = topics.get(resIterator.next());
			while (resIterator.hasNext()) {
				String string = (String) resIterator.next();
				intersection.retainAll(topics.get(string));
			}
			printSet(intersection);

			Map<String, Integer> weight = new HashMap<>();
			resIterator = topics.keySet().iterator();
			while (resIterator.hasNext()) {
				String string = (String) resIterator.next();
				Set<String> ts = topics.get(string);
				for (String s : ts) {
					Integer w = weight.get(s);
					if (w == null) {
						w = 0;
					}
					w++;
					weight.put(s, w);
				}
			}

			System.out.println("Weighted topics");

			weight.entrySet().stream().sorted(Entry.comparingByValue()).forEach(e -> {
				System.out.println(e.getKey() + " " + e.getValue());
			});

		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}

	private static void printSet(Set<String> s) {
		for (String t : s) {
			System.out.println(t);
		}
	}
}
