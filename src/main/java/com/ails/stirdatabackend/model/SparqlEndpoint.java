package com.ails.stirdatabackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SparqlEndpoint {

    private String name;
    private String sparqlEndpoint;

}
