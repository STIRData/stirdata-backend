package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class NutsService {

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;

    public String getNuts(String parentNode) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT ?code ?label WHERE {\n";
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nuts/ont/level> 0 .\n" +
                    "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label\n" +
                    "}\n";
        }
        else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ".\n" +
                    "  ?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label\n" +
                    "}\n";
        }

        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
//            ResultSetFormatter.output(outStream, rs, ResultsFormat.FMT_RDF_JSONLD);
            json = new String(outStream.toByteArray());
        }
        return json;
    }

}
