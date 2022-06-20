package com.ails.stirdatabackend.model;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index; 
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Places", indexes = { @Index(columnList = "parent") } )
public class PlaceDB {
	
	@Id
	@Column(name = "code")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
	Code code;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "parent")
//	@Column(name = "parent")
	private PlaceDB parent;

	@Column(name = "national_name")
	private String nationalName;

	@Column(name = "latin_name")
	private String latinName;

	@Column(name = "type", nullable = false)
	private String type;

	@Column(name = "level")
	private int level;
	
	@Column(name = "number_of_children")
	private int numberOfChildren = 0;

	@Column(name = "extra_regio") // true for Z nuts
	private boolean extraRegio;
	
	@Column(name = "country", nullable = false)
	private String country;

	@Column(name = "geometry_1m", columnDefinition="TEXT")
	private String geometry1M;

	@Column(name = "geometry_3m", columnDefinition="TEXT")
	private String geometry3M;

	@Column(name = "geometry_10m", columnDefinition="TEXT")
	private String geometry10M;

	@Column(name = "geometry_20m", columnDefinition="TEXT")
	private String geometry20M;

	@Column(name = "geometry_60m", columnDefinition="TEXT")
	private String geometry60M;
	

	public PlaceDB(Code code) {
		setCode(code);
	}
	
	public boolean isNuts() {
		return type.equals("NUTS");
	}

	public boolean isLau() {
		return type.equals("LAU");
	}
	
	public String getGeometry(String resolution) {
		
		if (resolution == null) {
			return null;
		}

		resolution = resolution.toUpperCase();
		
		if (resolution.equals("1M")) {
			return geometry1M;
		} else if (resolution.equals("3M")) {
			return geometry3M;
		} else if (resolution.equals("10M")) {
			return geometry10M;
		} else if (resolution.equals("20M")) {
			return geometry20M;
		} else if (resolution.equals("60M")) {
			return geometry60M;
		}
		
		return null;
	}
	
	public String toString() {
		return code.toString();
	}
}
