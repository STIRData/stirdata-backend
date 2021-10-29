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
    
    private String entitySparql;
    private String entityNameSparql;
    private String nuts3Sparql;
    private String naceSparql;
    private String foundingDateSparql;
    
    private String naceScheme;
    private String nacePath1;
    private String nacePath2;
    private String nacePath3;
    private String nacePath4;
    private int naceFixedLevel;
    
    private String nutsPrefix;
 
    public CountryConfiguration(String country) {
    	this.country = country;
    }
    
    
}
