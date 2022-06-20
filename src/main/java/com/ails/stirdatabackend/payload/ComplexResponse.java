package com.ails.stirdatabackend.payload;

import java.util.List;

import com.ails.stirdatabackend.model.Statistic;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplexResponse {

	@JsonProperty("selection")
	private GenericResponse entity;

	@JsonProperty("activityGroups")
	private List<GenericResponse> activities;
	
	@JsonProperty("placeGroups")
	private List<GenericResponse> places;
	
	@JsonProperty("foundingDateGroups")
	private List<GenericResponse> foundingDates;
	
	@JsonProperty("dissolutionDateGroups")
	private List<GenericResponse> dissolutionDates;
	
}
