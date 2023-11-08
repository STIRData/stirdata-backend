package com.ails.stirdatabackend.vocs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class SDVocabulary extends Vocabulary {
    
	public static String NAMESPACE = "https://w3id.org/stirdata/vocabulary/";
	public static String PREFIX = "stirdata";

	
	public static Property naceDataset =  model.createProperty(NAMESPACE + "naceDataset");
	public static Property level =  model.createProperty(NAMESPACE + "level");
 
}
