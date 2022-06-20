package com.ails.stirdatabackend.vocs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DCATVocabulary extends Vocabulary {
    
	public static String NAMESPACE = "http://www.w3.org/ns/dcat#";
    public static String PREFIX = "dcat";
    
    static {
    	register(NAMESPACE, PREFIX);
    }

    public static Property accessService = model.createProperty(NAMESPACE + "accessService");
    public static Property accessURL = model.createProperty(NAMESPACE + "accessURL");
    public static Property bbox = model.createProperty(NAMESPACE + "bbox");
    public static Property byteSize = model.createProperty(NAMESPACE + "byteSize");
    public static Property catalog = model.createProperty(NAMESPACE + "catalog");
    public static Property centroid = model.createProperty(NAMESPACE + "centroid");
    public static Property compressFormat = model.createProperty(NAMESPACE + "compressFormat");
    public static Property contactPoint = model.createProperty(NAMESPACE + "contactPoint");
    public static Property dataset = model.createProperty(NAMESPACE + "dataset");
    public static Property distribution = model.createProperty(NAMESPACE + "distribution");
    public static Property downloadURL = model.createProperty(NAMESPACE + "downloadURL");
    public static Property endDate = model.createProperty(NAMESPACE + "endDate");
    public static Property endpointDescription = model.createProperty(NAMESPACE + "endpointDescription");
    public static Property endpointURL = model.createProperty(NAMESPACE + "endpointURL");
    public static Property hadRole = model.createProperty(NAMESPACE + "hadRole");
    public static Property keyword = model.createProperty(NAMESPACE + "keyword");
    public static Property landingPage = model.createProperty(NAMESPACE + "landingPage");
    public static Property mediaType = model.createProperty(NAMESPACE + "mediaType");
    public static Property packageFormat = model.createProperty(NAMESPACE + "packageFormat");
    public static Property qualifiedRelation = model.createProperty(NAMESPACE + "qualifiedRelation");
    public static Property record = model.createProperty(NAMESPACE + "record");
    public static Property servesDataset = model.createProperty(NAMESPACE + "servesDataset");
    public static Property service = model.createProperty(NAMESPACE + "service");
    public static Property spatialResolutionInMeters = model.createProperty(NAMESPACE + "spatialResolutionInMeters");
    public static Property startDate = model.createProperty(NAMESPACE + "startDate");
    public static Property temporalResolution = model.createProperty(NAMESPACE + "temporalResolution");
    public static Property theme = model.createProperty(NAMESPACE + "theme");
    public static Property themeTaxonomy = model.createProperty(NAMESPACE + "themeTaxonomy");
    
    
    
    
    public static Resource Catalog = model.createResource(NAMESPACE + "Catalog");
    public static Resource CatalogRecord = model.createResource(NAMESPACE + "CatalogRecord");
    public static Resource DataService = model.createResource(NAMESPACE + "DataService");
    public static Resource Dataset = model.createResource(NAMESPACE + "Dataset");
    public static Resource Distribution = model.createResource(NAMESPACE + "Distribution");
    public static Resource Relationship = model.createResource(NAMESPACE + "Relationship");
    public static Resource Resource = model.createResource(NAMESPACE + "Resource");
    public static Resource Role = model.createResource(NAMESPACE + "Role");
    
}
