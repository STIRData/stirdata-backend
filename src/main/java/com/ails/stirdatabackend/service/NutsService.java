package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

import java.util.HashSet;

import java.util.Set;

@Service
public class NutsService {

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;

    public String getNextNutsLevel(String parentNode) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"+
                        "SELECT ?code ?label WHERE { ";
        
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nuts/ont/level> 0 . "+
                      "?code <http://www.w3.org/2004/02/skos/core#notation> ?notation ."+
                      "VALUES ?notation { " +
                        "\"NO\" \"BE\" \"CZ\" \"EL\"}";
        } else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ". ";
        }
        
        sparql += " ?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label }";

        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = outStream.toString();
        }
        return json;
    }

    public String getTopLevelNuts(String uri) {
        String sparql = "SELECT ?nuts0 WHERE {\n" +
                "<" +  uri + ">" + "<http://www.w3.org/2004/02/skos/core#broader>* ?nuts0 .\n" +
                "?nuts0 <https://lod.stirdata.eu/nuts/ont/level> 0 .\n" +
                "} ";
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                return sol.get("nuts0").asResource().toString();
            }
        }
        return null;
    }
    
    public Set<String> getNuts3Descendents(String uri) {
    	Set<String> res = new HashSet<>();
    	
    	int pos = uri.lastIndexOf("/");
    	if (pos < 0) {
    		return res;
    	}
    	
    	String code = uri.substring(pos + 1);
    	int level = code.length() - 2;
    	
    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  "; 
    	if (level == 0) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader/skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 1) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 2) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 3) {
    		res.add(uri);
    		return res;
    	}
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("nuts3").asResource().toString());
            }
        }
    	
    	return res;
    }


}
