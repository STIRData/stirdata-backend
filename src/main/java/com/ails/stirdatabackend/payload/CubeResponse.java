package com.ails.stirdatabackend.payload;

import java.util.ArrayList;
import java.util.List;

import com.ails.stirdatabackend.model.Statistic;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CubeResponse {

	private CodeLabel dataset;
	
	private CodeLabel property;

	private List<CodeLabel> values;
	

	public CubeResponse() {
//		values = new ArrayList<>();
	}
	
	public void addValue(CodeLabel value) {
		if (values == null) {
			values = new ArrayList<>();
		}
		
		values.add(value);
	}
}
