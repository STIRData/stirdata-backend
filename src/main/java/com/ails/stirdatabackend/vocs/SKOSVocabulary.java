package com.ails.stirdatabackend.vocs;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class SKOSVocabulary extends Vocabulary {
    
	public static String NAMESPACE = "http://www.w3.org/2004/02/skos/core#";
	public static String PREFIX = "skos";
	
    static {
    	register(NAMESPACE, PREFIX);
    }
    
    public static String getNamespace() {
    	return NAMESPACE;
    }	
    
	public static Resource Collection =  model.createProperty(NAMESPACE + "Collection");
	public static Resource Concept =  model.createProperty(NAMESPACE + "Concept");
	public static Resource ConceptScheme =  model.createProperty(NAMESPACE + "ConceptScheme");
	public static Resource OrderedCollection =  model.createProperty(NAMESPACE + "OrderedCollection");
	
	public static Property altLabel =  model.createProperty(NAMESPACE + "altLabel");
	public static Property broadMatch =  model.createProperty(NAMESPACE + "broadMatch");
	public static Property broader =  model.createProperty(NAMESPACE + "broader");
	public static Property broaderTransitive =  model.createProperty(NAMESPACE + "broaderTransitive");
	public static Property changeNote =  model.createProperty(NAMESPACE + "changeNote");
	public static Property closeMatch =  model.createProperty(NAMESPACE + "closeMatch");
	public static Property definition =  model.createProperty(NAMESPACE + "definition");
	public static Property editorialNote =  model.createProperty(NAMESPACE + "editorialNote");
	public static Property exactMatch =  model.createProperty(NAMESPACE + "exactMatch");
	public static Property example =  model.createProperty(NAMESPACE + "example");
	public static Property hasTopConcept =  model.createProperty(NAMESPACE + "hasTopConcept");
	public static Property hiddenLabel =  model.createProperty(NAMESPACE + "hiddenLabel");
	public static Property historyNote =  model.createProperty(NAMESPACE + "historyNote");
	public static Property inScheme =  model.createProperty(NAMESPACE + "inScheme");
	public static Property mappingRelation =  model.createProperty(NAMESPACE + "mappingRelation");
	public static Property member =  model.createProperty(NAMESPACE + "member");
	public static Property memberList =  model.createProperty(NAMESPACE + "memberList");
	public static Property narrowMatch =  model.createProperty(NAMESPACE + "narrowMatch");
	public static Property narrower =  model.createProperty(NAMESPACE + "narrower");
	public static Property narrowerTransitive =  model.createProperty(NAMESPACE + "narrowerTransitive");
	public static Property notation =  model.createProperty(NAMESPACE + "notation");
	public static Property note =  model.createProperty(NAMESPACE + "note");
	public static Property prefLabel =  model.createProperty(NAMESPACE + "prefLabel");
	public static Property related =  model.createProperty(NAMESPACE + "related");
	public static Property relatedMatch =  model.createProperty(NAMESPACE + "relatedMatch");
	public static Property scopeNote =  model.createProperty(NAMESPACE + "scopeNote");
	public static Property semanticRelation =  model.createProperty(NAMESPACE + "semanticRelation");
	public static Property topConceptOf =  model.createProperty(NAMESPACE + "topConceptOf");
	
 
}
