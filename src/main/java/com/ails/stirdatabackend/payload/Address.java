package com.ails.stirdatabackend.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {
	private String adminUnitL1;
	private String adminUnitL2;
	private String fullAddress;
	private String postCode;
	private String postName;
	private String thoroughfare;
	private String locatorName;
	private String locatorDesignator;

	private String country;
	private CodeLabel nuts3;
	private CodeLabel lau;
	
}
