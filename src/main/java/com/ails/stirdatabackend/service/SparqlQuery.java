package com.ails.stirdatabackend.service;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SparqlQuery {
	private String where;
//	public boolean nuts3;
//	public boolean lau;
//	public boolean nace;

	private String graph; 
	

    private static String prefix = //"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            //"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
    		"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
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

    public String countSelectQueryGroupByNuts3() {
    	return prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) ?nuts3 " + getGraphPart() + "  WHERE { " + getWhere() + " } GROUP BY ?nuts3 " ;
    }

    public String countSelectQueryGroupByLau() {
    	return prefix + "SELECT (COUNT(DISTINCT ?entity) AS ?count) ?lau " + getGraphPart() + "  WHERE { " + getWhere() + " } GROUP BY ?lau " ;
    }

    public String minMaxFoundingDateQuery(CountryDB cc) {
    	return prefix + "SELECT (MIN(?foundingDate) AS ?minDate) (MAX(?foundingDate) AS ?maxDate) " + getGraphPart() + "  WHERE { " + getWhere() + " " + cc.getFoundingDateSparql() + " } " ;
    }

    public Calendar[] minMaxFoundingDate(CountryDB cc) {
   		Calendar[] date = new Calendar[2];
   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), minMaxFoundingDateQuery(cc))) {
//   			System.out.println("A " + cc.getDataEndpoint());
//   			System.out.println("B " + minMaxFoundingDateQuery(cc));
   			
//   			System.out.println(QueryFactory.create(minMaxFoundingDateQuery(cc)));
	    	ResultSet rs = qe.execSelect();
		    	
	    	while (rs.hasNext()) {
	    		QuerySolution sol = rs.next();
		
	    		if (sol.get("minDate") != null) {
	    			date[0] = ((XSDDateTime)sol.get("minDate").asLiteral().getValue()).asCalendar();
	    		}
	    		if (sol.get("maxDate") != null) {
	    			date[1] = ((XSDDateTime)sol.get("maxDate").asLiteral().getValue()).asCalendar();
	    		}
	    	}
	    }
   		
   		return date;
    }
    
    public String minMaxDissolutionDateQuery(CountryDB cc) {
    	return prefix + "SELECT (MIN(?dissolutionDate) AS ?minDate) (MAX(?dissolutionDate) AS ?maxDate) " + getGraphPart() + "  WHERE { " + getWhere() + " " + cc.getDissolutionDateSparql() + " } " ;
    }
    
    public Calendar[] minMaxDissolutionDate(CountryDB cc) {
    	Calendar[] date = new Calendar[2];
    	
//    	System.out.println(QueryFactory.create(minMaxDissolutionDateQuery(cc)));
    	
   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), minMaxDissolutionDateQuery(cc))) {
	    	ResultSet rs = qe.execSelect();
		    	
	    	while (rs.hasNext()) {
	    		QuerySolution sol = rs.next();
	    		
	    		if (sol.get("minDate") != null) {
		    		date[0] = ((XSDDateTime)sol.get("minDate").asLiteral().getValue()).asCalendar();
		    		date[1] = ((XSDDateTime)sol.get("maxDate").asLiteral().getValue()).asCalendar();
	    		} else {
	    			date[0] = null;
	    			date[1] = null;
	    		}
	    	}
	    }
   		
   		return date;
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
    
    	query += "(COUNT(DISTINCT ?entity) AS ?count) " + getGraphPart();
    
    
    	query = query + " WHERE { " + getWhere() + " } GROUP BY " ;
    	if (nace) {
    		query += " ?nace ";
    	} 
    	if (nuts3) {
    		query += " ?nuts3 ";
    	}
    	
    	return query;
    }
    
    public static SparqlQuery buildCoreQuery(CountryDB cc, boolean active, boolean name, Collection<String> nuts3, Collection<String> lau, Collection<String> nace, Code foundingDate, Code dissolutionDate) {
    
    	SparqlQuery cq = new SparqlQuery(cc.getDataNamedGraph());
    	
        String sparql = "";
        
        sparql += cc.getEntitySparql() + " "; 
        
        if (active && cc.isDissolutionDate()) {
        	if (dissolutionDate == null || (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() == null)) {
	        	sparql += cc.getActiveSparql() + " ";
	        }
        }
        
        if (name && cc.isLegalName()) {
        	sparql += cc.getLegalNameSparql() + " ";
        }
        
        
        if (nuts3 != null && (lau == null || lau.size() == 0)) {
        	if (nuts3.size() > 0) {
	        	sparql += cc.getNuts3Sparql() + " ";
	        	
	            sparql += " VALUES ?nuts3 { ";
	            for (String uri : new HashSet<>(nuts3)) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
	            
//	            cq.setNuts3(true);
        	} else {
        		return null;
        	}
        } else if ((nuts3 == null || nuts3.size() == 0) && lau != null) {            
        	if (lau.size() > 0) {
	        	sparql += cc.getLauSparql() + " ";
	        	
	            sparql += " VALUES ?lau { ";
	            for (String uri : new HashSet<>(lau)) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
	        	
//	            cq.setLau(true);
        	} else {
        		return null;
        	}
        } else if (nuts3 != null && lau != null) { // should be avoided
        	sparql += "{ ";
            
        	sparql += cc.getNuts3Sparql() + " ";
        	
            sparql += " VALUES ?nuts3 { ";
            for (String uri : new HashSet<>(nuts3)) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            sparql += "} UNION { ";
            
            sparql += cc.getLauSparql() + " ";
        	
            sparql += " VALUES ?lau { ";
            for (String uri : new HashSet<>(lau)) {
                sparql += "<" + uri + "> ";
            }
            sparql += "} ";
            
            sparql += "} ";
            
//            cq.setNuts3(true);
//            cq.setLau(true);
        }

        
        if (nace != null) {
        	if (nace.size() > 0) {
	        	sparql += cc.getNaceSparql() + " ";
	        	
	        	if (cc.getNacePathSparql() != null) {
	        	
	        		sparql += " ?nace " + cc.getNacePathSparql() + " ?naceroot .";
	        		
		            sparql += " VALUES ?naceroot { ";
		            for (String uri : new HashSet<>(nace)) {
		                sparql += "<" + uri + "> ";
		            }
		            sparql += "} ";
	        		
	        	} else {
		            sparql += " VALUES ?nace { ";
		            for (String uri : new HashSet<>(nace)) {
		                sparql += "<" + uri + "> ";
		            }
		            sparql += "} ";
	        	}
        	} else {
        		return null;
        	}
            
//            cq.setNace(true);
        }
        

        // Date filter (if requested)
        
        if (foundingDate != null && (foundingDate.getDateFrom() != null || foundingDate.getDateTo() != null)) {
            sparql += cc.getFoundingDateSparql() + " ";

            if (foundingDate.getDateFrom() != null && foundingDate.getDateTo() == null) {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getDateFrom() + "\"^^xsd:date) ";
            } else if (foundingDate.getDateFrom() == null && foundingDate.getDateTo() != null) {
                sparql += "FILTER( ?foundingDate <= \"" + foundingDate.getDateTo() + "\"^^xsd:date) ";
            } else {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getDateFrom() + "\"^^xsd:date && ?foundingDate <= \"" + foundingDate.getDateTo() + "\"^^xsd:date) ";

            }
        }
        
        if (dissolutionDate != null && (dissolutionDate.getDateFrom() != null || dissolutionDate.getDateTo() != null)) {
            sparql += cc.getDissolutionDateSparql() + " ";

            if (dissolutionDate.getDateFrom() != null && dissolutionDate.getDateTo() == null) {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getDateFrom() + "\"^^xsd:date) ";
            } else if (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() != null) {
                sparql += "FILTER( ?dissolutionDate <= \"" + dissolutionDate.getDateTo() + "\"^^xsd:date) ";
            } else {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getDateFrom() + "\"^^xsd:date && ?dissolutionDate <= \"" + dissolutionDate.getDateTo() + "\"^^xsd:date) ";
            }
        }
        
        cq.setWhere(sparql);
        
        return cq;
    }

    public static SparqlQuery buildCoreQueryGroupPlace(CountryDB cc, boolean active, boolean name, Collection<String> nuts3, Collection<String> lau, boolean includeNutsLau, Collection<String> nace, Code foundingDate, Code dissolutionDate) {
        
//    	System.out.println(nuts3);
    	SparqlQuery cq = new SparqlQuery(cc.getDataNamedGraph());
    	
        String sparql = "";
        
        sparql += cc.getEntitySparql() + " "; 
        
        if (active && cc.isDissolutionDate()) {
        	if (dissolutionDate == null || (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() == null)) {
	        	sparql += cc.getActiveSparql() + " ";
	        }
        }
        
        if (name) {
        	sparql += cc.getLegalNameSparql() + " ";
        }
        
        
        if (nuts3 != null && (lau == null || lau.size() == 0)) {
        	if (nuts3.size() > 0) {
	        	sparql += cc.getNuts3Sparql() + " ";
	        	
	        	if (includeNutsLau) {
	              sparql += " VALUES ?nuts3 { ";
	              for (String uri : nuts3) {
	                  sparql += "<" + uri + "> ";
	              }
	              sparql += "} ";
	        	}
        	}
        	
           	
        } else if ((nuts3 == null || nuts3.size() == 0) && lau != null) {            
        	if (lau.size() > 0) {
	        	sparql += cc.getLauSparql() + " ";
	        	
	        	if (includeNutsLau) {
	              sparql += " VALUES ?lau { ";
	              for (String uri : lau) {
	                  sparql += "<" + uri + "> ";
	              }
	              sparql += "} ";
	        	}
        	}            
        }

        
        if (nace != null) {
        	sparql += cc.getNaceSparql() + " ";
        	
        	if (cc.getNacePathSparql() != null) {
        	
        		sparql += " ?nace " + cc.getNacePathSparql() + " ?naceroot .";
        		
	            sparql += " VALUES ?naceroot { ";
	            for (String uri : new HashSet<>(nace)) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
        		
        	} else {
	            sparql += " VALUES ?nace { ";
	            for (String uri : new HashSet<>(nace)) {
	                sparql += "<" + uri + "> ";
	            }
	            sparql += "} ";
        	}
            
//            cq.setNace(true);
        }
        

        // Date filter (if requested)
        
        if (foundingDate != null && (foundingDate.getDateFrom() != null || foundingDate.getDateTo() != null)) {
            sparql += cc.getFoundingDateSparql() + " ";

            if (foundingDate.getDateFrom() != null && foundingDate.getDateTo() == null) {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getDateFrom() + "\"^^xsd:date) ";
            } else if (foundingDate.getDateFrom() == null && foundingDate.getDateTo() != null) {
                sparql += "FILTER( ?foundingDate <= \"" + foundingDate.getDateTo() + "\"^^xsd:date) ";
            } else {
                sparql += "FILTER( ?foundingDate >= \"" + foundingDate.getDateFrom() + "\"^^xsd:date && ?foundingDate <= \"" + foundingDate.getDateTo() + "\"^^xsd:date) ";

            }
        }
        
        if (dissolutionDate != null && (dissolutionDate.getDateFrom() != null || dissolutionDate.getDateTo() != null)) {
            sparql += cc.getDissolutionDateSparql() + " ";

            if (dissolutionDate.getDateFrom() != null && dissolutionDate.getDateTo() == null) {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getDateFrom() + "\"^^xsd:date) ";
            } else if (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() != null) {
                sparql += "FILTER( ?dissolutionDate <= \"" + dissolutionDate.getDateTo() + "\"^^xsd:date) ";
            } else {
                sparql += "FILTER( ?dissolutionDate >= \"" + dissolutionDate.getDateFrom() + "\"^^xsd:date && ?dissolutionDate <= \"" + dissolutionDate.getDateTo() + "\"^^xsd:date) ";
            }
        }
        
        cq.setWhere(sparql);
        
        return cq;
    }

    
 }
    

