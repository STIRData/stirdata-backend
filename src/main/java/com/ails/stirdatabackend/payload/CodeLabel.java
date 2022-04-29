package com.ails.stirdatabackend.payload;


import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeLabel {

	public String code;
	public String label;
	public String uri;
	
	public String geometry;
	
	public CodeLabel(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public CodeLabel(String code, String label, String uri) {
		this.code = code;
		this.label = label;
		this.uri = uri;
	}

}