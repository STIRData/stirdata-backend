package com.ails.stirdatabackend.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddOn {
	
	private String name;
	private String label;
	
	private String country;
	private String endpoint;
	private String namedGraph;
	private String orderBy;
	
	private String entitySparql;
	
	private Map<String, AddOnProperty> properties;
	
	public AddOn() {
		properties = new LinkedHashMap<>();
	}
	
	public void addProperty(String name, String sparql, String label) {
		properties.put(name,new AddOnProperty(name, sparql, label));
	}
	
	public Object ask(String entity) {
		String sparql = "SELECT * " + (namedGraph != null ? "FROM <" + namedGraph + ">" : "") + "WHERE {" +
			entitySparql + " " + " VALUES ?entity { <" + entity + "> } ";
		
		for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
			AddOnProperty aop = entry.getValue();
		
			sparql += aop.getSparql() + " ";
		}
		
		sparql += " }";
		
		if (orderBy != null) {
			sparql += "ORDER BY " + orderBy;
		}
		
//		System.out.println(QueryFactory.create(sparql));
		
		Map<String, Object> res = new HashMap<>();
		List<Map<String,String>> list = new ArrayList<>();
		
		res.put("label", label);
		res.put("results", list);
		
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
        
                Map<String, String> result = new HashMap<>();
                
//                System.out.println(sol);
                
                for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
        			AddOnProperty aop = entry.getValue();
        			
        			RDFNode solution = sol.get(name + "" + aop.getName().substring(0,1).toUpperCase() + aop.getName().substring(1));
        			if (solution.isResource()) {
        				result.put(aop.getLabel(), solution.toString());
        			} else {
        				result.put(aop.getLabel(), solution.asLiteral().getLexicalForm());
        			}
        			
        			sparql += aop.getSparql() + " ";
        		}
                
                list.add(result);
            }
        }
    	
    	return res;
	}
	

	@Getter
	@Setter
	@AllArgsConstructor
	public class AddOnProperty {
		String name;
		String sparql;
		String label;
	}
}
