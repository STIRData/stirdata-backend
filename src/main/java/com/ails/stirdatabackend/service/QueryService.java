package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.EndpointManager;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;


@Service
public class QueryService {

    @Autowired
    @Qualifier("czech-sparql-endpoint")
    private SparqlEndpoint czechSparqlEndpoint;

    @Autowired
    @Qualifier("belgium-sparql-endpoint")
    private SparqlEndpoint belgiumSparqlEndpoint;

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;

    @Autowired
    private EndpointManager endpointManager;

    public enum SUPPORTED_COUNTRIES {
        BELGIUM,
        CZECH,
        NORWAY
    }

    public SparqlEndpoint getCountryOfNuts(String uri) {
        String sparql = "SELECT ?nuts0 WHERE {\n" +
                        "<" +  uri + ">" + "<http://www.w3.org/2004/02/skos/core#broader>* ?nuts0 .\n" +
                        "?nuts0 <https://lod.stirdata.eu/nuts/ont/level> 0 .\n" +
                        "} ";
        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsSparqlEndpoint.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            SparqlEndpoint res = null;
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String nuts0 = sol.get("nuts0").asResource().toString();
                res = endpointManager.getEndpointFromNutsUri(nuts0);
                System.out.println("Will query here: " + res.getSparqlEndpoint());

            }
            return res;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    public String query(List<String> nutsList, List<String> naceList) {
//        for (String nuts : nutsList) {
//
//        }
//    }
}
