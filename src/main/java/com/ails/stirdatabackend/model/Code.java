package com.ails.stirdatabackend.model;

import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;

import com.ails.stirdatabackend.payload.Country;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Code implements Serializable {

	public final static String nutsPrefix = "http://data.europa.eu/nuts/code/";
	public final static String lauPrefix = "https://w3id.org/stirdata/resource/lau/item/";
	
	public final static String statDatasetPrefix = "https://w3id.org/stirdata/resource/stat/dataset/";
	public final static String statPropertyPrefix = "https://w3id.org/stirdata/vocabulary/stat/";
	public final static String statValuePrefix = "https://w3id.org/stirdata/resource/stat/item/";
	
//    public final static String lau2021Prefix = "https://lod.stirdata.eu/lau/code/2021/";
    
    public final static String naceRev2Prefix = "http://data.europa.eu/ux2/nace2/";
//	public final static String naceRev2Prefix = "https://w3id.org/stirdata/resource/nace/nace-rev2/item/";
    public final static String naceRev2Namespace = "nace-rev2";

    public final static String nutsNamespace = "nuts";
    public final static String lauNamespace = "lau";
    public final static String dateIntervalNamespace = "date-range";
    
    public final static String statNamespace = "stat";
    public final static String statDatasetNamespace = "stat-ds";
    public final static String statPropertyNamespace = "stat-prop";
    public final static String statValueNamespace = "stat-val";
    
    public final static String date1D = "1D";
    public final static String date1M = "1M";
    public final static String date3M = "3M";
    public final static String date1Y = "1Y";    
    public final static String date10Y = "10Y";
    
	private static final long serialVersionUID = 1L;
	
	private final static Pattern dateIntervalPattern = Pattern.compile("^(.*?):(.*?)(?::(.*?))?$");
	private final static Pattern statPattern = Pattern.compile("^(.*?):(.*?):(.*?)$");
	private final static Pattern statValuePattern = Pattern.compile("^(.*?)/(.*?)(?::(.*?):(.*?))?$");
	
//	public static void main(String[] args) {
////		String s = "MOUNTAIN_NUTS:mountainLevel:MOUNTAIN_2";
//		String s = "TGS00112:tgs00112:accomunit/BEDPL~nace_r2/I551-I553~unit/NR:1000:";
//		
//		Matcher m = statPattern.matcher(s);
//		if (m.find()) {
//			for (int i = 0 ; i < m.groupCount() + 1; i++) {
//				System.out.println("*" + m.group(i) + "*");
//			}
//
//			System.out.println();
//			
//			String[] values = m.group(3).split("~");
//			
//			if (values.length > 1) {
//				for (String val : values) {
//					System.out.println(val);
//					
//					Matcher m2 = statValuePattern.matcher(val);
//					if (m2.find()) {
//						for (int i = 0 ; i < m2.groupCount() + 1; i++) {
//							System.out.println("*" + m2.group(i) + "*");
//						}
//					}
//				}
//			}
//
//		}
//		
//	}
	
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
	
	private String statDataset;
	private String statProperty;
	private String statValue;
	
	private Map<String, String> statOtherValues;
	
	private Integer statYear;
	private Double statMinValue;
	private Double statMaxValue;

	public Code(String string) {
//		System.out.println(">>>>> " + string);
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
				Matcher m = dateIntervalPattern.matcher(code);
				if (m.find()) {
					if (m.groupCount() == 2 || m.groupCount() == 3) {
						try {
							dateFrom = Date.valueOf(m.group(1));
						} catch (Exception e) { }
						try {
							dateTo = Date.valueOf(m.group(2));
						} catch (Exception e) { }
						
						if (m.groupCount() == 3) {
							dateInterval = m.group(3);
						}
					}
				}
			} else	if (namespace.equals(statNamespace)) {
				Matcher m = statPattern.matcher(code);
				if (m.find()) {
					statDataset = m.group(1);
					statProperty = m.group(2);
					statValue = m.group(3);
					
					String[] values = statValue.split("~");
					
					if (values.length > 0) {
						statOtherValues = new HashMap<>();
						for (String val : values) {
							Matcher m2 = statValuePattern.matcher(val);
							if (m2.find()) {
								
								if (m2.group(3) == null && m2.group(4) == null) {
									statOtherValues.put(m2.group(1), m2.group(2));
								} else {
									statValue = m2.group(1) + "/" + m2.group(2);
									try {
										statMinValue = Double.parseDouble(m2.group(3));
									} catch (Exception e) { }
									try {
										statMaxValue = Double.parseDouble(m2.group(4));
									} catch (Exception e) { }
								}
							}
						}
						
//						System.out.println(statDataset);
//						System.out.println(statProperty);
//						System.out.println(statValue);
//						System.out.println(statOtherValues);
//						System.out.println(statValue);
//						System.out.println(statMinValue);
//						System.out.println(statMaxValue);
					}
				}
			}
		} else {
			namespace = string;
		}
	}
	
	public static Code fromStatPropetyUri(String s) {
		if (s.startsWith(statPropertyPrefix)) {
			return createStatPropertyCode(s.substring(statPropertyPrefix.length()));
		} else {
			return null;
		}
	}
	
	public static Code createStatDatasetCode(String s) {
		return new Code(statDatasetNamespace + ":" + s);
	}
	
	public static Code fromStatDatasetUri(String s) {
		if (s.startsWith(statDatasetPrefix)) {
			return createStatDatasetCode(s.substring(statDatasetPrefix.length()));
		} else {
			return null;
		}
	}

	public static Code createStatPropertyCode(String s) {
		return new Code(statPropertyNamespace + ":" + s);
	}
	
	public static Code fromStatValueUri(String s) {
		if (s.startsWith(statValuePrefix)) {
			return createStatValueCode(s.substring(statValuePrefix.length()));
		} else {
			return null;
		}
	}
	
	public static Code createStatValueCode(String s) {
		return new Code(statValueNamespace + ":" + s);
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

	public static Code fromLauUri(String s, CountryDB cc) {
		if (s.startsWith(cc.getLauPrefix())) {
			return createLauCode(s.substring(cc.getLauPrefix().length()));
		} else {
			return null;
		}
	}
	
	public static Code fromCompanyTypeUri(String s, CountryDB cc) {
		if (s.startsWith(cc.getCompanyTypePrefix())) {
			return new Code(cc.getCompanyTypeNamespace() + ":" + s.substring(cc.getCompanyTypePrefix().length()));
		} else {
			return null;
		}
	}
	
	// 10Y 1Y 1M 1D
	public static String nextDateLevel(String level) {
		if (level.equals(date10Y)) {
			return date1Y;
		} else if (level.equals(date1Y)) {
//			return date3M;
//		} else if (level.equals(date3M)) {
			return date1M;
		} else if (level.equals(date1M)) {
			return date1D;
		}
		
		return null;
	}
	
	public static String previousDateLevel(String level) {
		if (level.equals(date1Y)) {
			return date10Y;
//		} else if (level.equals(date3M)) {
//			return date1Y;
//		} else if (level.equals(date1M)) {
//			return date3M;
		} else if (level.equals(date1M)) {
			return date1Y;
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
	
	public static Code fromNaceUri(String s, CountryDB cc) {
		if (s.startsWith(cc.getNacePrefix())) {
			return new Code(cc.getNaceNamespace() + ":" + s.substring(cc.getNacePrefix().length()));
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
	
	public boolean isStat() {
		return namespace != null && namespace.equals(statNamespace);
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
    	} else if (len == 3) {
    	    return 3;
    	} else if (len == 4) {
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
	
	public String localNaceToUri(CountryDB cc) {
		return cc.getNacePrefix() + code;
	}
	
//	public String toUx2NaceUri() {
//		return namespaceMap.get(namespace) + code.replaceAll("\\.", "");
//	}
	
//	public String toUri(String appendix) {
//		return namespaceMap.get(namespace) + appendix + "/" + code;
//	}

	public String getStatDatasetUri() {
		return statDatasetPrefix + this.statDataset;
	}

	public String getStatPropertyUri() {
		return statPropertyPrefix + this.statProperty;
	}

	public String getStatValueUri() {
		return statValuePrefix + this.statValue;
	}
	
	public Integer getStatYear() {
		return statYear;
	}
	
	public Double getStatMinValue() {
		return statMinValue;
	}

	public Double getStatMaxValue() {
		return statMaxValue;
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
	
	public static boolean isNutsUri(String uri) {
		return uri.startsWith(nutsPrefix);
	}
	
	public boolean isEuroStatValue() {
		return statValue.startsWith("unit/");
	}
}
