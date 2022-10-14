package com.github.spiceh2020.thematicreasoner;

import it.cnr.istc.stlab.edwin.EquivalenceSetGraphLoader;
import it.cnr.istc.stlab.edwin.model.EquivalenceSetGraph;
import it.cnr.istc.stlab.rocksmap.RocksMultiMap;
import it.cnr.istc.stlab.rocksmap.transformer.StringRocksTransformer;
import org.rocksdb.RocksDBException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ESGThemeDetector implements ThemeDetector {

    private final EquivalenceSetGraph esg;
    private final RocksMultiMap<String, String> subjectMap;
    private int distance = 1;


    public ESGThemeDetector(String esgFolder, String subjectDB) throws RocksDBException {
        esg = EquivalenceSetGraphLoader.loadEquivalenceSetGraphFromFolder(esgFolder);
        subjectMap = new RocksMultiMap<>(subjectDB, new StringRocksTransformer(), new StringRocksTransformer());
    }


    @Override
    public Set<String> detectTopics(String iri) {
        Collection<String> categories = subjectMap.get(iri);

        if (categories != null) {
            Set<String> result = new HashSet<>(categories);
            Set<String> alreadySeen = new HashSet<>();
            int maxDistance = distance;
            for (; maxDistance > 0; maxDistance--) {
                for (String category : categories) {
                    if (!alreadySeen.contains(category)) {
                        alreadySeen.add(category);
                        Set<String> superSets = esg.getSuperEquivalenceSets(category);
                        if (superSets != null) {
                            result.addAll(superSets);
                        }
                    }
                }
            }
            return result;
        }
        return new HashSet<>();
    }

    @Override
    public void setDistance(int distance) {
        this.distance = distance;
    }


}
