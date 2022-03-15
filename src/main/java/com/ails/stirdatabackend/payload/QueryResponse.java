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
public class QueryResponse {

	private CodeLabel country;
	
	private List<LegalEntity> legalEntities;
	
	private Page page;
	
}
