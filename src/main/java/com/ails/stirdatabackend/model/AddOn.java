package com.ails.stirdatabackend.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class AddOn {
	
	private String country;
	private String endpoint;
	private String namedGraph;
	
	private String entitySparql;
	
	Map<String, AddOnProperty> properties;
	
	public AddOn() {
		properties = new LinkedHashMap<>();
	}
	
	public void addProperty(String name, String sparql) {
		properties.put(name,new AddOnProperty(name, sparql));
	}
	
	
	public void ask(String entity) {
		String sparql = "SELECT * " + (namedGraph != null ? "FROM <" + namedGraph + ">" : "") + "WHERE {" +
			entitySparql + " " + " VALUES ?entity { <" + entity + "> } ";
		
		for (Map.Entry<String, AddOnProperty> entry : properties.entrySet()) {
			AddOnProperty aop = entry.getValue();
			
			sparql += aop.getSparql() + " ";
		}
		
		sparql += " }"; 
		
		System.out.println(QueryFactory.create(sparql));
		
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                
                System.out.println(sol);
            
            }
        }
	}
	

	@Getter
	@Setter
	@AllArgsConstructor
	public class AddOnProperty {
		String name;
		String sparql;
	}
}
