package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.EndpointManager;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.utils.URIMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class QueryService {

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;

    @Autowired
    private EndpointManager endpointManager;

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Autowired
    private URIMapper uriMapper;

    @Value("${page.size}")
    private int pageSize;

    
    private String buildCoreQuery(SparqlEndpoint endpoint, List<String> nuts3UrisList, Set<String> naceLeafUris, Optional<String> startDateOpt, Optional<String> endDateOpt) {
    

        String sparql = 
//        		  "SELECT ?organization WHERE { " +
//                  "SELECT DISTINCT ?organization WHERE { " +
                  " { " +
                  "  ?organization a rov:RegisteredOrganization . " +
                  "  ?organization rov:legalName ?organizationName . ";
        
        if (nuts3UrisList != null) {                            
            sparql += "  ?organization org:hasRegisteredSite ?registeredSite . " +
                      "  ?registeredSite org:siteAddress ?address . ";

			if (endpoint.getName().equals("czechia-endpoint")) {
                sparql += " ?address ebg:adminUnitL4 ?nuts3 . ";
            } else {
                sparql += " ?address <https://lod.stirdata.eu/model/nuts3> ?nuts3 . ";
            }

            sparql += " VALUES ?nuts3 { ";
            for (String uri : nuts3UrisList) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
        }
        
        if (naceLeafUris != null) {
        	sparql += " ?organization rov:orgActivity ?nace . ";
        	
            sparql += " VALUES ?nace { ";
            for (String uri : naceLeafUris) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";            	
        }
        

        // Date filter (if requested)
        if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
            sparql += "?organization schema:foundingDate ?foundingDate ";

            if (startDateOpt.isPresent() && !endDateOpt.isPresent()) {
                String startDate = startDateOpt.get();
                sparql += "FILTER( ?foundingDate > \"" + startDate + "\"^^xsd:date) ";
            }
            else if (!startDateOpt.isPresent() && endDateOpt.isPresent()) {
                String endDate = endDateOpt.get();
                sparql += "FILTER( ?foundingDate < \"" + endDate + "\"^^xsd:date) ";
            }
            else {
                String startDate = startDateOpt.get();
                String endDate = endDateOpt.get();
                sparql += "FILTER( ?foundingDate > \"" + startDate + "\"^^xsd:date && ?foundingDate < \"" + endDate + "\"^^xsd:date) ";

            }
        }
        sparql += "} ";
        
        return sparql;
    }
    
    private Set<String> getNaceLeafUris(SparqlEndpoint endpoint, Optional<List<String>> naceList) {
    	Set<String> naceLeafUris = null;
    	if (naceList.isPresent()) {
    		naceLeafUris = new HashSet<>();
    	
        	if (endpoint.getName().equals("czechia-endpoint")) {
        	    naceLeafUris.add("http://lod.stirdata.eu/nace/dummy");
        	} else if (endpoint.getName().equals("belgium-endpoint")) {
        		for (String s : naceList.get()) {
        			naceLeafUris.addAll(naceService.getLeafBeNaceLeaves(s));
                }    		
        	} else if (endpoint.getName().equals("norway-endpoint")) {
        		for (String s : naceList.get()) {
        			naceLeafUris.addAll(naceService.getLeafNoNaceLeaves(s));
                }    		
        	} else if (endpoint.getName().equals("greece-endpoint")) {
        		for (String s : naceList.get()) {
        			naceLeafUris.addAll(naceService.getLeafElNaceLeaves(s));
                }    		
            }
    	}
    	
    	return naceLeafUris;
    }
    
    private List<String> getNuts3Uris(SparqlEndpoint endpoint, HashMap<SparqlEndpoint, List<String>> requestMap) {
    	
    	List<String> nuts3UrisList = null;
    	if (requestMap.get(endpoint) != null) {
            Set<String> nuts3Uris = new HashSet<>();
            for (String s : requestMap.get(endpoint)) {
            	nuts3Uris.addAll(nutsService.getNuts3Descendents(s));
            }

            nuts3UrisList = new ArrayList<>(nuts3Uris);

            nuts3UrisList = endpoint.getName().equals("czechia-endpoint") ? 
            		uriMapper.mapCzechNutsUri(nuts3UrisList) : uriMapper.mapEuropaNutsUri(nuts3UrisList);

    	}
    	
    	return nuts3UrisList;

    }
    
    
    public List<EndpointResponse> paginatedQuery(Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, int page) {
        
    	List<EndpointResponse> responseList = new ArrayList<EndpointResponse>();
        
    	HashMap<SparqlEndpoint, List<String>> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = endpointManager.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = endpointManager.getEndpointsByNuts(); 
    	}
    	
        int offset = (page - 1) * pageSize;
        
        ObjectMapper mapper = new ObjectMapper();
        
        String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                "PREFIX rov: <http://www.w3.org/ns/regorg#> " +
                "PREFIX ebg: <http://data.businessgraph.io/ontology#> " +
                "PREFIX org: <http://www.w3.org/ns/org#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

        for (SparqlEndpoint endpoint : requestMap.keySet()) {

            if (endpoint.getName().equals("czechia-endpoint")) {
            	prefix += "PREFIX schema: <http://schema.org/> ";
            } else {
                prefix += "PREFIX schema: <https://schema.org/> ";
            }

            
        	Set<String> naceLeafUris = getNaceLeafUris(endpoint, naceList);
        	List<String> nuts3UrisList = getNuts3Uris(endpoint, requestMap);
        	
        	String sparql = buildCoreQuery(endpoint, nuts3UrisList, naceLeafUris, startDateOpt, endDateOpt); 

            System.out.println("Will query endpoint: "+endpoint.getSparqlEndpoint());

        	int count = 0;
        	
        	if ((nuts3UrisList != null && nuts3UrisList.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(endpoint.getName(), mapper.createArrayNode(), count));
        		return responseList;
        	}

        	
            if (page == 1) {
            	String countQuery = prefix + " SELECT (COUNT(DISTINCT ?organization) AS ?count) WHERE " + sparql ;
            	
                System.out.println(QueryFactory.create(countQuery));

                try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), countQuery)) {
                    ResultSet rs = qe.execSelect();
                    while (rs.hasNext()) {
                        QuerySolution sol = rs.next();
                        count = sol.get("count").asLiteral().getInt();
                    }
                }
            } else {
            	count = -1;
            }
            
            sparql = prefix + " SELECT DISTINCT ?organization WHERE " + sparql + "  LIMIT " + pageSize + " OFFSET " + offset;
            
            System.out.println(QueryFactory.create(sparql));

            List<String> companyUris = new ArrayList<String>();

            StringWriter sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), sparql)) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    companyUris.add(sol.get("organization").asResource().toString());
                }
            }
//            System.out.println(companyUris);

            if (!companyUris.isEmpty()) {
	            String sparqlConstruct = "CONSTRUCT { " + 
	                                     "  ?company ?p1 ?o1 . " + 
	                                     "  ?o1 <https://schema.org/foundingDate> ?date . " +	            		
	            		                 "  ?o1 <http://www.w3.org/ns/org#siteAddress> ?o2 . ?o2 ?p3 ?o3 } " + 
	            		                 "WHERE { " +
	                                     "  ?company ?p1 ?o1 . " +
	            		                 "  OPTIONAL {?o1 <http://schema.org/foundingDate> ?date }  . " +
	                                     "  OPTIONAL {?o1 <http://www.w3.org/ns/org#siteAddress> ?o2 . ?o2 ?p3 ?o3}  . " +
	                                     "  VALUES ?company { ";
	            for (String uri : companyUris) {
	                sparqlConstruct += " <" + uri + "> ";
	            }
	            sparqlConstruct += "} } ";
	
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
	                Model model = qe.execConstruct();
	                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
	            }
	            try {
	                responseList.add(new EndpointResponse(endpoint.getName(), mapper.readTree(sw.toString()), count));
	            } catch (Exception e) {
	                e.printStackTrace();
	                return null;
	            }
            } else {
            	responseList.add(new EndpointResponse(endpoint.getName(), mapper.createArrayNode(), count));
            }
        }
        return responseList;
    }
    
    public List<EndpointResponse> groupedQuery(Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, boolean gnace, boolean gnuts3) {
        
    	List<EndpointResponse> responseList = new ArrayList<EndpointResponse>();
        
    	HashMap<SparqlEndpoint, List<String>> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = endpointManager.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = endpointManager.getEndpointsByNuts(); 
    	}
    	
        ObjectMapper mapper = new ObjectMapper();

        String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                "PREFIX rov: <http://www.w3.org/ns/regorg#> " +
                "PREFIX ebg: <http://data.businessgraph.io/ontology#> " +
                "PREFIX org: <http://www.w3.org/ns/org#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

        for (SparqlEndpoint endpoint : requestMap.keySet()) {

        	if (endpoint.getName().equals("czechia-endpoint")) {
            	prefix += "PREFIX schema: <http://schema.org/> ";
            } else {
                prefix += "PREFIX schema: <https://schema.org/> ";
            }

        	boolean nace = gnace;
            boolean nuts3 = gnuts3;
            
            Set<String> naceLeafUris = getNaceLeafUris(endpoint, naceList);
        	List<String> nuts3UrisList = getNuts3Uris(endpoint, requestMap);
        	
        	if (naceLeafUris == null) {
        		nace = false;
        	}
        	
        	if (nuts3UrisList == null) {
        		nuts3 = false;
        	}

        	String sparql = buildCoreQuery(endpoint, nuts3UrisList, naceLeafUris, startDateOpt, endDateOpt); 

            System.out.println("Will query endpoint: "+endpoint.getSparqlEndpoint());

        	if ((nuts3UrisList != null && nuts3UrisList.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(endpoint.getName(), mapper.createArrayNode(), 0));
        		return responseList;
        	}
        	
            prefix += "SELECT ";
            if (nace) {
            	prefix += " ?nace ";
            }
            if (nuts3) {
            	prefix += " ?nuts3 ";
            }
            
            prefix += "(COUNT(?organization) AS ?count) ";
            
            
            sparql = prefix + " WHERE " + sparql + " GROUP BY " ;
            if (nace) {
            	sparql += " ?nace ";
            } 
            if (nuts3) {
            	sparql += " ?nuts3 ";
            }
            
            System.out.println(QueryFactory.create(sparql));

            String json;
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), sparql)) {
                ResultSet rs = qe.execSelect();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                ResultSetFormatter.outputAsJSON(outStream, rs);
                json = new String(outStream.toByteArray());
            }
            
            try {
                responseList.add(new EndpointResponse(endpoint.getName(), mapper.readTree(json), 0));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return responseList;
    }    
}
