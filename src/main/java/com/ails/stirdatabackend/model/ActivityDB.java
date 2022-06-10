package com.ails.stirdatabackend.model;

import javax.persistence.Column;
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
@Table(name = "Activities", indexes = { @Index(columnList = "parent"), @Index(columnList = "scheme") } )
public class ActivityDB {
	
	@Id
	@Column(name = "code")
	@Type(type = "com.ails.stirdatabackend.model.db.CodeDataType")
	private Code code;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "parent")
	private ActivityDB parent;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "exact_match")
	private ActivityDB exactMatch;	

	@Column(name = "label_bg")
	private String labelBg;

	@Column(name = "label_cs")
	private String labelCs;

	@Column(name = "label_da")
	private String labelDa;
	
	@Column(name = "label_de")
	private String labelDe;
	
	@Column(name = "label_ee")
	private String labelEe;
	
	@Column(name = "label_el", length = 500)
	private String labelEl;
	
	@Column(name = "label_en", length = 500)
	private String labelEn;
	
	@Column(name = "label_es")
	private String labelEs;
	
	@Column(name = "label_fi")
	private String labelFi;
	
	@Column(name = "label_fr")
	private String labelFr;
	
	@Column(name = "label_hr")
	private String labelHr;
	
	@Column(name = "label_hu")
	private String labelHu;
	
	@Column(name = "label_it")
	private String labelIt;
	
	@Column(name = "label_lt")
	private String labelLt;
	
	@Column(name = "label_lv")
	private String labelLv;
	
	@Column(name = "label_mt")
	private String labelMt;
	
	@Column(name = "label_nl")
	private String labelNl;
	
	@Column(name = "label_no")
	private String labelNo;
	
	@Column(name = "label_pl")
	private String labelPl;
	
	@Column(name = "label_pt")
	private String labelPt;
	
	@Column(name = "label_ro")
	private String labelRo;
	
	@Column(name = "label_ru")
	private String labelRu;
	
	@Column(name = "label_si")
	private String labelSi;
	
	@Column(name = "label_sk")
	private String labelSk;
	
	@Column(name = "label_sv")
	private String labelSv;
	
	@Column(name = "label_tr")
	private String labelTr;
	
	@Column(name = "scheme")
	private String scheme;

	@Column(name = "level", nullable = false)
	private int level;
	
	public ActivityDB(Code code) {
		setCode(code);
	}
	
	public String getLabel(String lang) {
		
		if (lang == null) {
			return labelEn;
		}

		lang = lang.toLowerCase();
		
		if (lang.equals("en")) {
			return labelEn;
		} else if (lang.equals("bg")) {
			return labelBg;
		} else if (lang.equals("cs")) {
			return labelCs;
		} else if (lang.equals("ds")) {
			return labelDa;
		} else if (lang.equals("de")) {
			return labelDe;
		} else if (lang.equals("ee")) {
			return labelEe;
		} else if (lang.equals("el")) {
			return labelEl;
		} else if (lang.equals("es")) {
			return labelEs;
		} else if (lang.equals("fi")) {
			return labelFi;
		} else if (lang.equals("fr")) {
			return labelFr;
		} else if (lang.equals("hr")) {
			return labelHr;
		} else if (lang.equals("hu")) {
			return labelHu;
		} else if (lang.equals("it")) {
			return labelIt;
		} else if (lang.equals("lt")) {
			return labelLt;
		} else if (lang.equals("lv")) {
			return labelLv;
		} else if (lang.equals("mt")) {
			return labelMt;
		} else if (lang.equals("nl")) {
			return labelNl;
		} else if (lang.equals("no")) {
			return labelNo;
		} else if (lang.equals("pl")) {
			return labelPl;
		} else if (lang.equals("pt")) {
			return labelPt;
		} else if (lang.equals("ro")) {
			return labelRo;
		} else if (lang.equals("ru")) {
			return labelRu;
		} else if (lang.equals("si")) {
			return labelSi;
		} else if (lang.equals("sk")) {
			return labelSk;
		} else if (lang.equals("sv")) {
			return labelSv;
		} else if (lang.equals("tr")) {
			return labelTr;	
		}
		
		return null;
	}

	public String toString() {
		return code.toString();
	}

}
