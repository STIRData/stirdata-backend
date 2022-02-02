package com.ails.stirdatabackend.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "statistics")
public class Statistic {
	   @Id
	   private String id;
	   
	   private String country;
	   
	   private Dimension dimension;

	   private int count;

	   private String place;

	   private String parentPlace;

	   private String activity;

	   private String parentActivity;

	   private String fromDate;

	   private String toDate;

	   private String dateInterval;

	   private String parentFromDate;

	   private String parentToDate;

	   private Date updated;
	   
}
