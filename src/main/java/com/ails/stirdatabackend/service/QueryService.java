package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.EndpointManager;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.utils.URIMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.IDN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


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

    @Value("${page.size}")
    private int pageSize;

    public enum SUPPORTED_COUNTRIES {
        BELGIUM,
        CZECH,
        NORWAY
    }

    // We suppose that NUTS3 is provided.
    // Only NUTS is handled right now.
    public List<EndpointResponse> paginatedQuery(List<String> nutsList, List<String> naceList, Optional<String> startDateOpt, Optional<String> endDateOpt, int page) {
        List<EndpointResponse> responseList = new ArrayList<EndpointResponse>();
        HashMap<SparqlEndpoint, List<String>> requestMap = endpointManager.getEndpointsByNuts(nutsList);
        System.out.println(requestMap.toString());
        int offset = (page - 1) * pageSize;
        for (SparqlEndpoint endpoint : requestMap.keySet()) {
            HashMap<String, String> mappedNutsUris = endpoint.getName().equals("czech-endpoint") ? uriMapper.mapCzechNutsUri(requestMap.get(endpoint))
                                                                                                 : uriMapper.mapEuropaNutsUri(requestMap.get(endpoint));

            String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
                            "PREFIX regorg: <http://www.w3.org/ns/regorg#>\n" +
                            "PREFIX ebg: <http://data.businessgraph.io/ontology#>\n" +
                            "PREFIX org: <http://www.w3.org/ns/org#>\n" +
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

            if (endpoint.getName().equals("czech-endpoint")) {
                sparql += "PREFIX schema: <http://schema.org/>";
            } else {
                sparql += "PREFIX schema: <https://schema.org/>";
            }


            sparql +=       "SELECT DISTINCT ?organization\n" +
                            "WHERE {\n" +
                            "  ?organization a regorg:RegisteredOrganization ;\n" +
                            "    regorg:legalName ?organizationName ;\n" +
                            "    org:hasRegisteredSite ?registeredSite .\n" +
                            " \n" +
                            "  ?registeredSite org:siteAddress ?address .\n" +
                            " \n";
            if (endpoint.getName().equals("czech-endpoint")) {
                sparql +=   "  ?address ebg:adminUnitL4 ?NUTS3 .\n";
            }
            else {
                sparql += "  ?address <http://lod.stirdata.eu/model/nuts3> ?NUTS3 .\n";
            }

            sparql += " VALUES ?NUTS3 { ";
            for (String uri : requestMap.get(endpoint)) {
                sparql += "<" + mappedNutsUris.get(uri) + "> ";
            }

            sparql += "}\n";

            // Date filter (if requested)
            if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
                sparql += "?organization schema:foundingDate ?foundingDate\n";

                if (startDateOpt.isPresent() && !endDateOpt.isPresent()) {
                    String startDate = startDateOpt.get();
                    sparql += "FILTER( ?foundingDate > \"" + startDate + "\"^^xsd:date)";
                }
                else if (!startDateOpt.isPresent() && endDateOpt.isPresent()) {
                    String endDate = endDateOpt.get();
                    sparql += "FILTER( ?foundingDate < \"" + endDate + "\"^^xsd:date)";
                }
                else {
                    String startDate = startDateOpt.get();
                    String endDate = endDateOpt.get();
                    sparql += "FILTER( ?foundingDate > \"" + startDate + "\"^^xsd:date && ?foundingDate < \"" + endDate + "\"^^xsd:date)";

                }
            }
            sparql += "}\n";
            sparql +=  "LIMIT " + pageSize + " OFFSET " + offset;

            System.out.println("Will query endpoint: "+endpoint.getSparqlEndpoint());
            System.out.println(QueryFactory.create(sparql));
            List<String> companyUris = new ArrayList<String>();

            StringWriter sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), sparql)) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    companyUris.add(sol.get("organization").asResource().toString());
                }
            }
            System.out.println(companyUris);

            String sparqlConstruct = "CONSTRUCT { ?company ?p ?q } WHERE {\n" +
                                    "  ?company ?p ?q . \n" +
                                    "  VALUES ?company {";
            for (String uri : companyUris) {
                sparqlConstruct += " <" + uri + "> ";
            }
            sparqlConstruct += "}\n" +
                                    "}\n";

//            Writer sw = new StringWriter();
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint.getSparqlEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
                Model model = qe.execConstruct();
                RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                responseList.add(new EndpointResponse(endpoint.getName(), mapper.readTree(sw.toString())));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return responseList;
    }
}
