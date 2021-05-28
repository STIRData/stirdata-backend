package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.apache.jena.util.URIref.encode;

@Service
public class TestService {

    @Autowired
    @Qualifier("czech-sparql-endpoint")
    private SparqlEndpoint czechSparqlEndpoint;

    @Autowired
    @Qualifier("belgium-sparql-endpoint")
    private SparqlEndpoint belgiumSparqlEndpoint;

    public List<String> testSparqlQueryCzech() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<String>();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(czechSparqlEndpoint.getSparqlEndpoint(), sparql)) {

            ResultSet rs = qe.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                prop.add(sol.get("p").toString());
            }
        }
        return prop;
    }

    public List<String> testSparqlQueryBelgium() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<String>();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(belgiumSparqlEndpoint.getSparqlEndpoint(), sparql)) {

            ResultSet rs = qe.execSelect();

            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                prop.add(sol.get("p").toString());
            }
        }
        return prop;
    }
}
