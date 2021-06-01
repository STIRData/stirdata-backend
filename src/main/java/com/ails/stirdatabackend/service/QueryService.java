package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.EndpointManager;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.utils.URIMapper;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
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

    @Autowired
    private NutsService nutsService;

    @Autowired
    private URIMapper uriMapper;

    public enum SUPPORTED_COUNTRIES {
        BELGIUM,
        CZECH,
        NORWAY
    }

    // We suppose that NUTS3 is provided.
    // Only NUTS is handled
    public void query(List<String> nutsList, List<String> naceList) {
        HashMap<SparqlEndpoint, List<String>> requestMap = endpointManager.getEndpointsByNuts(nutsList);
        System.out.println(requestMap.toString());
        for (SparqlEndpoint endpoint : requestMap.keySet()) {
            String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";

            if (endpoint.getName().equals("czech-endpoint")) {
                sparql += "PREFIX regorg: <http://www.w3.org/ns/regorg#>\n";
            }
            if (endpoint.getName().equals("czech-endpoint")) {
                sparql += "SELECT DISTINCT ?organizationName\n" +
                        "WHERE {\n" +
                        "  ?organization a regorg:RegisteredOrganization ;\n" +
                        "    regorg:legalName ?organizationName ;\n" +
                        "    org:hasRegisteredSite ?registeredSite .\n" +
                        " \n" +
                        "  ?registeredSite org:siteAddress ?address .\n" +
                        " \n" +
                        "  ?address ebg:adminUnitL4 ?NUTS3 .\n" +
                        " VALUES ?NUTS3 { ";
                HashMap<String, String> mappedUris = uriMapper.mapCzechNutsUri(requestMap.get(endpoint));
                for (String uri : requestMap.get(endpoint)) {
                    sparql += "<" + mappedUris.get(uri) + "> ";
                }
                sparql += "}\n" +
                        "}\n";
            }
            else {
                sparql += "SELECT ?code WHERE {\n" +
                        "  ?code <http://www.w3.org/ns/org#hasSite> ?site .\n" +
                        "  ?site <http://www.w3.org/ns/org#siteAddress> ?address . \n" +
                        "  ?address <http://lod.stirdata.eu/model/nuts3> ?NUTS3 .\n" +
                        "  VALUES ?NUTS3 { ";
                HashMap<String, String> mappedUris = uriMapper.mapEuropaNutsUri(requestMap.get(endpoint));
                for (String uri : requestMap.get(endpoint)) {
                    sparql += "<" + mappedUris.get(uri) + "> ";
                }
                sparql += "}\n" +
                        "}\n";
                System.out.println("Will query endpoint: "+endpoint.getSparqlEndpoint());
                System.out.println(sparql);
            }


            if (endpoint.getName().equals("czech-endpoint")) {
                HashMap<String, String> mappedNuts = uriMapper.mapCzechNutsUri(requestMap.get(endpoint));

            }
        }
    }
}
