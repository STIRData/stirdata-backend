package com.ails.stirdatabackend.payload;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CountryResponse {

	private CodeLabel country;
	
	private Integer legalEntityCount;
	private Integer activeLegalEntityCount;
	
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
