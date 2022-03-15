package com.ails.stirdatabackend.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Statistics" 
     ,  indexes = { @Index(columnList = "country"), 
    		       @Index(columnList = "dimension"), 
    		       @Index(columnList = "place"), 
    		       @Index(columnList = "activity"),
    		       @Index(columnList = "parent_place"),
    		       @Index(columnList = "parent_activity") } 
)
public class StatisticDB {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "country", nullable = false)
	private String country;

	@Column(name = "dimension", nullable = false)
	private String dimension;

	@Column(name = "count", nullable = false)
	private int count;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place" 
//                , foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
	           )
//    @Column(name = "place")
    @Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code place;
	private PlaceDB place;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_place"
//	            , foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
	           )
//	@Column(name = "parent_place")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code parentPlace;
	private PlaceDB parentPlace;

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity" 
//                , foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
               )
//  @Column(name = "activity")
    @Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code activity;
	private ActivityDB activity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_activity"
//	            , foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
	           )
//	@Column(name = "parent_activity")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code parentActivity;
	private ActivityDB parentActivity;

	@Column(name = "founding_from_date")
	@Temporal(TemporalType.DATE)
	private Date foundingFromDate;

	@Column(name = "founding_to_date")
	@Temporal(TemporalType.DATE)
	private Date foundingToDate;

	@Column(name = "founding_date_interval")
	private String foundingDateInterval;

	@Column(name = "parent_founding_from_date")
	@Temporal(TemporalType.DATE)
	private Date parentFoundingFromDate;

	@Column(name = "parent_founding_to_date")
	@Temporal(TemporalType.DATE)
	private Date parentFoundingToDate;

	@Column(name = "dissolution_from_date")
	@Temporal(TemporalType.DATE)
	private Date dissolutionFromDate;

	@Column(name = "dissolution_to_date")
	@Temporal(TemporalType.DATE)
	private Date dissolutionToDate;

	@Column(name = "dissolution_date_interval")
	private String dissolutionDateInterval;

	@Column(name = "parent_dissolution_from_date")
	@Temporal(TemporalType.DATE)
	private Date parentDissolutionFromDate;

	@Column(name = "parent_dissolution_to_date")
	@Temporal(TemporalType.DATE)
	private Date parentDissolutionToDate;
	
	@Column(name = "updated")
	private Date updated;

	@Column(name = "reference_date")
	private Date referenceDate;
	
	public StatisticDB(long count) {
		this.count = (int)count;
	}	

	public StatisticDB(long count, ActivityDB activity) {
		this.count = (int)count;
		this.activity = activity;
	}	
	
	public StatisticDB(String dimension, long count,  Date fromDate, Date toDate) {
		if (dimension.equals(Dimension.FOUNDING.toString()) || dimension.equals(Dimension.NUTSLAU_FOUNDING.toString()) || dimension.equals(Dimension.NACE_FOUNDING.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.foundingFromDate = fromDate;
			this.foundingToDate = toDate;
		} else if (dimension.equals(Dimension.DISSOLUTION.toString()) || dimension.equals(Dimension.NUTSLAU_DISSOLUTION.toString()) || dimension.equals(Dimension.NACE_DISSOLUTION.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.dissolutionFromDate = fromDate;
			this.dissolutionToDate = toDate;
		}

	}
	
	public StatisticDB(String dimension, ActivityDB activity, long count,  Date fromDate, Date toDate) {
		if (dimension.equals(Dimension.FOUNDING.toString()) || dimension.equals(Dimension.NUTSLAU_FOUNDING.toString()) || dimension.equals(Dimension.NACE_FOUNDING.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.foundingFromDate = fromDate;
			this.foundingToDate = toDate;
		} else if (dimension.equals(Dimension.DISSOLUTION.toString()) || dimension.equals(Dimension.NUTSLAU_DISSOLUTION.toString()) || dimension.equals(Dimension.NACE_DISSOLUTION.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.dissolutionFromDate = fromDate;
			this.dissolutionToDate = toDate;
		}
		
		this.activity = activity;

	}
	
	public StatisticDB(String dimension, PlaceDB place, long count,  Date fromDate, Date toDate) {
		if (dimension.equals(Dimension.FOUNDING.toString()) || dimension.equals(Dimension.NUTSLAU_FOUNDING.toString()) || dimension.equals(Dimension.NACE_FOUNDING.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.foundingFromDate = fromDate;
			this.foundingToDate = toDate;
		} else if (dimension.equals(Dimension.DISSOLUTION.toString()) || dimension.equals(Dimension.NUTSLAU_DISSOLUTION.toString()) || dimension.equals(Dimension.NACE_DISSOLUTION.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.dissolutionFromDate = fromDate;
			this.dissolutionToDate = toDate;
		}
		
		this.place = place;

	}
	
	public StatisticDB(String dimension, String country, long count,  Date fromDate, Date toDate) {
		if (dimension.equals(Dimension.FOUNDING.toString()) || dimension.equals(Dimension.NUTSLAU_FOUNDING.toString()) || dimension.equals(Dimension.NACE_FOUNDING.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.foundingFromDate = fromDate;
			this.foundingToDate = toDate;
		} else if (dimension.equals(Dimension.DISSOLUTION.toString()) || dimension.equals(Dimension.NUTSLAU_DISSOLUTION.toString()) || dimension.equals(Dimension.NACE_DISSOLUTION.toString())) {
			this.count = (int)count;
	//		this.country = country;
			this.dissolutionFromDate = fromDate;
			this.dissolutionToDate = toDate;
		}
		
		this.country = country;

	}
	

}
