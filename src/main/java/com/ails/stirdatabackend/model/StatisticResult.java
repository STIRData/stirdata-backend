package com.ails.stirdatabackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class StatisticResult {
	private Code code;
	
	private String country;
	
//	private Date fromDate;
//	
//	private Date toDate;
//
//	private String interval;

	private Code parentCode;

//	private Date parentFromDate;
//
//	private Date parentToDate;

	private int count;
	
	public String toString() {
//		return (code != null ?  code :  fromDate + " - " + toDate) + " : " + count + (parentCode != null ? " [ " + parentCode + " ] " :""); 
		return (code + " : " + count + " [ " + parentCode + " ] ");
	}
}