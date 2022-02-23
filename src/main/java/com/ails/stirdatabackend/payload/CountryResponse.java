package com.ails.stirdatabackend.payload;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ails.stirdatabackend.model.Resource;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class CountryResponse {

	private CodeLabel country;
	
	private int legalEntityCount;
	
	private String conformsTo;
	
	private List<String> placeVocabularies;
	private List<String> activityVocabularies;
	private Interval foundingDate;
	private Interval dissolutionDate;

	private Date lastUpdated;
	
	private Resource source;

	private String sparqlEndpoint;
	
	private String accrualPeriodicity;
	
	public void setCountry(String code, String label) {
		this.country = new CodeLabel(code, label);
	}
	
	public void addPlace(String s) {
		if (placeVocabularies == null) {
			placeVocabularies = new ArrayList<>();
		}
		placeVocabularies.add(s);
	}
	
	public void addActivity(String s) {
		if (activityVocabularies == null) {
			activityVocabularies = new ArrayList<>();
		}
		activityVocabularies.add(s);
	}
	   
	
	
}
