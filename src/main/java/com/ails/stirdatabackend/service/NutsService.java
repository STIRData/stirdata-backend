package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    public String getTopLevelNuts(String uri) {
        String sparql = "SELECT ?nuts0 WHERE {\n" +
                "<" +  uri + ">" + "<http://www.w3.org/2004/02/skos/core#broader>* ?nuts0 .\n" +
                "?nuts0 <https://lod.stirdata.eu/nuts/ont/level> 0 .\n" +
                "} ";
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String nuts0 = sol.get("nuts0").asResource().toString();
                return nuts0;
            }
        }
        return null;
    }

    public List<String> normalizeNuts(List<String> nuts) {
        List<String> res = new ArrayList<String>();
        for (String nut : nuts) {
            try {
                URI uri = new URI(nut);
                String path = uri.getPath();
                String lastPart = path.substring(path.lastIndexOf('/') + 1);
                res.add("https://lod.stirdata.eu/nuts/code/" + lastPart);
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return res;
    }

}
