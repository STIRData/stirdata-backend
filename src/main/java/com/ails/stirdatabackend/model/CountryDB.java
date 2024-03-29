package com.ails.stirdatabackend.model;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Countries")
public class CountryDB {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	
	@Column(unique = true)
	private String code;
	
	private String label;
	
	@Column(unique = true, nullable = false)
	private String dcat;
	
	@Column(name = "conforms_to", columnDefinition="TEXT")
	private String conformsTo;
	
	@Column(name = "source_uri", columnDefinition="TEXT")
	private String sourceUri;
	
	@Column(name = "source_label", columnDefinition="TEXT")
	private String sourceLabel;
	
	@Column(name = "last_updated")
	private Date lastUpdated;

	@Column(name = "last_accessed")
	private Date lastAccessed;

	@Column(name = "last_accessed_start")
	private Date lastAccessedStart;

	@Column(name = "last_accessed_end")
	private Date lastAccessedEnd;

	@Column(name = "accrual_periodicity", columnDefinition="TEXT")
	private String accrualPeriodicity;

	@Column(name = "data_endpoint", columnDefinition="TEXT")
	private String dataEndpoint;

	@Column(name = "nace_endpoint", columnDefinition="TEXT")
	private String naceEndpoint;
	
//	@Column(name = "nace_scheme", columnDefinition="TEXT")
//	private String naceScheme;
	
//	@Column(name = "nace_path_1", columnDefinition="TEXT")
//	private String nacePath1;
//	
//	@Column(name = "nace_path_2", columnDefinition="TEXT")
//	private String nacePath2;
//	
//	@Column(name = "nace_path_3", columnDefinition="TEXT")
//	private String nacePath3;
//	
//	@Column(name = "nace_path_4", columnDefinition="TEXT")
//	private String nacePath4;
	
//	@Column(name = "nace_fixed_level")
//	private Integer naceFixedLevel;

	@Column(name = "nace_fixed_levels")
	private String naceFixedLevels;

	@Column(name = "nace_levels")
	private Integer naceLevels;

//	@Column(name = "nace_namespace")
//	private String naceNamespace;

	public String getNaceNamespace() {
		return "nace-" + code.toLowerCase(); 
	}
	
	@Column(name = "nace_languages")
	private String naceLanguages;
	
	@Transient
	private String[] naceLanguagesArray;

	@Column(name = "nace_prefix", columnDefinition="TEXT")
    private String nacePrefix;

//	@Column(name = "nace_named_graph", columnDefinition="TEXT")
//    private String naceNamedGraph;

	@Column(name = "nuts_endpoint", columnDefinition="TEXT")
    private String nutsEndpoint;
	
	@Column(name = "nuts_named_graph", columnDefinition="TEXT")
    private String nutsNamedGraph;
	
	@Column(name = "nuts_prefix", columnDefinition="TEXT")
	private String nutsPrefix;
	
	@Column(name = "lau_prefix", columnDefinition="TEXT")
	private String lauPrefix;

	@Column(name = "legal_entity_prefix", columnDefinition="TEXT")
	private String legalEntityPrefix;
	
	@Column(name = "active_legal_entity_count")
	private Integer activeLegalEntityCount;

	@Column(name = "total_legal_entity_count")
	private Integer totalLegalEntityCount;

	@Column(name = "data_named_graph", columnDefinition="TEXT")
    private String dataNamedGraph;
	
	@Column(name = "company_type_scheme", columnDefinition="TEXT")
	private String companyTypeScheme;
	
	@Column(name = "company_type_prefix", columnDefinition="TEXT")
    private String companyTypePrefix;
	
	@Column(name = "company_type_namespace")
	private String companyTypeNamespace;

	@Column(name = "company_type_endpoint", columnDefinition="TEXT")
    private String companyTypeEndpoint;
	
	@Column(name = "company_type_languages")
	private String companyTypeLanguages;

	@Transient
	private String[] companyTypeLanguagesArray;
    
    private boolean lau;
    private boolean nuts;
    private boolean nace;
    
    @Column(name = "company_type")
    private Boolean companyType;
    
    @Column(name = "legal_name")
    private boolean legalName;
    
    @Column(name = "trading_name")
    private boolean tradingName;
    
    @Column(name = "founding_date")
    private boolean foundingDate;
    
    @Column(name = "founding_date_from")
    private java.sql.Date foundingDateFrom;
    
    @Column(name = "founding_date_to")
    private java.sql.Date foundingDateTo;
    
    @Column(name = "dissolution_date")
    private boolean dissolutionDate;

    @Column(name = "dissolution_date_from")
    private java.sql.Date dissolutionDateFrom;

    @Column(name = "dissolution_date_to")
    private java.sql.Date dissolutionDateTo;
 
    @Column(name = "entity_sparql")
    private String entitySparql;
    
    @Column(name = "legal_name_sparql")
    private String legalNameSparql;

    @Column(name = "trading_name_sparql")
    private String tradingNameSparql;

    @Column(name = "active_sparql")
    private String activeSparql;
    
    @Column(name = "nace_sparql")
    private String naceSparql;
    
	@Column(name = "nace_path_sparql")
	private String nacePathSparql;

    @Column(name = "address_sparql")
    private String addressSparql;

    @Column(name = "nuts3_sparql")
    private String nuts3Sparql;

    @Column(name = "lau_sparql")
    private String lauSparql;

    @Column(name = "company_type_sparql")
    private String companyTypeSparql;

    @Column(name = "founding_date_sparql")
    private String foundingDateSparql;
    
    @Column(name = "dissolution_date_sparql")
    private String dissolutionDateSparql;

    @Column(name = "stats_data_date")
    private Date statsDataDate;

    @Column(name = "stats_nutslau_date")
    private Date statsNutsLauDate;
    
    @Column(name = "stats_nace_date")
    private Date statsNaceDate;
    
    @Column(name = "stats_founding_date")
    private Date statsFoundingDate;
    
    @Column(name = "stats_dissolution_date")
    private Date statsDissolutionDate;
    
    @Column(name = "stats_nutslau_nace_date")
    private Date statsNutsLauNaceDate;

    @Column(name = "stats_nutslau_founding_date")
    private Date statsNutsLauFoundingDate;

    @Column(name = "stats_nutslau_dissolution_date")
    private Date statsNutsLauDissolutionDate;

    @Column(name = "stats_nace_founding_date")
    private Date statsNaceFoundingDate;

    @Column(name = "stats_nace_dissolution_date")
    private Date statsNaceDissolutionDate;

    @Column(name = "license_label")
    private String licenseLabel;

    @Column(name = "license_uri")
    private String licenseUri;
    
    @Column(name = "lei_code_sparql")
    private String leiCodeSparql;

    @Column(name = "same_as_sparql")
    private String sameAsSparql;
    
    @Column(name = "last_indexed")
    private Date lastIndexed;

    @Column(name = "index_name")
    private String indexName;

	@Transient
    private ModelConfiguration modelConfiguration;
	
    @Transient
    private boolean naceEffectiveLevelsComputed;
    
    @Transient
    private int[] naceEffectiveLevels;

    @Transient
    public boolean modified = false;
    
//    @Transient
//    private Set<Dimension> statistics;

    public String getEntitySparql() {
    	return entitySparql != null ? entitySparql : modelConfiguration.getEntitySparql();
    }
    
    public String getLegalNameSparql() {
    	return legalNameSparql != null ? legalNameSparql : modelConfiguration.getLegalNameSparql();
    }

    public String getTradingNameSparql() {
    	return tradingNameSparql != null ? tradingNameSparql : modelConfiguration.getTradingNameSparql();
    }

    public String getActiveSparql() {
    	return activeSparql != null ? activeSparql : modelConfiguration.getActiveSparql();
    }

    public String getNaceSparql() {
    	return naceSparql != null ? naceSparql : modelConfiguration.getNaceSparql();
    }

    public String getCompanyTypeSparql() {
    	return companyTypeSparql != null ? companyTypeSparql : modelConfiguration.getCompanyTypeSparql();
    }

    public String getAddressSparql() {
    	return addressSparql != null ? addressSparql : modelConfiguration.getAddressSparql();
    }

    public String getNuts3Sparql() {
    	return nuts3Sparql != null ? nuts3Sparql : modelConfiguration.getNuts3Sparql();
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
    
    public String getLeiCodeSparql() {
    	return leiCodeSparql != null ? leiCodeSparql : modelConfiguration.getLeiCodeSparql();
    }
    
    public String getSameAsSparql() {
    	return sameAsSparql != null ? sameAsSparql : modelConfiguration.getSameAsSparql();
    }
    
    public Date getStatsDate(Dimension dimension) {
    	if (dimension == Dimension.DATA) {
    		return this.getStatsDataDate();
    	} else if (dimension == Dimension.NACE) {
    		return this.getStatsNaceDate();
    	} else if (dimension == Dimension.NUTSLAU) {
    		return this.getStatsNutsLauDate();
    	} else if (dimension == Dimension.FOUNDING) {
    		return this.getStatsFoundingDate();
    	} else if (dimension == Dimension.DISSOLUTION) {
    		return this.getStatsDissolutionDate();
    	} else if (dimension == Dimension.NUTSLAU_FOUNDING) {
    		return this.getStatsNutsLauFoundingDate();
    	} else if (dimension == Dimension.NUTSLAU_DISSOLUTION) {
    		return this.getStatsNutsLauDissolutionDate();
    	} else if (dimension == Dimension.NUTSLAU_NACE) {
    		return this.getStatsNutsLauNaceDate();
    	} else if (dimension == Dimension.NACE_FOUNDING) {
    		return this.getStatsNaceFoundingDate();
    	} else if (dimension == Dimension.NACE_DISSOLUTION) {
    		return this.getStatsNaceDissolutionDate();
    	}
    	
    	return null;
    }
    
    public String getPreferredNaceLanguage() {
    	if (naceLanguages != null) {
    		if (naceLanguagesArray == null) { 
    			naceLanguagesArray = naceLanguages.split(",");
    		}
   	    	return naceLanguagesArray[0]; 
    	}
    	return null;
    }
    
    public String getPreferredCompanyTypeLanguage() {
    	if (companyTypeLanguages != null) {
    		if (companyTypeLanguagesArray == null) { 
    			companyTypeLanguagesArray = companyTypeLanguages.split(",");
    		}
   	    	return companyTypeLanguagesArray[0]; 
    	}
    	return null;
    }
    
    public String toString() {
    	return code;
    }
    
    public Date getMinStatsDate() {
    	
        Date date = statsDataDate;

        if (date == null) {
        	date = statsNutsLauDate;
        }
        if (date == null) {
        	date = statsNaceDate;
        }
        if (date == null) {
        	date = statsFoundingDate;
        }
        if (date == null) {
        	date = statsDissolutionDate;
        }
        if (date == null) {        
        	date = statsNutsLauNaceDate;
        }
        if (date == null) {
        	date = statsNutsLauFoundingDate;
        }
        if (date == null) {
        	date = statsNutsLauDissolutionDate;
        }
        if (date == null) {
        	date = statsNaceFoundingDate;
        }
        if (date == null) {
        	date = statsNaceDissolutionDate;
        }
        
        if (date == null) {
        	return null;
        }

        if (statsDataDate != null && statsDataDate.before(date)) {
        	date = statsDataDate;
        }
        
        if (statsNutsLauDate != null && statsNutsLauDate.before(date)) {
        	date = statsNutsLauDate;
        }

        if (statsNaceDate != null && statsNaceDate.before(date)) {
        	date = statsNaceDate;
        }

        if (statsFoundingDate != null && statsFoundingDate.before(date)) {
        	date = statsFoundingDate;
        }

        if (statsDissolutionDate != null && statsDissolutionDate.before(date)) {
        	date = statsDissolutionDate;
        }

        if (statsNutsLauNaceDate != null && statsNutsLauNaceDate.before(date)) {
        	date = statsNutsLauNaceDate;
        }

        if (statsNutsLauFoundingDate != null && statsNutsLauFoundingDate.before(date)) {
        	date = statsNutsLauFoundingDate;
        }

        if (statsNutsLauDissolutionDate != null && statsNutsLauDissolutionDate.before(date)) {
        	date = statsNutsLauDissolutionDate;
        }

        if (statsNaceFoundingDate != null && statsNaceFoundingDate.before(date)) {
        	date = statsNaceFoundingDate;
        }

        if (statsNaceDissolutionDate != null && statsNaceDissolutionDate.before(date)) {
        	date = statsNaceDissolutionDate;
        }
        
        return date ;
    	
    }
    
    
    public int[] getEffectiveNaceLevels() {
    	if (!naceEffectiveLevelsComputed) {
//    		if (naceFixedLevel != null) {
    		if (naceFixedLevels != null) {
    			String[] levels = naceFixedLevels.split(",");
    			
    			naceEffectiveLevels = new int[levels.length];
    			for (int i = 0; i < levels.length; i++) {
    				naceEffectiveLevels[i] = Integer.parseInt(levels[i]);
    			}
    		}
    		naceEffectiveLevelsComputed = true;
    	}
    	
    	return naceEffectiveLevels;
    		
    	
    }
    
    public String getIsoCode() {
    	if (code == null) {
    		return code;
    	}
    	
    	if (code.equals("EL")) {
    		return "GR";
    	} else if (code.equals("UK")) {
        		return "GB";
    	} else {
    		return code;
    	}
    }
    
}
