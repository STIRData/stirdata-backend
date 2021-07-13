package com.ails.stirdatabackend.model;

import com.ails.stirdatabackend.configuration.Dimension;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SparqlEndpoint {

    private String country;
    private Dimension dimension;
    
    private String sparqlEndpoint;

}
