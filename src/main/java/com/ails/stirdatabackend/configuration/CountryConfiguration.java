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
 
    private String entitySparql;
    private String legalNameSparql;
    private String activeSparql;
    private String nuts3Sparql;
    private String naceSparql;
    private String lauSparql;
    private String foundingDateSparql;
    private String dissolutionDateSparql;
    
    public CountryConfiguration(String country) {
    	this.country = country;
    }
    
    public String getEntitySparql() {
    	return entitySparql != null ? entitySparql : modelConfiguration.getEntitySparql();
    }
    
    public String getLegalNameSparql() {
    	return legalNameSparql != null ? legalNameSparql : modelConfiguration.getLegalNameSparql();
    }

    public String getActiveSparql() {
    	return activeSparql != null ? activeSparql : modelConfiguration.getActiveSparql();
    }

    public String getNuts3Sparql() {
    	return nuts3Sparql != null ? nuts3Sparql : modelConfiguration.getNuts3Sparql();
    }

    public String getNaceSparql() {
    	return naceSparql != null ? naceSparql : modelConfiguration.getNaceSparql();
    }

    public String getLauSparql() {
    	return lauSparql != null ? lauSparql : modelConfiguration.getLauSparql();
    }
    
    public String getFoundingDateSparql() {
    	return foundingDateSparql != null ? foundingDateSparql : modelConfiguration.getFoundingDateSparql();
    }

    public String getDissolutionDateSparql() {
    	return dissolutionDateSparql != null ? dissolutionDateSparql : modelConfiguration.getDissolutionDateSparql();
    }
    
}
