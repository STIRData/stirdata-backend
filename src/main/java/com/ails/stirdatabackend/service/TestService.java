package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.resultset.RDFOutput;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
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

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;

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

    public String testSparqlQueryBelgium() {
        String sparql = "SELECT * WHERE {?p ?q ?r } LIMIT 10";
        List<String> prop = new ArrayList<String>();
        String json;
        StringWriter sw = new StringWriter();
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(belgiumSparqlEndpoint.getSparqlEndpoint(), sparql)) {

            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
//            ResultSetFormatter.outputAsJSON(outStream, rs);
            ResultSetFormatter.output(outStream, rs, ResultsFormat.FMT_RDF_JSONLD);
            json = new String(outStream.toByteArray());
        }
        return json;
    }
    
}
