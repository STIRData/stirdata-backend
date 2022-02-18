package com.ails.stirdatabackend.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "Statistics")
public class StatisticDB {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	@Column(name = "country", nullable = false)
	private String country;

	@Column(name = "dimension", nullable = false)
	private String dimension;

	@Column(name = "count", nullable = false)
	private int count;

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place")	
//    @Column(name = "place")
    @Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code place;
	private PlaceDB place;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_place")
//	@Column(name = "parent_place")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code parentPlace;
	private PlaceDB parentPlace;

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity")
//  @Column(name = "activity")
    @Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code activity;
	private ActivityDB activity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_activity")
//	@Column(name = "parent_activity")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
//	private Code parentActivity;
	private ActivityDB parentActivity;

	@Column(name = "from_date")
	@Temporal(TemporalType.DATE)
	private Date fromDate;

	@Column(name = "to_date")
	@Temporal(TemporalType.DATE)
	private Date toDate;

	@Column(name = "date_interval")
	private String dateInterval;

	@Column(name = "parent_from_date")
	@Temporal(TemporalType.DATE)
	private Date parentFromDate;

	@Column(name = "parent_to_date")
	@Temporal(TemporalType.DATE)
	private Date parentToDate;

	@Column(name = "updated")
	private Date updated;
	
	public StatisticDB(long count) {
		this.count = (int)count;
	}	

	public StatisticDB(long count, ActivityDB activity) {
		this.count = (int)count;
		this.activity = activity;
	}	
	
	public StatisticDB(long count,  Date fromDate, Date toDate) {
		this.count = (int)count;
//		this.country = country;
		this.fromDate = fromDate;
		this.toDate = toDate;
	}
	

}
