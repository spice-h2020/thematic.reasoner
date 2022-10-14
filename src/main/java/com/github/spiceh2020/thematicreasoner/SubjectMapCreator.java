package com.github.spiceh2020.thematicreasoner;

import it.cnr.istc.stlab.rocksmap.RocksMultiMap;
import it.cnr.istc.stlab.rocksmap.transformer.StringRocksTransformer;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.DCTerms;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SubjectMapCreator {

    private static final Logger logger = LoggerFactory.getLogger(SubjectMapCreator.class);

    public static void main(String[] args) throws RocksDBException {
        logger.info("Creating Subject Map");
        AtomicInteger ai = new AtomicInteger();
        String inputFile = args[0];
        String subjectDB = args[1];
        RocksMultiMap<String, String> subjectMultiMap = new RocksMultiMap<>(subjectDB, new StringRocksTransformer(), new StringRocksTransformer());
        StreamRDF destination = new StreamRDFBase() {

            @Override
            public void triple(Triple triple) {
                if (triple.getPredicate().matches(DCTerms.subject.asNode())) {
                    logger.trace("Adding {} {}", triple.getSubject().getURI(), triple.getObject().getURI());
                    subjectMultiMap.put(triple.getSubject().getURI(), triple.getObject().getURI());
                }
                if (ai.incrementAndGet() % 100000 == 0) {
                    logger.info("{} triples processed", ai.get());
                }
            }

            @Override
            public void quad(Quad quad) {
                triple(quad.asTriple());
            }
        };
        RDFDataMgr.parse(destination, inputFile);
        subjectMultiMap.close();
        logger.info("End");
    }

}
