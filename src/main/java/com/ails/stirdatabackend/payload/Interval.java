package com.ails.stirdatabackend.payload;

import java.sql.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Interval {

	public Date from;
	public Date to;
	
	public Interval(Date from, Date to) {
		this.from = from;
		this.to = to;
	}
	
	public Interval(java.util.Date from, java.util.Date to) {
		if (from != null) {
			this.from = Date.valueOf(from.toString());
		}
		if (to != null) {
			this.to = Date.valueOf(to.toString());
		}
	}
	
	public Interval(String from, String to) {
		if (from != null) {
			this.from = Date.valueOf(from);
		}
		
		if (to != null) {
			this.to = Date.valueOf(to);
		}
	}
	
}
