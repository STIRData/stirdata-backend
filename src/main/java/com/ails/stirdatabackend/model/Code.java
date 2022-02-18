package com.ails.stirdatabackend.model;

import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Code implements Serializable {

    public final static String nutsPrefix = "https://lod.stirdata.eu/nuts/code/";
    public final static String lauPrefix = "https://lod.stirdata.eu/lau/code/";
//    public final static String lau2021Prefix = "https://lod.stirdata.eu/lau/code/2021/";
    
    public final static String naceRev2Prefix = "https://lod.stirdata.eu/nace/nace-rev2/code/";
    public final static String naceRev2Namespace = "nace-rev2";

    public final static String nutsNamespace = "nuts";
    public final static String lauNamespace = "lau";
    public final static String dateIntervalNamespace = "date-range";
    
    public final static String date1D = "1D";
    public final static String date1M = "1M";
    public final static String date3M = "3M";
    public final static String date1Y = "1Y";    
    public final static String date10Y = "10Y";
    
	private static final long serialVersionUID = 1L;
	
	private final static Pattern datePattern = Pattern.compile("^(.*?):(.*?)(?::(.*?))?$");
	
	private static Map<String, String> namespaceMap = new HashMap<>();
	static {
		namespaceMap.put(nutsNamespace, nutsPrefix);	
		namespaceMap.put(lauNamespace, lauPrefix);
		namespaceMap.put(naceRev2Namespace, naceRev2Prefix);
	}
	
	private String namespace;
	private String code;
	
	private Date dateFrom;
	private Date dateTo;
	private String dateInterval;

	public Code(String string) {
//		String[] r = string.split(":");
//		this.namespace = r[0];
//		if (r.length > 1) {
//			this.code = r[1];
//		}
		
		int p = string.indexOf(":");
		if (p > 0) {
			namespace = string.substring(0, p);
			code = string.substring(p + 1);
		
			if (namespace.equals(dateIntervalNamespace)) {
				Matcher m = datePattern.matcher(code);
				if (m.find()) {
					if (m.groupCount() == 2 || m.groupCount() == 3) {
						try {
							dateFrom = Date.valueOf(m.group(1));
						} catch (IllegalArgumentException e) { }
						try {
							dateTo = Date.valueOf(m.group(2));
						} catch (IllegalArgumentException e) { }
						
						if (m.groupCount() == 3) {
							dateInterval = m.group(3);
						}
					}
				}
			}
		} else {
			namespace = string;
		}
	}
	
	public static Code createNutsCode(String s) {
		return new Code(nutsNamespace + ":" + s);
	}
	
	public static Code fromNutsUri(String s) {
		if (s.startsWith(nutsPrefix)) {
			return createNutsCode(s.substring(nutsPrefix.length()));
		} else {
			return null;
		}
	}
	
	public static String nextDateLevel(String level) {
		if (level.equals(date10Y)) {
			return date1Y;
		} else if (level.equals(date1Y)) {
			return date3M;
		} else if (level.equals(date3M)) {
			return date1M;
		} else if (level.equals(date1M)) {
			return date1D;
		}
		
		return null;
	}
	
	public static String previousDateLevel(String level) {
		if (level.equals(date1Y)) {
			return date10Y;
		} else if (level.equals(date3M)) {
			return date1Y;
		} else if (level.equals(date1M)) {
			return date3M;
		} else if (level.equals(date1D)) {
			return date1M;
		}
		
		return null;
	}	

	public static Code createDateCode(String from, String to, String interval) {
		return new Code(dateIntervalNamespace + ":" + from + ":" + to + ":" + interval);
	}

	public static Code createDateCode(Date from, Date to) {
		return new Code(dateIntervalNamespace + ":" + from + ":" + to);
	}

	public static Code createDateCode(Date from, Date to, String interval) {
		return new Code(dateIntervalNamespace + ":" + from + ":" + to + ":" + interval);
	}
	
	public static Code createLauCode(String s) {
		return new Code(lauNamespace + ":" + s);
	}
	
	public static Code fromLauUri(String s, String year) {
		if (s.startsWith(lauPrefix + year + "/")) {
			return createLauCode(s.substring((lauPrefix + year + "/").length()));
		} else {
			return null;
		}
	}
	
	public static Code createNaceRev2Code(String s) {
		return new Code(naceRev2Namespace + ":" + s);
	}
	
	public static Code fromNaceRev2Uri(String s) {
		if (s.startsWith(naceRev2Prefix)) {
			return createNaceRev2Code(s.substring(naceRev2Prefix.length()));
		} else {
			return null;
		}
	}

	public Code normalizeDate(Date defaultFromDate) {
		if (namespace.equals(dateIntervalNamespace)) {

			java.util.Date fromMonth;
			
			Date from = this.getDateFrom();
			if (from != null) {
				fromMonth = DateUtils.truncate(from, Calendar.MONTH);
			} else {
				fromMonth = DateUtils.truncate(defaultFromDate, Calendar.MONTH);
			}
			
			java.util.Date toMonth;
			
			Date to = this.getDateTo();
			if (to != null) {
				toMonth = DateUtils.truncate(to, Calendar.MONTH);
			} else {
				toMonth = DateUtils.truncate(new java.util.Date(), Calendar.MONTH);
			}

			toMonth = DateUtils.addMonths(toMonth, 1);
			toMonth = DateUtils.addDays(toMonth, -1);

			return createDateCode(new Date(fromMonth.getTime()), new Date(toMonth.getTime()), this.dateInterval);
		} else {
			return this;
		}
		
	}
	
//	public Date getFromDate() {
//		return dateFrom;
//	}
//
//	public Date getToDate() {
//		return dateTo;
//	}
//
//	public String getDateInterval() {
//		return dateInterval;
//	}

	public boolean isDateInterval1D() {
		return dateInterval != null && dateInterval.equals("1D");
	}
	
	public boolean isDate() {
		return namespace != null && namespace.equals(dateIntervalNamespace);
	}

	public boolean isNuts() {
		return namespace != null && namespace.equals(nutsNamespace);
	}
	
	public boolean isNutsZ() {
		return isNuts() && (code.length() == 3 && code.endsWith("Z") ||
				            code.length() == 4 && code.endsWith("ZZ") ||
				            code.length() == 5 && code.endsWith("ZZZ"));
	}

	public boolean isNutsCountry() {
		return getNutsLevel() == 0;
	}
	
	public String getNutsCountry() {
		if (isNuts() || isLau()) {
			return code.substring(0, 2);
		} else {
			return null;
		}
	}

    public int getNutsLevel() {
    	if (isNuts()) {
    		return code.length() - 2;
    	} else {
    		return -1;
    	}
    }
    
	public String getLauCountry() {
		if (isLau()) {
			return code.substring(0, 2);
		} else {
			return null;
		}
	}
	
	public boolean isLau() {
		return namespace != null && namespace.equals(lauNamespace);
	}
	
	public boolean isNaceRev2() {
		return namespace != null && namespace.equals(naceRev2Namespace);
	}

	public boolean isNaceRev2Leaf() {
		return isNaceRev2() && getNaceRev2Level() == 4;
	}
	
    public int getNaceRev2Level() {
    	if (!isNaceRev2()) {
    		return -1;
    	}
    	
    	int len = code.length();
    	
    	if (len == 1 && Character.isLetter(code.charAt(0))) {
    		return 1;
    	} else if (len == 2) {
    		return 2;
    	} else if (len == 4 && code.charAt(2) == '.') {
    	    return 3;
    	} else if (len == 5 && code.charAt(2) == '.') {
    		return 4;
    	}
    	
    	return -1;
    }
	
	public boolean isStirdata() {
		return namespace != null && namespace.equals("stirdata");
	}

	public String toString() {
		return namespace + ":" + code;
	}
	
	public String toUri() {
		return namespaceMap.get(namespace) + code;
	}
	
	public String toUri(String appendix) {
		return namespaceMap.get(namespace) + appendix + "/" + code;
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Code)) {
			return false;
		} else {
			return this.toString().equals(((Code)obj).toString());
		}
	}
}
