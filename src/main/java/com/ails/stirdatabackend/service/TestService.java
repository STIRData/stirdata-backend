package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
import java.net.IDN;

@Service
public class TestService {

    @Autowired
    @Qualifier("czechia-sparql-endpoint")
    private SparqlEndpoint czechiaSparqlEndpoint;

    @Autowired
    @Qualifier("belgium-sparql-endpoint")
    private SparqlEndpoint belgiumSparqlEndpoint;

    @Autowired
    @Qualifier("greece-sparql-endpoint")
    private SparqlEndpoint greeceSparqlEndpoint;

    public List<String> testSparqlQueryCzech() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(IDN.toASCII(czechiaSparqlEndpoint.getSparqlEndpoint()), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                prop.add(sol.get("p").toString());
            }
        }
        return prop;
    }

    public String testSparqlQueryBelgium() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<>();
        String json;
        StringWriter sw = new StringWriter();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(belgiumSparqlEndpoint.getSparqlEndpoint(), sparql)) {

            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
//            ResultSetFormatter.outputAsJSON(outStream, rs);
            ResultSetFormatter.output(outStream, rs, ResultsFormat.FMT_RDF_JSONLD);
            json = outStream.toString();
        }
        return json;
    }
    
    public String testSparqlQueryGreece() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<>();
        String json;
        StringWriter sw = new StringWriter();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(greeceSparqlEndpoint.getSparqlEndpoint(), sparql)) {

            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
//            ResultSetFormatter.outputAsJSON(outStream, rs);
            ResultSetFormatter.output(outStream, rs, ResultsFormat.FMT_RDF_JSONLD);
            json = outStream.toString();
        }
        return json;
    }

}
