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
    
    private ModelConfiguration modelConfiguration;
    
    private SparqlEndpoint dataEndpoint;
    private SparqlEndpoint naceEndpoint;
    private SparqlEndpoint nutsEndpoint;
    
    private String naceScheme;
    private String nacePath1;
    private String nacePath2;
    private String nacePath3;
    private String nacePath4;
    private int naceFixedLevel;
    
    private String nutsPrefix;
    private String lauPrefix;
    
    private boolean lau;
    private boolean nuts;
    private boolean nace;
 
    public CountryConfiguration(String country) {
    	this.country = country;
    }
    
    
}
