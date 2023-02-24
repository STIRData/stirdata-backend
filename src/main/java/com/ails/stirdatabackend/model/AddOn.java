package com.ails.stirdatabackend.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
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
//	private String orderBy;
	
	private String sparql;
//	private String entitySparql;
	
	private Map<String, AddOnProperty> properties;
	
	public AddOn() {
		properties = new LinkedHashMap<>();
	}
	
	public void addProperty(String name, String sparql, String label) {
		properties.put(name,new AddOnProperty(name, sparql, label));
	}
	
	public Object ask(String entity) {
//		String sparql = "SELECT * " + (namedGraph != null ? "FROM <" + namedGraph + ">" : "") + "WHERE {" +
//			entitySparql + " " + " VALUES ?entity { <" + entity + "> } ";
//		
//		for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
//			AddOnProperty aop = entry.getValue();
//		
//			sparql += aop.getSparql() + " ";
//		}
//		
//		sparql += " }";
//		
//		if (orderBy != null) {
//			sparql += "ORDER BY " + orderBy;
//		}
		
//		System.out.println(QueryFactory.create(sparql));
		
		Map<String, Object> res = new HashMap<>();
		List<Map<String,String>> list = new ArrayList<>();
		
		res.put("label", label);
		
		List<Object> fields = new ArrayList<>();
		for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
			AddOnProperty aop = entry.getValue();
			
			Map<String,Object> field = new HashMap<>();
			field.put("name", aop.getName());
			field.put("label", aop.getLabel());
			fields.add(field);
		}
		res.put("fields", fields);
		
		res.put("results", list);
		
		String ssparql = sparql.replaceAll("\\{@@ENTITY@@\\}", "<" + entity + ">"); 
		
//		System.out.println(QueryFactory.create(ssparql));
//		System.out.println(namedGraph);
//		
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, ssparql, namedGraph)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
        
                Map<String, String> result = new HashMap<>();
                
                for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
        			AddOnProperty aop = entry.getValue();
        			
        			RDFNode solution = sol.get(aop.getName());
        			if (solution != null) {
	        				
	        			if (solution.isResource()) {
	        				result.put(aop.getName(), solution.toString());
	        			} else {
	        				result.put(aop.getName(), solution.asLiteral().getLexicalForm());
	        			}
        			}
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
