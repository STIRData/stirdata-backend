package com.ails.stirdatabackend.vocs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DCTVocabulary extends Vocabulary {
    
	public static String NAMESPACE = "http://purl.org/dc/terms/";
	public static String PREFIX = "dct";

	public static String SPEC = "http://dublincore.org/2020/01/20/dublin_core_terms.ttl";
	
	static {
    	register(NAMESPACE, PREFIX);
    }

	public static Property abztract =  model.createProperty(NAMESPACE + "abstract");
	public static Property accessRights =  model.createProperty(NAMESPACE + "accessRights");
	public static Property accrualMethod =  model.createProperty(NAMESPACE + "accrualMethod");
	public static Property accrualPeriodicity =  model.createProperty(NAMESPACE + "accrualPeriodicity");
	public static Property accrualPolicy =  model.createProperty(NAMESPACE + "accrualPolicy");
	public static Property alternative =  model.createProperty(NAMESPACE + "alternative");
	public static Property audience =  model.createProperty(NAMESPACE + "audience");
	public static Property available =  model.createProperty(NAMESPACE + "available");
	public static Property bibliographicCitation =  model.createProperty(NAMESPACE + "/bibliographicCitation");
	public static Property conformsTo =  model.createProperty(NAMESPACE + "conformsTo");
	public static Property contributor =  model.createProperty(NAMESPACE + "contributor");
	public static Property coverage =  model.createProperty(NAMESPACE + "coverage");
	public static Property created =  model.createProperty(NAMESPACE + "created");
	public static Property creator =  model.createProperty(NAMESPACE + "creator");
	public static Property date =  model.createProperty(NAMESPACE + "date");
	public static Property dateAccepted =  model.createProperty(NAMESPACE + "dateAccepted");
	public static Property dateCopyrighted =  model.createProperty(NAMESPACE + "dateCopyrighted");
	public static Property dateSubmitted =  model.createProperty(NAMESPACE + "dateSubmitted");
	public static Property description =  model.createProperty(NAMESPACE + "description");
	public static Property educationLevel =  model.createProperty(NAMESPACE + "educationLevel");
	public static Property extent =  model.createProperty(NAMESPACE + "extent");
	public static Property format =  model.createProperty(NAMESPACE + "format");
	public static Property hasFormat =  model.createProperty(NAMESPACE + "hasFormat");
	public static Property hasPart =  model.createProperty(NAMESPACE + "hasPart");
	public static Property hasVersion =  model.createProperty(NAMESPACE + "hasVersion");
	public static Property identifier =  model.createProperty(NAMESPACE + "identifier");
	public static Property instructionalMethod =  model.createProperty(NAMESPACE + "instructionalMethod");
	public static Property isFormatOf =  model.createProperty(NAMESPACE + "isFormatOf");
	public static Property isPartOf =  model.createProperty(NAMESPACE + "isPartOf");
	public static Property isReferencedBy =  model.createProperty(NAMESPACE + "isReferencedBy");
	public static Property isReplacedBy =  model.createProperty(NAMESPACE + "isReplacedBy");
	public static Property isRequiredBy =  model.createProperty(NAMESPACE + "isRequiredBy");
	public static Property issued =  model.createProperty(NAMESPACE + "issued");
	public static Property isVersionOf =  model.createProperty(NAMESPACE + "isVersionOf");
	public static Property language =  model.createProperty(NAMESPACE + "language");
	public static Property license =  model.createProperty(NAMESPACE + "license");
	public static Property mediator =  model.createProperty(NAMESPACE + "mediator");
	public static Property medium =  model.createProperty(NAMESPACE + "medium");
	public static Property modified =  model.createProperty(NAMESPACE + "modified");
	public static Property provenance =  model.createProperty(NAMESPACE + "provenance");
	public static Property publisher =  model.createProperty(NAMESPACE + "publisher");
	public static Property references =  model.createProperty(NAMESPACE + "references");
	public static Property relation =  model.createProperty(NAMESPACE + "relation");
	public static Property replaces =  model.createProperty(NAMESPACE + "replaces");
	public static Property requires =  model.createProperty(NAMESPACE + "requires");
	public static Property rights =  model.createProperty(NAMESPACE + "rights");
	public static Property rightsHolder =  model.createProperty(NAMESPACE + "rightsHolder");
	public static Property source =  model.createProperty(NAMESPACE + "source");
	public static Property spatial =  model.createProperty(NAMESPACE + "spatial");
	public static Property subject =  model.createProperty(NAMESPACE + "subject");
	public static Property tableOfContents =  model.createProperty(NAMESPACE + "tableOfContents");
	public static Property temporal =  model.createProperty(NAMESPACE + "temporal");
	public static Property title =  model.createProperty(NAMESPACE + "title");
	public static Property type =  model.createProperty(NAMESPACE + "type");
	public static Property valid =  model.createProperty(NAMESPACE + "valid");
	
	public static Resource Agent =  model.createResource(NAMESPACE + "Agent");
	public static Resource AgentClass =  model.createResource(NAMESPACE + "AgentClass");
	public static Resource BibliographicResource =  model.createResource(NAMESPACE + "BibliographicResource");
	public static Resource FileFormat =  model.createResource(NAMESPACE + "FileFormat");
	public static Resource Frequency =  model.createResource(NAMESPACE + "Frequency");
	public static Resource Jurisdiction =  model.createResource(NAMESPACE + "Jurisdiction");
	public static Resource LicenseDocument =  model.createResource(NAMESPACE + "LicenseDocument");
	public static Resource LinguisticSystem =  model.createResource(NAMESPACE + "LinguisticSystem");
	public static Resource Location =  model.createResource(NAMESPACE + "Location");
	public static Resource LocationPeriodOrJurisdiction =  model.createResource(NAMESPACE + "LocationPeriodOrJurisdiction");
	public static Resource MediaType =  model.createResource(NAMESPACE + "MediaType");
	public static Resource MediaTypeOrExtent =  model.createResource(NAMESPACE + "MediaTypeOrExtent");
	public static Resource MethodOfAccrual =  model.createResource(NAMESPACE + "MethodOfAccrual");
	public static Resource MethodOfInstruction =  model.createResource(NAMESPACE + "MethodOfInstruction");
	public static Resource PeriodOfTime =  model.createResource(NAMESPACE + "PeriodOfTime");
	public static Resource PhysicalMedium =  model.createResource(NAMESPACE + "PhysicalMedium");
	public static Resource PhysicalResource =  model.createResource(NAMESPACE + "PhysicalResource");
	public static Resource Policy =  model.createResource(NAMESPACE + "Policy");
	public static Resource ProvenanceStatement =  model.createResource(NAMESPACE + "ProvenanceStatement");
	public static Resource RightsStatement =  model.createResource(NAMESPACE + "RightsStatement");
	public static Resource SizeOrDuration =  model.createResource(NAMESPACE + "SizeOrDuration");
	public static Resource Standard =  model.createResource(NAMESPACE + "Standard");
 
}
