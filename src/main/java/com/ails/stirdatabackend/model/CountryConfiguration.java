package com.ails.stirdatabackend.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "countries")
public class CountryConfiguration {

	@Id
	private String id;
	
    private String countryCode;
    private String countryLabel;
    
	private int legalEntityCount;
	
	private String conformsTo;
	
	private Date lastUpdated;
	
//	private String source;

	private String accrualPeriodicity;
    
	@Transient
	@JsonIgnore
    private ModelConfiguration modelConfiguration;
    
    private String dataEndpoint;
    private String dataNamedGraph;
    private String naceEndpoint;
    private String naceNamedGraph;
    private String nutsEndpoint;
    private String nutsNamedGraph;
    
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
    private boolean foundingDate;
    private String foundingDateFrom;
    private String foundingDateTo;
    private boolean dissolutionDate;
    private String dissolutionDateFrom;
    private String dissolutionDateTo;
 
    private String entitySparql;
    private String legalNameSparql;
    private String activeSparql;
    private String nuts3Sparql;
    private String naceSparql;
    private String lauSparql;
    private String foundingDateSparql;
    private String dissolutionDateSparql;

    @Transient
    @JsonIgnore
    private Set<Dimension> statistics;
    
    public CountryConfiguration(String country) {
    	this.countryCode = country;
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
