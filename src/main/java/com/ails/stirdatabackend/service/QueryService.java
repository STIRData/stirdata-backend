package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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

    public String query(List<String> nutsList, List<String> naceList) {

    }
}
