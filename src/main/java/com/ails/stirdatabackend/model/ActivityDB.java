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

	@Column(name = "label_bg", length = 500)
	private String labelBg;

	@Column(name = "label_cs", length = 500)
	private String labelCs;

	@Column(name = "label_da", length = 500)
	private String labelDa;
	
	@Column(name = "label_de", length = 500)
	private String labelDe;
	
	@Column(name = "label_et", length = 500)
	private String labelEt;
	
	@Column(name = "label_el", length = 500)
	private String labelEl;
	
	@Column(name = "label_en", length = 500)
	private String labelEn;
	
	@Column(name = "label_es", length = 500)
	private String labelEs;
	
	@Column(name = "label_fi", length = 500)
	private String labelFi;
	
	@Column(name = "label_fr", length = 500)
	private String labelFr;
	
	@Column(name = "label_hr", length = 500)
	private String labelHr;
	
	@Column(name = "label_hu", length = 500)
	private String labelHu;
	
	@Column(name = "label_it", length = 500)
	private String labelIt;
	
	@Column(name = "label_lt", length = 500)
	private String labelLt;
	
	@Column(name = "label_lv", length = 500)
	private String labelLv;
	
	@Column(name = "label_mt", length = 500)
	private String labelMt;
	
	@Column(name = "label_nl", length = 500)
	private String labelNl;
	
	@Column(name = "label_no", length = 500)
	private String labelNo;
	
	@Column(name = "label_pl", length = 500)
	private String labelPl;
	
	@Column(name = "label_pt", length = 500)
	private String labelPt;
	
	@Column(name = "label_ro", length = 500)
	private String labelRo;
	
	@Column(name = "label_ru", length = 500)
	private String labelRu;
	
	@Column(name = "label_sl", length = 500)
	private String labelSl;
	
	@Column(name = "label_sk", length = 500)
	private String labelSk;
	
	@Column(name = "label_sv", length = 500)
	private String labelSv;
	
	@Column(name = "label_tr", length = 500)
	private String labelTr;
	
	@Column(name = "scheme")
	private String scheme;

//	@Column(name = "level", nullable = false)
//	private int level;
	
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
		} else if (lang.equals("et")) {
			return labelEt;
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
		} else if (lang.equals("sl")) {
			return labelSl;
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
