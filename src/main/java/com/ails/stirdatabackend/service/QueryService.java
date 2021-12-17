package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.configuration.Dimension;
import com.ails.stirdatabackend.configuration.ModelConfiguration;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.NutsService.RegionCodes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.datatypes.xsd.XSDDateTime;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
    
    @Getter
    @Setter
    private class CoreQuery {
    	public String where;
    	public boolean nuts3;
    	public boolean lau;
    	public boolean nace;
    }
    
    private CoreQuery buildCoreQuery(CountryConfiguration cc, boolean name, List<String> nuts3, List<String> lau, List<String> nace, String foundingStartDate, String foundingEndDate, String dissolutionStartDate, String dissolutionEndDate) {
    
    	CoreQuery cq = new CoreQuery();
    	
        String sparql = "";
        
        ModelConfiguration mc = cc.getModelConfiguration();
        
        sparql += cc.getEntitySparql() + " "; 
        
        if (dissolutionStartDate == null && dissolutionEndDate == null) {
        	sparql += cc.getActiveSparql() + " ";
        }
        
        if (name) {
        	sparql += cc.getLegalNameSparql() + " ";
        }
        
        
        if (nuts3 != null && (lau == null || lau.size() == 0)) {            
        	
        	sparql += cc.getNuts3Sparql() + " ";
        	
            sparql += " VALUES ?nuts3 { ";
            for (String uri : nuts3) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setNuts3(true);
            
        } else if ((nuts3 == null || nuts3.size() == 0) && lau != null) {            
        	
        	sparql += cc.getLauSparql() + " ";
        	
            sparql += " VALUES ?lau { ";
            for (String uri : lau) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setLau(true);
            
        } else if (nuts3 != null && lau != null) { // should be avoided
        	sparql += "{ ";
            
        	sparql += cc.getNuts3Sparql() + " ";
        	
            sparql += " VALUES ?nuts3 { ";
            for (String uri : nuts3) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            sparql += "} UNION { ";
            
            sparql += cc.getLauSparql() + " ";
        	
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
        	sparql += cc.getNaceSparql() + " ";
        	
            sparql += " VALUES ?nace { ";
            for (String uri : nace) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            cq.setNace(true);
        }
        

        // Date filter (if requested)
        if (foundingStartDate != null || foundingEndDate != null) {
            sparql += cc.getFoundingDateSparql() + " ";

            if (foundingStartDate != null && foundingEndDate == null) {
                sparql += "FILTER( ?foundingDate >= \"" + foundingStartDate + "\"^^xsd:date) ";
            }
            else if (foundingStartDate == null && foundingEndDate != null) {
                sparql += "FILTER( ?foundingDate < \"" + foundingEndDate + "\"^^xsd:date) ";
            }
            else {
                sparql += "FILTER( ?foundingDate >= \"" + foundingStartDate + "\"^^xsd:date && ?foundingDate < \"" + foundingEndDate + "\"^^xsd:date) ";

            }
        }
        
        if (dissolutionStartDate != null || dissolutionEndDate != null) {
            sparql += cc.getDissolutionDateSparql() + " ";

            if (dissolutionStartDate != null && dissolutionEndDate == null) {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionStartDate + "\"^^xsd:date) ";
            }
            else if (dissolutionStartDate == null && dissolutionEndDate != null) {
                sparql += "FILTER( ?dissolutionDate < \"" + dissolutionEndDate + "\"^^xsd:date) ";
            }
            else {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionStartDate + "\"^^xsd:date && ?dissolutionDate < \"" + dissolutionEndDate + "\"^^xsd:date) ";

            }
        }

        
        cq.setWhere(sparql);
        
        return cq;
    }
    
    
    public List<EndpointResponse> paginatedQuery(List<String> nutsLauList, List<String> naceList, String foundingStartDate, String foundingEndDate, String dissolutionStartDate, String dissolutionEndDate, int page, String country) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
        
    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsLauList != null) {
    		requestMap = nutsService.getEndpointsByNuts(nutsLauList);
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

        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList);
        	List<String> nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3()); 
        	List<String> lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());
        	
//	        	System.out.println("A NACE : " + naceLeafUris);
//	        	System.out.println("A NUTS : " + nuts3UrisList);

        	int count = 0;
        	
        	if ((nuts3UrisList != null && nuts3UrisList.size() == 0 && lauUrisList != null && lauUrisList.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), count, country));
        		return responseList;
        	}
        	
        	
        	CoreQuery sparql = buildCoreQuery(cc, true, nuts3UrisList, lauUrisList, naceLeafUris, foundingStartDate, foundingEndDate, dissolutionStartDate, dissolutionEndDate); 

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
	
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
	                Model model = qe.execConstruct();
	                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
	            }
	            try {
	                responseList.add(new EndpointResponse(cc.getLabel(), mapper.readTree(sw.toString()), count, country));
	            } catch (Exception e) {
	                e.printStackTrace();
	                return null;
	            }
            } else {
            	responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), count, country));
            }
        }
        
        return responseList;
    }
    
    public List<EndpointResponse> groupedQuery(List<String> nutsLauList, List<String> naceList, String foundingStartDate, String foundingEndDate, String dissolutionStartDate, String dissolutionEndDate, boolean gnace, boolean gnuts3) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
//        
    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsLauList != null) {
    		requestMap = nutsService.getEndpointsByNuts(nutsLauList);
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
            
        	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList);
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

        	CoreQuery sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, foundingStartDate, foundingEndDate, dissolutionStartDate, dissolutionEndDate); 

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
//            System.out.println(QueryFactory.create(query));

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
    

    @Getter
    @Setter
    public class StatisticResult {
    	@JsonInclude(JsonInclude.Include.NON_NULL)
    	private String uri;
    	
    	@JsonInclude(JsonInclude.Include.NON_NULL)
    	private String fromDate;
    	
    	@JsonInclude(JsonInclude.Include.NON_NULL)
    	private String toDate;

    	@JsonIgnore
    	private String interval;

    	private int count;
    	
    	StatisticResult(String uri, int count) {
    		this.uri = uri;
    		this.count = count;
    	}
    	
    	StatisticResult(String fromDate, String toDate, String interval, int count) {
    		this.fromDate = fromDate;
    		this.toDate = toDate;
    		this.interval = interval;
    		this.count = count;
    	}
    }
    
    public void statistics(CountryConfiguration cc, Dimension dimension, String rootUri, List<String> nutsLauList, List<String> naceList, String foundingStartDateOpt, String foundingEndDate, String dissolutionStartDateOpt, String dissolutionEndDate, List<StatisticResult> res) {

        if (dimension == Dimension.NUTS && rootUri != null && rootUri.startsWith("https://lod.stirdata.eu/lau/code/")) {
    		return;
    	} else if (dimension == Dimension.NACE && rootUri != null && rootUri.matches("https://lod.stirdata.eu/nace/nace-rev2/code/\\d\\d\\.\\d\\d")) {
    		return;
    	}
        	

    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsLauList != null) {
    		requestMap = nutsService.getEndpointsByNuts(nutsLauList);
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
    	RegionCodes regions = requestMap.get(cc);

    	List<String> naceLeafUris = null;
    	List<String> nuts3UrisList = null;
    	List<String> lauUrisList = null;
    	
    	List<String> iterUris = null;
    	
//    	System.out.println(cc.getCountry());
//    	System.out.println(rootUri);
//    	System.out.println(nutsLauList);
    	
        if (dimension == Dimension.NUTS) {
         	naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList);
         	
         	iterUris = nutsService.getNextNutsLevelList(rootUri == null ? NutsService.nutsPrefix + "" + cc.getCountry() : rootUri);
        } else if (dimension == Dimension.LAU) {
         	naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList);
         	
         	iterUris = nutsService.getNextNutsLevelList(rootUri);
        } else if (dimension == Dimension.NACE) {
	        nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3()); 
	        lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());
	        
	        iterUris = naceService.getNextNaceLevelList(rootUri);
        } 

//    	System.out.println(iterUris);

        String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

        for (String uri : iterUris) {
        	CoreQuery sparql = null; 
        			
	        if (dimension == Dimension.NUTS) {
	        	if (uri.startsWith("https://lod.stirdata.eu/nuts/code/")) {
	        		sparql = buildCoreQuery(cc, false, new ArrayList<>(nutsService.getLocalizedNuts3Descendents(uri, cc)), null, naceLeafUris, foundingStartDateOpt, foundingEndDate, dissolutionStartDateOpt, dissolutionEndDate);
	        	} else if (uri.startsWith("https://lod.stirdata.eu/lau/code/")) {
	            	List<String> uriAsList = new ArrayList<>();
	            	uriAsList.add(nutsService.getLocalizedLau(uri, cc));
	            	
	        		sparql = buildCoreQuery(cc, false, null, uriAsList, naceLeafUris, foundingStartDateOpt, foundingEndDate, dissolutionStartDateOpt, dissolutionEndDate);
	        	}
	        } else if (dimension == Dimension.NACE) {
            	List<String> uriAsList = new ArrayList<>();
            	uriAsList.add(uri);
            	
		        sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceService.getLocalNaceLeafUris(cc.getCountry(),  uriAsList), foundingStartDateOpt, foundingEndDate, dissolutionStartDateOpt, dissolutionEndDate); 
	        }
	  
	        String query = prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE { " + sparql.getWhere() + " } " ;
	
//	        System.out.println(uri);
//	        System.out.println(query);
	
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), query)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		QuerySolution sol = rs.next();

//            		System.out.println(sol);
            		int count = sol.get("count").asLiteral().getInt();
            		if (count > 0) {
            			res.add(new StatisticResult(uri, count));
            		}
            	}
	        }
        }        	        

    }    

    public void dateStatistics(CountryConfiguration cc, Dimension dimension, String fromDate, String toDate, String interval, List<String> nutsLauList, List<String> naceList, String foundingStartDate, String foundingEndDate, String dissolutionStartDate, String dissolutionEndDate,  List<StatisticResult> res) throws ParseException {

    	if (interval != null && interval.equals("1M")) {
    		return;
    	}
    	
    	Map<CountryConfiguration, RegionCodes> requestMap;
    	if (nutsLauList != null) {
    		requestMap = nutsService.getEndpointsByNuts(nutsLauList);
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
    	RegionCodes regions = requestMap.get(cc);

    	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc.getCountry(), naceList);
    	List<String> nuts3UrisList = regions == null ? null : nutsService.getNuts3Uris(cc, regions.getNuts3());
    	List<String> lauUrisList = regions == null ? null : nutsService.getLauUris(cc, regions.getLau());
    	
    	String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

   		CoreQuery sparql = null; 
		String query = null;
		
		if (dimension == Dimension.FOUNDING) {
			sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, null, null, dissolutionStartDate, dissolutionEndDate);
   			query = prefix + "SELECT (MIN(?foundingDate) AS ?date) WHERE { " + sparql.getWhere() + " " + cc.getFoundingDateSparql() + " } " ;
		} else if (dimension == Dimension.DISSOLUTION) {
			sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, foundingStartDate, foundingEndDate, null, null);
			query = prefix + "SELECT (MIN(?dissolutionDate) AS ?date) WHERE { " + sparql.getWhere() + " " + cc.getDissolutionDateSparql() + " } " ;
		}

   		Calendar minDate = null;
   		if (fromDate == null) {
	   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), query)) {
		    	ResultSet rs = qe.execSelect();
		    	
		    	while (rs.hasNext()) {
		    		QuerySolution sol = rs.next();
		
		    		minDate = ((XSDDateTime)sol.get("date").asLiteral().getValue()).asCalendar();
		    	}
		    }
   		} else {
   			minDate = Calendar.getInstance();
   			minDate.setTime(dateFormat.parse(fromDate));
   		}
   		
   		Calendar maxDate = Calendar.getInstance();
   		if (toDate != null) {
   			maxDate.setTime(dateFormat.parse(toDate));
   		}
   		
//   		System.out.println(dateFormat.format(minDate.getTime()) + " >>> " + dateFormat.format(maxDate.getTime()) );
   		Calendar endDate = maxDate;
   		while (endDate.after(minDate)) {
   			String resInterval = null;
   			Calendar startDate = (Calendar)endDate.clone();
   			if (interval == null) {
   				startDate.add(Calendar.YEAR, -10);
   				resInterval = "10Y";
   			} else if (interval.equals("10Y")) {
   				startDate.add(Calendar.YEAR, -1);
   				resInterval = "1Y";
   			} else if (interval.equals("1Y")) {
   				startDate.add(Calendar.MONTH, -1);
   				resInterval = "1M";
   			}
   			
   			if (startDate.before(minDate)) {
   				startDate = minDate;
   				
   				Calendar startDate2 = (Calendar)startDate.clone();
   				startDate2.add(Calendar.YEAR, 1);
   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
   					resInterval = "1Y";
   				} else {
   					startDate2.add(Calendar.YEAR, -1);
   					startDate2.add(Calendar.MONTH, 1);
   	   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
   	   					resInterval = "1M";
   	   				}
   				}
   			}
   			
   			if (dimension == Dimension.FOUNDING) {
   				sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, dateFormat.format(startDate.getTime()), dateFormat.format(endDate.getTime()), dissolutionStartDate, dissolutionEndDate); 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				sparql = buildCoreQuery(cc, false, nuts3UrisList, lauUrisList, naceLeafUris, foundingStartDate, foundingEndDate, dateFormat.format(startDate.getTime()), dateFormat.format(endDate.getTime()));
   			}
   			
	        query = prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE { " + sparql.getWhere() + " } " ;
	
//	        System.out.println(dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime()));
//	        System.out.println(query);
	
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint().getSparqlEndpoint(), query)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		QuerySolution sol = rs.next();

//            		System.out.println(sol);
            		int count = sol.get("count").asLiteral().getInt();
            		if (count > 0) {
            			res.add(new StatisticResult(dateFormat.format(startDate.getTime()), dateFormat.format(endDate.getTime()), resInterval, count));
            		}
            	}
	        }
            
            endDate = startDate;
   		}

    }    

}
