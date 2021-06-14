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
import java.io.Writer;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class QueryService {

    @Autowired
    @Qualifier("czech-sparql-endpoint")
    private SparqlEndpoint czechSparqlEndpoint;

    @Autowired
    @Qualifier("belgium-sparql-endpoint")
    private SparqlEndpoint belgiumSparqlEndpoint;

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

    public enum SUPPORTED_COUNTRIES {
        BELGIUM,
        CZECH,
        NORWAY
    }

    // We suppose that NUTS3 is provided.
    // Only NUTS is handled right now.
    public List<EndpointResponse> paginatedQuery(Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, int page) {
        
    	List<EndpointResponse> responseList = new ArrayList<EndpointResponse>();
        
    	HashMap<SparqlEndpoint, List<String>> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = endpointManager.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = endpointManager.getEndpointsByNuts(); 
    	}
    	
        System.out.println(requestMap.toString());
        int offset = (page - 1) * pageSize;
        

        for (SparqlEndpoint endpoint : requestMap.keySet()) {
            
        	Set<String> naceLeafUris = null;
        	if (naceList.isPresent()) {
        		naceLeafUris = new HashSet<>();
        	
	        	if (endpoint.getName().equals("czech-endpoint")) {
	        	    naceLeafUris.add("http://lod.stirdata.eu/nace/dummy");
	        	} else if (endpoint.getName().equals("belgium-endpoint")) {
	        		for (String s : naceList.get()) {
	        			naceLeafUris.addAll(naceService.getLeafBeNaceLeaves(s));
	                }    		
	        	} else if (endpoint.getName().equals("norway-endpoint")) {
	        		for (String s : naceList.get()) {
	        			naceLeafUris.addAll(naceService.getLeafNoNaceLeaves(s));
	                }    		
	            }
        	}
        	
        	List<String> nuts3UrisList = null;
        	if (requestMap.get(endpoint) != null) {
	            Set<String> nuts3Uris = new HashSet<>();
	            for (String s : requestMap.get(endpoint)) {
	            	nuts3Uris.addAll(nutsService.getNuts3Descendents(s));
	            }
	
	            nuts3UrisList = new ArrayList<>(nuts3Uris);

	            nuts3UrisList = endpoint.getName().equals("czech-endpoint") ? 
	            		uriMapper.mapCzechNutsUri(nuts3UrisList) : uriMapper.mapEuropaNutsUri(nuts3UrisList);

        	}


            String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "PREFIX rov: <http://www.w3.org/ns/regorg#> " +
                            "PREFIX ebg: <http://data.businessgraph.io/ontology#> " +
                            "PREFIX org: <http://www.w3.org/ns/org#> " +
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

            if (endpoint.getName().equals("czech-endpoint")) {
                sparql += "PREFIX schema: <http://schema.org/> ";
            } else {
                sparql += "PREFIX schema: <https://schema.org/> ";
            }

            sparql += 
//            		  "SELECT ?organization WHERE { " +
                      "SELECT DISTINCT ?organization WHERE { " +
                      "  ?organization a rov:RegisteredOrganization . " +
                      "  ?organization rov:legalName ?organizationName . ";
            
            if (nuts3UrisList != null) {                            
	            sparql += "  ?organization org:hasRegisteredSite ?registeredSite . " +
	                      "  ?registeredSite org:siteAddress ?address . ";
	
				if (endpoint.getName().equals("czech-endpoint")) {
	                sparql += " ?address ebg:adminUnitL4 ?NUTS3 . ";
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
            sparql +=
//                  "ORDER BY ?organizationName } " +
            		" LIMIT " + pageSize + " OFFSET " + offset;

            System.out.println("Will query endpoint: "+endpoint.getSparqlEndpoint());
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
            System.out.println(companyUris);

            String sparqlConstruct = "CONSTRUCT { ?company ?p ?q } WHERE {\n" +
                                    "  ?company ?p ?q . \n" +
                                    "  VALUES ?company {";
            for (String uri : companyUris) {
                sparqlConstruct += " <" + uri + "> ";
            }
            sparqlConstruct += "}\n" +
                                    "}\n";

//            Writer sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
                Model model = qe.execConstruct();
                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                responseList.add(new EndpointResponse(endpoint.getName(), mapper.readTree(sw.toString())));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return responseList;
    }
}
