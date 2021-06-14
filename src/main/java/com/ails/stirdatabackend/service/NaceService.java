package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

@Service
public class NaceService {

    @Autowired
    @Qualifier("nace-sparql-endpoint")
    private SparqlEndpoint naceSparqlEndpoint;

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

//        System.out.println(sparql);
        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = new String(outStream.toByteArray());
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
    
    public Set<String> getLeafNoNaceLeaves(String uri) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "SELECT ?activity WHERE { ";
		if (level == 1) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
		} else if (level == 2) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
		} else if (level == 3) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
		} else if (level == 4) {
			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
    	} else if (level == 5) {
    		res.add(uri);
    		return res;
    	}
    	
		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/SIC2007NO> } ";

    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
    
    public Set<String> getLeafBeNaceLeaves(String uri) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		                "SELECT ?activity WHERE { ";
    	if (level == 1) {
    		sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 2) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 3) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
    	} else if (level == 4) {
			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
    	} else if (level == 5) {
    		res.add(uri);
    		return res;
    	}

		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/NACEBEL2008> } ";

    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
}
