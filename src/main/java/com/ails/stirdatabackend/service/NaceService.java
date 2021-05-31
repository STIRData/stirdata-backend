package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class NaceService {

    @Autowired
    @Qualifier("nace-sparql-endpoint")
    private SparqlEndpoint naceSparqlEndpoint;

    public String getNace(String parentNode, String language) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?code ?label WHERE {\n";
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nace/ont/level> 1 .\n" +
                    "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label\n" +
                    "FILTER (lang(?label) = \"" + language + "\")\n" +
                    "}\n";
        }
        else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ".\n" +
                      "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label\n" +
                      "FILTER (lang(?label) = \"" + language + "\")\n" +
                    "}\n";
        }
        System.out.println(sparql);
        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = new String(outStream.toByteArray());
        }
        return json;
    }
}
