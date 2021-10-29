package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.http.client.HttpClient;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class NaceService {

	private final static Logger logger = LoggerFactory.getLogger(NaceService.class);
	
    @Autowired
    @Qualifier("endpoint-nace-eu")
    private SparqlEndpoint naceEndpointEU;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
    public List<String> getLocalNaceLeafUris(String country, List<String> naceList, HttpClient httpClient) {
    	List<String> naceLeafUris = null;
    	if (naceList != null) {
    		naceLeafUris = new ArrayList<>();
    		
    		CountryConfiguration cc = countryConfigurations.get(country);
    		for (String s : naceList) {
       			naceLeafUris.addAll(getNaceLeaves(s, httpClient, cc));
            }
    	}
    	
    	return naceLeafUris;
    }    
    
    public Set<String> getNaceLeaves(String uri, HttpClient httpClient, CountryConfiguration cc) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = 
    			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "SELECT ?activity WHERE { ";
		
    	if (level == 1) {
			sparql += "?activity " + cc.getNacePath1() + " <" + uri + "> . "; 
		} else if (level == 2) {
			sparql += "?activity " + cc.getNacePath2() + " <" + uri + "> . "; 
		} else if (level == 3) {
			sparql += "?activity " + cc.getNacePath3() + " <" + uri + "> . "; 
		} else if (level == 4) {
			sparql += "?activity " + cc.getNacePath4() + " <" + uri + "> . "; 
    	}
    	
    	if (cc.getNaceFixedLevel() >= 0) {
    		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> " + cc.getNaceFixedLevel() + " . ";
    	}
    	
		sparql += " ?activity skos:inScheme <" + cc.getNaceScheme() + "> } ";

    	
		System.out.println(cc.getNaceEndpoint().getSparqlEndpoint());
		System.out.println(QueryFactory.create(sparql));
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }    
    
    public String getNextNaceLevel(String parentNode, String language) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "SELECT ?code ?label WHERE { ";
        
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nace/ont/level> 1 . ";
        } else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ". ";
        }
        sparql += "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label " +
                  "FILTER (lang(?label) = \"" + language + "\") }";

//        System.out.println(QueryFactory.create(sparql));
        
        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceEndpointEU.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = outStream.toString();
        }
        return json;
    }
    
    public int getNaceLevel(String uri) {
    	int pos = uri.lastIndexOf("/");
    	if (pos < 0) {
    		return -1;
    	}
    	
    	String code = uri.substring(pos + 1);
    	int len = code.length();
    	
    	if (len == 1 && Character.isLetter(code.charAt(0))) {
    		return 1;
    	} else if (len == 2) {
    		return 2;
    	} else if (len == 4 && code.charAt(2) == '.') {
    	    return 3;
    	} else if (len == 5 && code.charAt(2) == '.') {
    		return 4;
    	}
    	
    	return -1;
    }

    
//    public Set<String> getNONaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//                "SELECT ?activity WHERE { ";
//		if (level == 1) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
//		} else if (level == 2) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
//		} else if (level == 3) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
//		} else if (level == 4) {
//			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
//    	}
//    	
//		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
//		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/SIC2007NO> } ";
//
//    	
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("NO").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//
//    }
//    
//    public Set<String> getBENaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//		                "SELECT ?activity WHERE { ";
//    	if (level == 1) {
//    		sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 2) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 3) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
//    	} else if (level == 4) {
//			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
//    	}
//
//		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
//		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/NACEBEL2008> } ";
//
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("BE").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//
//    }
//    
//    public Set<String> getELNaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//		                "SELECT ?activity WHERE { ";
//    	if (level == 1) {
//    		sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 2) {
//			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 3) {
//			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
//    	} else if (level == 4) {
//			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch <" + uri + "> . "; 
//    	}
//
//		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 7 . " +
//		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/KAD2008> } ";
//
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("EL").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//
//    }
//    
//    public Set<String> getCZNaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//		                "SELECT ?activity WHERE { " +
//    		            "   ?activity skos:broader*/skos:exactMatch  <" + uri + "> . " + 
//		                "   ?activity skos:inScheme <https://obchodní-rejstřík.stirdata.opendata.cz/zdroj/číselníky/nace-cz> " +
//    		            " } ";
//
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("CZ").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//
//    }
//    
//    public Set<String> getFINaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//		                "SELECT ?activity WHERE { ";
//    	if (level == 1) {
//    		sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 2) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
//    	} else if (level == 3) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
//    	} else if (level == 4) {
//			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
//    	}
//
//		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
//		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/TOL2008> } ";
//
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("FI").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//    }
//    
//    public Set<String> getUKNaceLeaves(String uri, HttpClient httpClient) {
//    	Set<String> res = new HashSet<>();
//
//    	int level = getNaceLevel(uri);
//    	if (level < 0) {
//    		return res;
//    	}
//
//    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
//                "SELECT ?activity WHERE { ";
//		if (level == 1) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
//		} else if (level == 2) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
//		} else if (level == 3) {
//			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
//		} else if (level == 4) {
//			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
//    	}
//    	
//		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
//		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/SIC2007UK> } ";
//
//    	
//    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("UK").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
//            ResultSet rs = qe.execSelect();
//            while (rs.hasNext()) {
//                QuerySolution sol = rs.next();
//                res.add(sol.get("activity").asResource().toString());
//            }
//        }
//    	
//    	return res;
//
//    }

}
