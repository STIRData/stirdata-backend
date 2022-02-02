package com.ails.stirdatabackend.util;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;

public class VirtuosoSelectIterator implements AutoCloseable {
	
	private static int VIRTUOSO_LIMIT = 10000;
	
	private String sparql;

	private int iter;
	private String offset; 
	
	private String location;
	
	private QueryExecution qe;
	private ResultSet rs; 
	
	private int returned;
	
	public VirtuosoSelectIterator(String location, String sparql) {
		this.location = location;
		this.sparql = sparql + " LIMIT " + VIRTUOSO_LIMIT + " ";
		
		iter = 0;
		offset = "";
		
		returned = 0;
		
		execute();
	}

	private void execute() {
//		System.out.println(location);
//		System.out.println(QueryFactory.create(sparql + offset, Syntax.syntaxARQ));
		
		qe = QueryExecutionFactory.sparqlService(location, QueryFactory.create(sparql + offset, Syntax.syntaxARQ));
		rs = qe.execSelect();
	}
	
	public boolean hasNext() {
		if (rs.hasNext()) {
			return true;
		} else if (returned == VIRTUOSO_LIMIT) {
			iter++;
			offset = " OFFSET " + iter*VIRTUOSO_LIMIT;

			returned = 0;

			qe.close();
			
			execute();
			
			return hasNext();
		}
		
		return false;
	}
	
	public QuerySolution next() {
		returned++;
		return rs.next();
	}
	
	public void close() {
		qe.close();
	}

}
