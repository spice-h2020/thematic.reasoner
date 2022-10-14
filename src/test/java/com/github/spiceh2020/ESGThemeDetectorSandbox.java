package com.github.spiceh2020 ;

import com.github.spiceh2020.thematicreasoner.ESGThemeDetector;
import org.rocksdb.RocksDBException;

import java.util.Set;

public class ESGThemeDetectorSandbox {

    public static void main(String[] args) {
        try {
            ESGThemeDetector d = new ESGThemeDetector("/Users/lgu/Desktop/NOTime/SPICE/ESGs/esg_dbc","/Users/lgu/Desktop/NOTime/SPICE/DBPediaCategories/dbrSubjects");
            Set<String> topics = d.detectTopics("http://dbpedia.org/resource/Barack_Obama");

            System.out.println(topics);
            System.out.println(topics.size());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
  
}
