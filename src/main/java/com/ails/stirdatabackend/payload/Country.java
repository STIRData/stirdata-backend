package com.ails.stirdatabackend.payload;


import java.util.Date;

import javax.persistence.Column;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
public class Country {
	
	private String code;
	
	private String label;
	
	private String dcat;

	private String naceEnpoint;

	private String naceScheme;
	
	private String nacePath1;
	
	private String nacePath2;
	
	private String nacePath3;
	
	private String nacePath4;

	private String naceNamespace;
	
	private Integer naceFixedLevel;
	
    private String naceNamedGraph;

    private String nutsEndpoint;
	
    private String nutsNamedGraph;
	
	private String nutsPrefix;
	
	private String lauPrefix;
	
	private String legalEntityPrefix;

    private String dataNamedGraph;
	
    private String entitySparql;
    
    private String legalNameSparql;
    
    private String activeSparql;
    
    private String addressSparql;
    
    private String nuts3Sparql;
    
    private String naceSparql;
    
    private String nacePathSparql;
    
    private String lauSparql;
    
    private String foundingDateSparql;
    
    private String dissolutionDateSparql;
    
    private String licenceLabel;
    
    private String licenceUri;
    
		
	public Country(String code) {
		this.code = code;
	}
	
	public void setNaceScheme(String scheme, String namespace) {
		this.naceScheme = scheme;
		this.naceNamespace = namespace;
	}
	
	public void setLicense(String label, String uri) {
		this.licenceLabel = label;
		this.licenceUri = uri;
	}
	
}
