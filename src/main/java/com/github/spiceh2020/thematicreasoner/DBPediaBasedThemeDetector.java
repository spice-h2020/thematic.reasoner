package com.github.spiceh2020.thematicreasoner;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class DBPediaBasedThemeDetector implements ThemeDetector {

	private static final String GET_CATEGORIES = "SELECT DISTINCT ?category { ?iri <http://purl.org/dc/terms/subject> ?category . }";
	private static final String GET_BROADER_CATEGORIES = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?category { ?iri skos:broader ?category . }";
	public static final String DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql";

	@Override
	public Set<String> detectTopics(String iri) {

		ParameterizedSparqlString pss = new ParameterizedSparqlString(GET_CATEGORIES);
		pss.setIri("iri", iri);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, pss.asQuery());

		Set<String> result = new HashSet<>();

		ResultSet rs = qexec.execSelect();

		while (rs.hasNext()) {
			QuerySolution qs = (QuerySolution) rs.next();
			result.add(qs.get("category").asResource().getURI());
			result.addAll(getBroaderCategories(qs.get("category").asResource().getURI()));
		}

		qexec.close();

		return result;
	}

	@Override
	public void setDistance(int distance) {
		throw new UnsupportedOperationException();
	}

	private Set<String> getBroaderCategories(String iri) {

		ParameterizedSparqlString pss = new ParameterizedSparqlString(GET_BROADER_CATEGORIES);
		pss.setIri("iri", iri);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, pss.asQuery());

		Set<String> result = new HashSet<>();

		ResultSet rs = qexec.execSelect();

		while (rs.hasNext()) {
			QuerySolution qs = (QuerySolution) rs.next();
			result.add(qs.get("category").asResource().getURI());

		}

		qexec.close();

		return result;
	}

}
