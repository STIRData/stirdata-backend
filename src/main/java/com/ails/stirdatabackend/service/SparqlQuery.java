package com.ails.stirdatabackend.service;

import java.util.List;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SparqlQuery {
	private String where;
	public boolean nuts3;
	public boolean lau;
	public boolean nace;

	private String graph; 
	

    private static String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
    
    public SparqlQuery(String graph) {
    	this.graph = graph;
    }
    
    public String getGraphPart() {
    	return graph != null ? " FROM <" + graph + ">" : "";
    }
    
    public String countSelectQuery() {
    	return prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) " + getGraphPart() + "  WHERE { " + getWhere() + " } " ;
    }

    public String allSelectQuery(int offset, int limit) {
    	return prefix + " SELECT DISTINCT ?entity " + getGraphPart() + " WHERE { " + getWhere() + " } LIMIT " + limit + " OFFSET " + offset;
    }
    
    public String groupBySelectQuery(boolean nuts3, boolean nace) {
    	String query = prefix + "SELECT ";
    	if (nace) {
    		query += " ?nace ";
    	}
    	if (nuts3) {
    		query += " ?nuts3 ";
    	}
    
    	query += "(COUNT(?entity) AS ?count) " + getGraphPart();
    
    
    	query = query + " WHERE { " + getWhere() + " } GROUP BY " ;
    	if (nace) {
    		query += " ?nace ";
    	} 
    	if (nuts3) {
    		query += " ?nuts3 ";
    	}
    	
    	return query;
    }
    
    public static SparqlQuery buildCoreQuery(CountryConfiguration cc, boolean name, List<String> nuts3, List<String> lau, List<String> nace, Code foundingDate, Code dissolutionDate) {
    
    	SparqlQuery cq = new SparqlQuery(cc.getDataNamedGraph());
    	
        String sparql = "";
        
        sparql += cc.getEntitySparql() + " "; 
        
        if (cc.isDissolutionDate() && (dissolutionDate == null || (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() == null))) {
        	sparql += cc.getActiveSparql() + " ";
        }
        
        if (name) {
        	sparql += cc.getLegalNameSparql() + " ";
        }
        
        
        if (nuts3 != null && (lau == null || lau.size() == 0)) {
        	if (nuts3.size() > 0) {
	        	sparql += cc.getNuts3Sparql() + " ";
	        	
	            sparql += " VALUES ?nuts3 { ";
	            for (String uri : nuts3) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
	            
	            cq.setNuts3(true);
        	}            
        } else if ((nuts3 == null || nuts3.size() == 0) && lau != null) {            
        	if (lau.size() > 0) {
	        	sparql += cc.getLauSparql() + " ";
	        	
	            sparql += " VALUES ?lau { ";
	            for (String uri : lau) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
	        	
	            cq.setLau(true);
        	}            
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
        if (foundingDate != null && (foundingDate.getDateFrom() != null || foundingDate.getDateTo() != null)) {
            sparql += cc.getFoundingDateSparql() + " ";

            if (foundingDate.getDateFrom() != null && foundingDate.getDateTo() == null) {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getDateFrom() + "\"^^xsd:date) ";
            }
            else if (foundingDate.getDateFrom() == null && foundingDate.getDateTo() != null) {
                sparql += "FILTER( ?foundingDate <= \"" + foundingDate.getDateTo() + "\"^^xsd:date) ";
            }
            else {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getFromDate() + "\"^^xsd:date && ?foundingDate <= \"" + foundingDate.getToDate() + "\"^^xsd:date) ";

            }
        }
        
        if (dissolutionDate != null && (dissolutionDate.getFromDate() != null || dissolutionDate.getToDate() != null)) {
            sparql += cc.getDissolutionDateSparql() + " ";

            if (dissolutionDate.getFromDate() != null && dissolutionDate.getToDate() == null) {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getFromDate() + "\"^^xsd:date) ";
            }
            else if (dissolutionDate.getFromDate() == null && dissolutionDate.getToDate() != null) {
                sparql += "FILTER( ?dissolutionDate < \"" + dissolutionDate.getToDate() + "\"^^xsd:date) ";
            }
            else {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getFromDate() + "\"^^xsd:date && ?dissolutionDate < \"" + dissolutionDate.getToDate() + "\"^^xsd:date) ";

            }
        }
        
        cq.setWhere(sparql);
        
        return cq;
    }
}

