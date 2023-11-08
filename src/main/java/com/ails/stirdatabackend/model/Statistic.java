package com.ails.stirdatabackend.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
//@NoArgsConstructor
@Document(collection = "statistics")
public class Statistic {
	   @Id
	   private String id;
	   
	   @Indexed(name = "country", direction = IndexDirection.ASCENDING)
	   private String country;
	   
	   @Indexed(name = "dimension", direction = IndexDirection.ASCENDING)
	   private Dimension dimension;

	   private int count;

	   @Indexed(name = "place", direction = IndexDirection.ASCENDING)
	   private String place;

	   private String parentPlace;

	   @Indexed(name = "activity", direction = IndexDirection.ASCENDING)
	   private String activity;

	   private String parentActivity;

	   private String fromDate;

	   private String toDate;

	   private String dateInterval;

	   private String parentFromDate;

	   private String parentToDate;

	   private Date updated;
	   
	   @Indexed(name = "referenceDate", direction = IndexDirection.ASCENDING)
	   private Date referenceDate;
	   
	   public Statistic() { }
}
