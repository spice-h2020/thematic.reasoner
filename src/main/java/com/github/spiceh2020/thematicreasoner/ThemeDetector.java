package com.github.spiceh2020.thematicreasoner;

import java.util.Set;

public interface ThemeDetector {
	
	/**
	 * Retrieve a list of topics associated to a give IRI
	 * 
	 * @param iri the IRI of the entity to which the topics are to be associated.
	 * @return A (possibly empty) list of IRIs of the topics associated to the IRI given as parameter
	 * 
	 */
	public Set<String> detectTopics(String iri);

	/**
	 * Set the maximum distance of the categories to be included in the neighborhood.
	 *
	 * @param distance
	 */
	public void setDistance(int distance);

}
