package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.configuration.Dimension;
import com.ails.stirdatabackend.configuration.ModelConfiguration;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.NutsService.RegionCodes;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import org.apache.http.impl.client.CloseableHttpClient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class QueryService {

	private final static Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Value("${page.size}")
    private int pageSize;

    @Getter
    @Setter
    private class CoreQuery {
    	public String where;
    	public boolean nuts3;
    	public boolean lau;
    	public boolean nace;
    }
    
    private CoreQuery buildCoreQuery(CountryConfiguration cc, boolean name, List<String> nuts3, List<String> lau, List<String> nace, Optional<String> startDateOpt, Optional<String> endDateOpt) {
    
    	CoreQuery cq = new CoreQuery();
    	
        String sparql = "";
        
        ModelConfiguration mc = cc.getModelConfiguration();
        
        sparql += mc.getEntitySparql() + " "; 
        
        if (name) {
        	sparql += mc.getEntityNameSparql() + " ";
        }
        
        if (nuts3 != null && (lau == null || lau.size() == 0)) {            
        	
        	sparql += mc.getNuts3Sparql() + " ";
        	
            sparql += " VALUES ?nuts3 { ";
            for (String uri : nuts3) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setNuts3(true);
            
        } else if ((nuts3 == null || nuts3.size() == 0) && lau != null) {            
        	
        	sparql += mc.getLauSparql() + " ";
        	
            sparql += " VALUES ?lau { ";
            for (String uri : lau) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setLau(true);
            
        } else if (nuts3 != null && lau != null) { // should be avoided
        	sparql += "{ ";
            
        	sparql += mc.getNuts3Sparql() + " ";
        	
            sparql += " VALUES ?nuts3 { ";
            for (String uri : nuts3) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            sparql += "} UNION { ";
            
            sparql += mc.getLauSparql() + " ";
        	
            sparql += " VALUES ?lau { ";
            for (String uri : lau) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            sparql += "} ";
            
            cq.setNuts3(true);
            cq.setLau(true);
        }

        
        if (nace != null) {
//        	sparql += " ?organization rov:orgActivity ?nace . ";
        	sparql += mc.getNaceSparql() + " ";
        	
            sparql += " VALUES ?nace { ";
            for (String uri : nace) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setNace(true);
        }
        

        // Date filter (if requested)
        if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
//            sparql += "?organization schema:foundingDate ?foundingDate ";
            sparql += mc.getFoundingDateSparql() + " ";

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
//        sparql += "} ";
        
        cq.setWhere(sparql);
        
        return cq;
    }
    
    
    public List<EndpointResponse> paginatedQuery(Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, int page, Optional<String> country) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
        String countryCode = country.orElse(null);
        
    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = nutsService.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        int offset = (page - 1) * pageSize;
        
        ObjectMapper mapper = new ObjectMapper();
        
        for (Map.Entry<CountryConfiguration, RegionCodes> ccEntry : requestMap.entrySet()) {
        	CountryConfiguration cc = ccEntry.getKey();
        	RegionCodes regions = ccEntry.getValue();
        	
        	ModelConfiguration mc = cc.getModelConfiguration();

            String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
            
//	        	System.out.println("B NACE : " + naceList.orElse(null));
//	        	System.out.println("B NUTS : " + countyNuts);

        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList.orElse(null));
        	List<String> nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3()); 
        	List<String> lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());
        	
//	        	System.out.println("A NACE : " + naceLeafUris);
//	        	System.out.println("A NUTS : " + nuts3UrisList);

        	int count = 0;
        	
        	if ((nuts3UrisList != null && nuts3UrisList.size() == 0 && lauUrisList != null && lauUrisList.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), count, countryCode));
        		return responseList;
        	}
        	
        	
        	CoreQuery sparql = buildCoreQuery(cc, true, nuts3UrisList, lauUrisList, naceLeafUris, startDateOpt, endDateOpt); 

//	            System.out.println("Will query endpoint: " + cc.getDataEndpoint().getSparqlEndpoint());

        	
            if (page == 1) {
            	String countQuery = prefix + " SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE { " + sparql.getWhere() + " } ";
            	
//	                System.out.println(QueryFactory.create(countQuery));
//	                System.out.println(cc.getDataEndpoint().getSparqlEndpoint());
             
                try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), QueryFactory.create(countQuery, Syntax.syntaxARQ))) {
                    ResultSet rs = qe.execSelect();
                    while (rs.hasNext()) {
                        QuerySolution sol = rs.next();
                        count = sol.get("count").asLiteral().getInt();
                    }
                }
            } else {
            	count = -1;
            }
            
            String query = prefix + " SELECT DISTINCT ?entity WHERE { " + sparql.getWhere() + " } LIMIT " + pageSize + " OFFSET " + offset;
            
//            System.out.println(query);
//            System.out.println(QueryFactory.create(query));

            List<String> companyUris = new ArrayList<>();

            StringWriter sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), QueryFactory.create(query, Syntax.syntaxARQ))) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    companyUris.add(sol.get("entity").asResource().toString());
                }
            }
//            System.out.println(companyUris);

            if (!companyUris.isEmpty()) {
//		            String sparqlConstruct = "CONSTRUCT { " + 
//                            "  ?entity ?p1 ?o1 . " + 
//                            "  ?o1 <https://schema.org/foundingDate> ?date . " +	            		
//   		                 "  ?o1 <http://www.w3.org/ns/org#siteAddress> ?o2 . ?o2 ?p3 ?o3 } " + 
//   		                 "WHERE { " +
//                            "  ?entity ?p1 ?o1 . " +
//   		                 "  OPTIONAL {?o1 <http://schema.org/foundingDate> ?date }  . " +
//                            "  OPTIONAL {?o1 <http://www.w3.org/ns/org#siteAddress> ?o2 . ?o2 ?p3 ?o3}  . " +
                        

                String sparqlConstruct = 
                		"CONSTRUCT { " + 
	                    "  ?entity a <http://www.w3.org/ns/regorg#RegisteredOrganization> . " + 
	                    "  ?entity <http://www.w3.org/ns/regorg#legalName> ?entityName . " +
	                    "  ?entity <http://www.w3.org/ns/regorg#orgActivity> ?nace . " +
	            		"  ?entity <http://www.w3.org/ns/org#siteAddress> ?address . ?address ?ap ?ao . " + 
	                    "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	            		
	            		"WHERE { " +
                        mc.getEntitySparql() + " " +
                        mc.getEntityNameSparql() + " " + 
                        "OPTIONAL { " + mc.getNuts3Sparql() + " ?address ?ap ?ao . } " + 
        	            "OPTIONAL { " + mc.getNaceSparql() + " } " +
	                    "OPTIONAL { " + mc.getFoundingDateSparql() + " } " +
	                                     
	                    "VALUES ?entity { ";
                for (String uri : companyUris) {
                   sparqlConstruct += " <" + uri + "> ";
                }
                sparqlConstruct += "} } ";
	
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
	                Model model = qe.execConstruct();
	                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
	            }
	            try {
	                responseList.add(new EndpointResponse(cc.getLabel(), mapper.readTree(sw.toString()), count, countryCode));
	            } catch (Exception e) {
	                e.printStackTrace();
	                return null;
	            }
            } else {
            	responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), count, countryCode));
            }
        }
        
        return responseList;
    }
    
    public List<EndpointResponse> groupedQuery(Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, boolean gnace, boolean gnuts3) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
//        
    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = nutsService.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<CountryConfiguration, RegionCodes> ccEntry : requestMap.entrySet()) {
        	CountryConfiguration cc = ccEntry.getKey();
        	RegionCodes regions = ccEntry.getValue();

            String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
            
        	boolean nace = gnace;
            boolean nuts3 = gnuts3;
//	            boolean lau = glau;
            
        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList.orElse(null));
        	List<String> nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3()); 
        	List<String> lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());

        	
        	if (naceLeafUris == null) {
        		nace = false;
        	}
        	
        	if (nuts3UrisList == null) {
        		nuts3 = false;
        	}
        	
//	        	if (lauUrisList == null) {
//	        		lau = false;
//	        	}

        	CoreQuery sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, startDateOpt, endDateOpt); 

//	            System.out.println("Will query endpoint: " + cc.getDataEndpoint().getSparqlEndpoint());

        	if ((nuts3UrisList != null && nuts3UrisList.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), 0, null));
        		return responseList;
        	}
        	
            prefix += "SELECT ";
            if (nace) {
            	prefix += " ?nace ";
            }
            if (nuts3) {
            	prefix += " ?nuts3 ";
            }
            
            prefix += "(COUNT(?entity) AS ?count) ";
            
            
            String query = prefix + " WHERE { " + sparql.getWhere() + " } GROUP BY " ;
            if (nace) {
            	query += " ?nace ";
            } 
            if (nuts3) {
            	query += " ?nuts3 ";
            }
            
//	            System.out.println(sparql);
            System.out.println(QueryFactory.create(query));

            String json;
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), query)) {
                ResultSet rs = qe.execSelect();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                ResultSetFormatter.outputAsJSON(outStream, rs);
                json = outStream.toString();
            }
            
            try {
                responseList.add(new EndpointResponse(cc.getLabel(), mapper.readTree(json), 0, null));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        	        
        return responseList;
    }
    
    public List<EndpointResponse> statistics(CountryConfiguration cc, Dimension dimension, String dimensionRoot) {
    	return statistics(cc, dimension, dimensionRoot, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    public List<EndpointResponse> statistics(CountryConfiguration cc, Dimension dimension, String dimensionRootUri, Optional<List<String>> nutsList, Optional<List<String>> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt) {

    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsList.isPresent()) {
    		requestMap = nutsService.getEndpointsByNuts(nutsList.get());
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
    	RegionCodes regions = requestMap.get(cc);

    	List<String> naceLeafUris = null;
    	List<String> nuts3UrisList = null;
    	List<String> lauUrisList = null;
    	
    	List<String> iterUris = null;
    	
    	System.out.println(cc.getCountry());
    	System.out.println(dimensionRootUri);
    	
        if (dimension == Dimension.NUTS) {
         	naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList.orElse(null));
         	
         	iterUris = nutsService.getNextNutsLevelList(dimensionRootUri == null ? cc.getNutsPrefix() + "" + cc.getCountry() : dimensionRootUri);
        } else if (dimension == Dimension.LAU) {
         	naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList.orElse(null));
         	
         	iterUris = nutsService.getNextNutsLevelList(dimensionRootUri);
        } else if (dimension == Dimension.NACE) {
	        nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3()); 
	        lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());
	        
	        iterUris = naceService.getNextNaceLevelList(dimensionRootUri);
        }

//        System.out.println(naceLeafUris);
//        System.out.println(nuts3UrisList);
//        System.out.println(lauUrisList);
    	System.out.println(iterUris);

        String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

        for (String uri : iterUris) {
        	CoreQuery sparql = null; 
        			
        	
	        if (dimension == Dimension.NUTS) {
	        	if (uri.startsWith("https://lod.stirdata.eu/nuts/code/")) {
	        		sparql = buildCoreQuery(cc, false, new ArrayList<>(nutsService.getLocalizedNuts3Descendents(uri, cc)), null, naceLeafUris, startDateOpt, endDateOpt);
	        	} else if (uri.startsWith("https://lod.stirdata.eu/lau/code/")) {
	            	List<String> uriAsList = new ArrayList<>();
	            	uriAsList.add(nutsService.getLocalizedLau(uri, cc));
	            	
	        		sparql = buildCoreQuery(cc, false, null, uriAsList, naceLeafUris, startDateOpt, endDateOpt);
	        	}
	        } else if (dimension == Dimension.NACE) {
            	List<String> uriAsList = new ArrayList<>();
            	uriAsList.add(uri);
            	
		        sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceService.getLocalNaceLeafUris(cc.getCountry(),  uriAsList), startDateOpt, endDateOpt); 
	        }
	  
	        String query = prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE { " + sparql.getWhere() + " } " ;
	
	        System.out.println(uri);
//	        System.out.println(QueryFactory.create(query));
	
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), query)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		System.out.println(rs.next());
            	}
	        }
        }        	        
//        return responseList;
           return null;
    }    

}
