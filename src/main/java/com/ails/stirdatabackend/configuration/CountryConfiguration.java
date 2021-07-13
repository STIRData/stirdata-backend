package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.model.SparqlEndpoint;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class CountryConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(CountryConfiguration.class);

    private String country;
    
    private String label;
    
    private SparqlEndpoint dataEndpoint;
    private SparqlEndpoint naceEndpoint;
    private SparqlEndpoint nutsEndpoint;
 
    public CountryConfiguration(String country) {
    	this.country = country;
    }
}
