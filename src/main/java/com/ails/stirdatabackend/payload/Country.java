package com.ails.stirdatabackend.payload;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Country {
	
	private String code;
	
	private String label;
	
	private String dcat;

	private String naceEnpoint;

	private String naceScheme;
	
//	private String nacePath1;
//	
//	private String nacePath2;
//	
//	private String nacePath3;
//	
//	private String nacePath4;

	private String naceNamespace;
	
//	private Integer naceFixedLevel;
	
	private int[] naceFixedLevels;
	
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
    
//    private String nacePathSparql;
    
    private String companyTypeSparql;
    
    private String lauSparql;
    
    private String foundingDateSparql;
    
    private String dissolutionDateSparql;
    
    private String licenceLabel;
    
    private String licenceUri;
    
    private String companyTypeScheme;

    private String companyTypeNamespace;
    
    private String companyTypeEndpoint;

	public Country() {
	}    
		
	public Country(String code) {
		this.code = code;
	}
	
	public void setNaceScheme(String scheme, String namespace) {
		this.naceScheme = scheme;
		this.naceNamespace = namespace;
	}
	
	public void setCompanyTypeScheme(String scheme, String namespace) {
		this.companyTypeScheme = scheme;
		this.companyTypeNamespace = namespace;
	}
	
	public void setLicense(String label, String uri) {
		this.licenceLabel = label;
		this.licenceUri = uri;
	}
	
	public void setNaceFixedLevels(int... l) {
		this.naceFixedLevels = l;
	}
	
}
