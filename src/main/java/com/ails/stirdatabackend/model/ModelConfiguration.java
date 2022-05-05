package com.ails.stirdatabackend.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelConfiguration {

    private String model;
    
    private String url;
    
    private String entitySparql;
    private String legalNameSparql;
    private String tradingNameSparql;
    private String activeSparql;
    private String naceSparql;
    private String companyTypeSparql;
    private String addressSparql;
    private String nuts3Sparql;
    private String lauSparql;
    private String foundingDateSparql;
    private String dissolutionDateSparql;
    
    public ModelConfiguration(String model) {
    	this.model = model;
    }
    
}
