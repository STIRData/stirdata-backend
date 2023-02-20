package com.ails.stirdatabackend.payload;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ails.stirdatabackend.model.Code;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalEntity {
	
	private String uri;
	private List<LanguageString> legalNames;
	private CodeLabel companyType;
	private List<CodeLabel> companyActivities;
	private List<Address> registeredAddresses;
	private Date foundingDate;
	private Date dissolutionDate;
	private String leiCode;
	
	private List addOns;

	public LegalEntity(String uri) {
		this.uri = uri;
	}
	
	public void addLegalName(String name) {
		addLegalName(name, null);
	}
	
	public void addLegalName(String name, String language) {
		if (legalNames == null) {
			legalNames = new ArrayList<>();
		}
		
		legalNames.add(new LanguageString(name, language));
	}
	
	public void addCompanyActivity(CodeLabel activity) {
		if (companyActivities == null) {
			companyActivities = new ArrayList<>();
		}
		
		companyActivities.add(activity);
	}
	
	public void addRegisteredAddress(Address address) {
		if (registeredAddresses == null) {
			registeredAddresses = new ArrayList<>();
		}
		
		registeredAddresses.add(address);
	}

}
