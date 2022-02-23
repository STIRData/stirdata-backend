package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.NutsService.PlaceSelection;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class QueryService {

	private final static Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Value("${page.size}")
    private int pageSize;

    
    public List<EndpointResponse> paginatedQuery(List<Code> nutsLauCodes, List<Code> naceCodes, Code founding, Code dissolution, int page, String country) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
        
    	Map<CountryConfiguration, PlaceSelection> countryPlaceMap;
    	if (nutsLauCodes != null) {
    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
    	} else {
    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        int offset = (page - 1) * pageSize;
        
        ObjectMapper mapper = new ObjectMapper();
        
        for (Map.Entry<CountryConfiguration, PlaceSelection> ccEntry : countryPlaceMap.entrySet()) {
        	CountryConfiguration cc = ccEntry.getKey();
        	PlaceSelection places = ccEntry.getValue();
        	
        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCodes);
        	List<String> nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, places.getNuts3()); 
        	List<String> lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places.getLau());
        	
        	int count = 0;
        	
        	if ((nutsLeafUris != null && nutsLeafUris.size() == 0 && lauUris != null && lauUris.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getCountryLabel(), mapper.createArrayNode(), count, country));
//        		return responseList;
        	}
        	
        	//could use statistics table instead of this if available
        	SparqlQuery sparql = SparqlQuery.buildCoreQuery(cc, true, true, nutsLeafUris, lauUris, naceLeafUris, founding, dissolution); 

            if (page == 1) {
            	String countQuery = sparql.countSelectQuery();
            	
//            	System.out.println(countQuery);
//              System.out.println(cc.getDataEndpoint());
             
                try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(countQuery, Syntax.syntaxARQ))) {
                    ResultSet rs = qe.execSelect();
                    while (rs.hasNext()) {
                        QuerySolution sol = rs.next();
                        count = sol.get("count").asLiteral().getInt();
                    }
                }
            } else {
            	count = -1;
            }
            
            String query = sparql.allSelectQuery(offset, pageSize);
            
//           System.out.println(query);

            List<String> companyUris = new ArrayList<>();

            StringWriter sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(query, Syntax.syntaxARQ))) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    companyUris.add(sol.get("entity").asResource().toString());
                }
            }

            if (!companyUris.isEmpty()) {
                String sparqlConstruct = 
                		"CONSTRUCT { " + 
	                    "  ?entity a <http://www.w3.org/ns/regorg#RegisteredOrganization> . " + 
	                    "  ?entity <http://www.w3.org/ns/regorg#legalName> ?entityName . " +
	                    "  ?entity <http://www.w3.org/ns/regorg#orgActivity> ?nace . " +
	            		"  ?entity <http://www.w3.org/ns/org#siteAddress> ?address . ?address ?ap ?ao . " + 
	                    "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	            		
	            		" WHERE { " +
                        cc.getEntitySparql() + " " +
                        cc.getLegalNameSparql() + " " + 
                        cc.getActiveSparql() + " " +
                        "OPTIONAL { " + cc.getNuts3Sparql() + " ?address ?ap ?ao . } " + 
        	            "OPTIONAL { " + cc.getNaceSparql() + " } " +
	                    "OPTIONAL { " + cc.getFoundingDateSparql() + " } " +
	                                     
	                    "VALUES ?entity { ";
                for (String uri : companyUris) {
                   sparqlConstruct += " <" + uri + "> ";
                }
                sparqlConstruct += "} } ";
	
//                System.out.println(sparqlConstruct);
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
	                Model model = qe.execConstruct();
	                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
	            }
	            try {
	                responseList.add(new EndpointResponse(cc.getCountryLabel(), mapper.readTree(sw.toString()), count, country));
	            } catch (Exception e) {
	                e.printStackTrace();
	                return null;
	            }
            } else {
            	responseList.add(new EndpointResponse(cc.getCountryLabel(), mapper.createArrayNode(), count, country));
            }
        }
        
        return responseList;
    }
    
    public List<EndpointResponse> groupedQuery(List<Code> nutsLauCodes, List<Code> naceCodes, Code founding, Code dissolution, boolean gnace, boolean gnuts3) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
//        
    	Map<CountryConfiguration, PlaceSelection> countryPlaceMap;
    	if (nutsLauCodes != null) {
    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
    	} else {
    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<CountryConfiguration, PlaceSelection> ccEntry : countryPlaceMap.entrySet()) {
        	CountryConfiguration cc = ccEntry.getKey();
        	PlaceSelection places = ccEntry.getValue();

        	boolean nace = gnace;
            boolean nuts3 = gnuts3;
//	        boolean lau = glau;
            
        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCodes);
        	List<String> nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUris(cc, places.getNuts3()); 
        	List<String> lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places.getLau());
        	
        	if (naceLeafUris == null) {
        		nace = false;
        	}
        	
        	if (nutsLeafUris == null) {
        		nuts3 = false;
        	}
        	
//	        	if (lauUrisList == null) {
//	        		lau = false;
//	        	}

        	SparqlQuery sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsLeafUris, lauUris, naceLeafUris, founding, dissolution); 

        	if ((nutsLeafUris != null && nutsLeafUris.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getCountryLabel(), mapper.createArrayNode(), 0, null));
        		return responseList;
        	}
        	
            
            String query = sparql.groupBySelectQuery(nuts3, nace);
//	            System.out.println(sparql);
            System.out.println(QueryFactory.create(query));

            String json;
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
                ResultSet rs = qe.execSelect();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                ResultSetFormatter.outputAsJSON(outStream, rs);
                json = outStream.toString();
            }
            
            try {
                responseList.add(new EndpointResponse(cc.getCountryLabel(), mapper.readTree(json), 0, null));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        	        
        return responseList;
    }
    



}
