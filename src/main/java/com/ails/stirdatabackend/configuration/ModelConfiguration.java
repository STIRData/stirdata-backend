package com.ails.stirdatabackend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class ModelConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(ModelConfiguration.class);

    private String model;
    
    private String url;
    
    private String entitySparql;
    private String legalNameSparql;
    private String activeSparql;
    private String nuts3Sparql;
    private String naceSparql;
    private String lauSparql;
    private String foundingDateSparql;
    private String dissolutionDateSparql;
    
    public ModelConfiguration(String model) {
    	this.model = model;
    }
    
    
}
