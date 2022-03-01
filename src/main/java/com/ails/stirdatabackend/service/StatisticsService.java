package com.ails.stirdatabackend.service;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticResult;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.service.NutsService.PlaceSelection;

@Service
public class StatisticsService {
	
	private final static Logger logger = LoggerFactory.getLogger(StatisticsService.class);
	
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

//    @Autowired
//    private StatisticsService statisticsService;

    @Autowired
    private StatisticsRepository statisticsRepository;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

    public void computeAndSaveAllStatistics(CountryConfiguration cc, Set<Dimension> dimensions) {
    	
    	logger.info("Statistics to compute " + dimensions);
    	
    	StatisticsCache sc = new StatisticsCache(cc);
    	
    	if (dimensions.contains(Dimension.DATA)) {
	    	logger.info("Computing DATA statistics for " + cc.getCountryCode());
	    	
	    	String query = SparqlQuery.buildCoreQuery(cc, true, false, null, null, null, null, null).countSelectQuery();
	    	statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.DATA);
	    	
	    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	        	ResultSet rs = qe.execSelect();
	        	
	        	while (rs.hasNext()) {
	        		QuerySolution sol = rs.next();
	
	        		int count = sol.get("count").asLiteral().getInt();
	        		if (count > 0) {
	        			Statistic stat = new Statistic();
	        			stat.setCountry(cc.getCountryCode());
	        			stat.setDimension(Dimension.DATA);
//	        			stat.setUpdated(cc.getLastUpdated());
	        			stat.setUpdated(new java.util.Date());
	        			stat.setCount(count);
	        			
	//        			System.out.println(stat.getCountry() + " " + stat.getCount());
	        			statisticsRepository.save(stat);
	        		}
	        	}
	        }
    	}
    	
    	if (cc.isNuts() && dimensions.contains(Dimension.NUTSLAU)) {
    		logger.info("Computing NUTS statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nuts = statistics(cc, Dimension.NUTSLAU, null, null, null, null, null, true, sc);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.NUTSLAU);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCountryCode());
    			stat.setDimension(Dimension.NUTSLAU);
    			stat.setPlace(sr.getCode().toString());
    			if (sr.getParentCode() != null) {
    				stat.setParentPlace(sr.getParentCode().toString());
    			}
//	    			stat.setUpdated(cc.getLastUpdated());
    			stat.setUpdated(sr.getComputed());
    			stat.setCount(sr.getCount());
    			
//    			System.out.println(stat.getPlace() + " " + stat.getCount());
    			statisticsRepository.save(stat);
    		}
    	}
    	
    	if (cc.isNuts() && cc.isNace() && dimensions.contains(Dimension.NUTSLAU_NACE)) {

    		List<StatisticResult> nuts = new ArrayList<>(); 
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCountryCode(), Dimension.NUTSLAU)) {
				StatisticResult sr = new StatisticResult();
				sr.setCode(new Code(stat.getPlace()));
				sr.setCountry(stat.getCountry());
				sr.setCount(stat.getCount());
				sr.setComputed(stat.getUpdated());
				if (stat.getParentPlace() != null) {
					sr.setParentCode(new Code(stat.getParentPlace()));
				}
				nuts.add(sr);
			}
			
			logger.info("Computing NUTS - NACE statistics for " + cc.getCountryCode());
			
			for (StatisticResult iter : nuts) {
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
				
	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, nutsLauList, null, null, null, true, sc);
//				statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.NUTSLAU_NACE);
	    		
	      		for (StatisticResult sr : nutsNace) {
	    			Statistic stat = new Statistic();
	    			stat.setCountry(cc.getCountryCode());
	    			stat.setDimension(Dimension.NUTSLAU_NACE);
	    			stat.setPlace(iter.getCode().toString());
	    			if (iter.getParentCode() != null) {
	    				stat.setParentPlace(iter.getParentCode().toString());
	    			}
	    			stat.setActivity(sr.getCode().toString());
	    			if (sr.getParentCode() != null) {
	    				stat.setParentActivity(sr.getParentCode().toString());
	    			}
//    	    			stat.setUpdated(cc.getLastUpdated());
	    			stat.setUpdated(sr.getComputed());
	    			stat.setCount(sr.getCount());
	    			
//    	    			System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount());
	    			statisticsRepository.save(stat);
	    		}
			}
   		}
    	
    	if (cc.isNuts() && cc.isFoundingDate() && dimensions.contains(Dimension.NUTSLAU_FOUNDING)) {

    		List<StatisticResult> nuts = new ArrayList<>(); 
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCountryCode(), Dimension.NUTSLAU)) {
				StatisticResult sr = new StatisticResult();
				sr.setCode(new Code(stat.getPlace()));
				sr.setCountry(stat.getCountry());
				sr.setCount(stat.getCount());
				sr.setComputed(stat.getUpdated());
				if (stat.getParentPlace() != null) {
					sr.setParentCode(new Code(stat.getParentPlace()));
				}
				nuts.add(sr);
			}    	
    		
    		logger.info("Computing NUTS - founding date statistics for " + cc.getCountryCode());
    		
			for (StatisticResult iter : nuts) {
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
    		
    			nuts = dateStatistics(cc, Dimension.FOUNDING, null, nutsLauList, null, null, null, true);
	        	statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.NUTSLAU_FOUNDING);
        		
        		for (StatisticResult sr : nuts) {
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCountryCode());
        			stat.setDimension(Dimension.NUTSLAU_FOUNDING);
        			stat.setPlace(iter.getCode().toString());
	    			if (iter.getParentCode() != null) {
	    				stat.setParentPlace(iter.getParentCode().toString());
	    			}	        			
        			stat.setFromDate(sr.getCode().getDateFrom().toString());
        			stat.setToDate(sr.getCode().getDateTo().toString());
        			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
        			if (sr.getParentCode() != null) {
        				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
        				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
        			}
//	        			stat.setUpdated(cc.getLastUpdated());
        			stat.setUpdated(sr.getComputed());
        			stat.setCount(sr.getCount());
        			
//	        			statisticsRepository.save(stat);
        		}
			}
		}
//		
		if (cc.isNace() && dimensions.contains(Dimension.NACE)) {
    		
    		logger.info("Computing NACE statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nace = statistics(cc, Dimension.NACE, null, null, null, null, null, true, sc);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.NACE);
    		
			for (StatisticResult sr : nace) {
				Statistic stat = new Statistic();
				stat.setCountry(cc.getCountryCode());
				stat.setDimension(Dimension.NACE);
				stat.setActivity(sr.getCode().toString());
				if (sr.getParentCode() != null) {
					stat.setParentActivity(sr.getParentCode().toString());
				}
//				stat.setUpdated(cc.getLastUpdated());
				stat.setUpdated(sr.getComputed());
				stat.setCount(sr.getCount());
				
//				System.out.println(stat.getActivity() + " " + stat.getCount());
				statisticsRepository.save(stat);
			}
			
		}
//		
		if (cc.isFoundingDate() && dimensions.contains(Dimension.FOUNDING)) {
    		logger.info("Computing founding date statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.FOUNDING, null, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.FOUNDING);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCountryCode());
    			stat.setDimension(Dimension.FOUNDING);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
    			}
//    			stat.setUpdated(cc.getLastUpdated());
    			stat.setUpdated(sr.getComputed());
    			stat.setCount(sr.getCount());
    			
    			System.out.println(stat.getFromDate() + " " + stat.getToDate() + " " + stat.getDateInterval());
    			
    			statisticsRepository.save(stat);
    		}
		}
		
		if (cc.isDissolutionDate() && dimensions.contains(Dimension.DISSOLUTION)) {
    		logger.info("Computing dissolution date statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.DISSOLUTION, null, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.DISSOLUTION);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCountryCode());
    			stat.setDimension(Dimension.DISSOLUTION);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
    			}
//    			stat.setUpdated(cc.getLastUpdated());
    			stat.setUpdated(sr.getComputed());
    			stat.setCount(sr.getCount());
    			
    			statisticsRepository.save(stat);
    		}
		}
		
		logger.info("Computing statistics for " + cc.getCountryCode() + " completed");
    }

   	private boolean hasMore(Dimension dimension, Code code) {
        if (dimension == Dimension.NUTSLAU && code != null && code.isLau()) {
    		return false;
    	} else if (dimension == Dimension.NACE && code != null && code.isNaceRev2Leaf()) {
    		return false;
    	}
   		
        return true;
   	}
   	
   	private class StatisticsCache extends HashMap<Code, List<String>> {

		private static final long serialVersionUID = 1L;
		
		private CountryConfiguration cc;
   		
   		public StatisticsCache(CountryConfiguration cc) {
   			super();
   			this.cc = cc;
   		}
   		
   		public List<String> nacelookup(Code naceCode) {

   			List<String> leaves = get(naceCode);
   			if (leaves == null) {
   				leaves = naceService.getLocalNaceLeafUris(cc,  naceCode);
   				put(naceCode, leaves);
   			}
   			
   			return leaves;
   		}
   	}
   	
   	private class StatisticsHelper {
   		public List<String> naceLeafUris;
	    public List<String> nutsLeafUris;
	    public List<String> lauUris;
	    
	    Calendar minDate;
	    Calendar maxDate;
	    
   		public StatisticsHelper(CountryConfiguration cc, Dimension dimension, List<Code> nutsLauCodes, List<Code> naceCodes) {
   	    	Map<CountryConfiguration, PlaceSelection> countryPlaceMap;
   	    	if (nutsLauCodes != null) {
   	    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
   	    	} else {
   	    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
   	    	}
   	    	
   	    	PlaceSelection places = countryPlaceMap.get(cc);
   	    	
//   	    if (dimension == Dimension.NUTSLAU) {
   	        	naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCodes);
//   	    } else if (dimension == Dimension.NACE) {
   		        nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, places.getNuts3()); 
   		        lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places.getLau());
// 	        } 

   		}


   		public void minMaxDates(CountryConfiguration cc, Dimension dimension, Code foundingDate, Code dissolutionDate) {
   		    SparqlQuery sparql;
	    	
   	    	Calendar[] minMaxDate = null; 
   			if (dimension == Dimension.FOUNDING) {
   				sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsLeafUris, lauUris, naceLeafUris, null, dissolutionDate);
   				minMaxDate = sparql.minMaxFoundingDate(cc); 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				sparql = SparqlQuery.buildCoreQuery(cc, false, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, null);
   				minMaxDate = sparql.minMaxDissolutionDate(cc);
   			}   			
   			
   			minDate = minMaxDate[0];
   			maxDate = minMaxDate[1];
   		}
   	}


   	public List<StatisticResult> statistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		return statistics(cc, dimension, root, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, allLevels, null);
   	}
   	
   	public List<StatisticResult> statistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels, StatisticsCache sc) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
   		
   		if (!hasMore(dimension, root)) {
	   		return res; 
   		}
   		
   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		
		statistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res, sc);
		
   		if (allLevels) {
    		for (int i = 0; i < res.size(); i++) {
    			statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
    		}
		}
		   		
   		return res;
   	}
   	
   	
//   	public List<StatisticResult> dateStatistics(CountryConfiguration cc, Dimension dimension, Code date, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
//   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
//   		
//   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
//   		
//		dateStatistics(cc, dimension, date, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, res);
//    		
//   		if (allLevels) {
//    		for (int i = 0; i < res.size(); i++) {
//    			dateStatistics(cc, dimension, res.get(i).getCode(), nutsLauCodes, naceCodes, foundingDate, dissolutionDate, res);
//    		}
//		}
//   		
//   		return res;
//   	}
   	
   	public List<StatisticResult> dateStatistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
   		
   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		sh.minMaxDates(cc, dimension, foundingDate, dissolutionDate);
   		
   		if (root == null) {
   			root = Code.createDateCode(new Date(sh.minDate.getTime().getTime()), new Date(sh.maxDate.getTime().getTime()), Code.date10Y);
   		}
   		
		dateStatistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res);
    		
   		if (allLevels) {
    		for (int i = 0; i < res.size(); i++) {
    			dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
    		}
		}
   		
   		return res;
   	}
   	
   	public void statistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {

   		if (!hasMore(dimension, root)) {
   			return;
   		}
   		
	   	statistics(cc, dimension, root, new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes), foundingDate, dissolutionDate, res, sc);
   	}
   	
   	private static int c = 0;
    private void statistics(CountryConfiguration cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {
//    	System.out.println(">>> " + root);
	    List<Code> iterCodes = null;
	    
        if (dimension == Dimension.NUTSLAU) {
        	List<PlaceDB> place = nutsService.getNextNutsLauLevelListDb(root == null ? Code.createNutsCode(cc.getCountryCode()) : root);
        	iterCodes = place.stream().map(item -> item.getCode()).collect(Collectors.toList());;
//        } else if (dimension == Dimension.LAU) {
//        	List<PlaceDB> place = nutsService.getNextNutsLauLevelListDb(root);
//        	iterCodes = place.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        } else if (dimension == Dimension.NACE) {
        	List<ActivityDB> activities = naceService.getNextNaceLevelListDb(root);
        	iterCodes = activities.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        } 
	        
    	for (Code code : iterCodes) {
//    		System.out.println("CODE " + code + " " + code.isNuts() + " " + code.isLau());
    		SparqlQuery sparql = null; 
        			
//	        if (dimension == Dimension.NUTS) {
    		if (dimension == Dimension.NUTSLAU) {
	        	if (code.isNuts()) {
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsService.getLocalNutsLeafUrisDB(cc, code), null, sh.naceLeafUris, foundingDate, dissolutionDate);
	        	} else if (code.isLau()) {
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, null, Arrays.asList(nutsService.getLocalLauUri(cc, code)), sh.naceLeafUris, foundingDate, dissolutionDate);
	        	}
	        } else if (dimension == Dimension.NACE) {
	        	List<String> naceLeafUris = (sc == null) ? naceService.getLocalNaceLeafUris(cc, code) : sc.nacelookup(code);
	        	
            	if (naceLeafUris.isEmpty()) {
            		continue;
            	}
            	
		        sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, naceLeafUris, foundingDate, dissolutionDate); 
	        }
	  
	        String query = sparql.countSelectQuery() ;
	
//	        System.out.println(uri);
//	        System.out.println(cc.getDataEndpoint());
	        System.out.println(query);
	        long start = System.currentTimeMillis();
	        
	        int tries = 0;
	        while (tries < 3) {
	        	tries++;
	        	
		        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	            	ResultSet rs = qe.execSelect();
	            	
	            	while (rs.hasNext()) {
	            		QuerySolution sol = rs.next();
	
	//            		System.out.println(sol);
	            		int count = sol.get("count").asLiteral().getInt();
	            		
	            		System.out.println(c++ +" "+ code.getCode() + " " + count + " " +  (System.currentTimeMillis() - start));
	
	            		if (count > 0) {
	            			StatisticResult sr = new StatisticResult();
	            			sr.setCountry(cc.getCountryCode());
	            			sr.setCode(code);
	            			sr.setParentCode(root);
	            			sr.setCount(count);
	            			sr.setComputed(new java.util.Date());
	            			res.add(sr);
	            		}
	            	}
		        } catch (QueryExceptionHTTP ex) {
		        	System.out.println(ex.getMessage());
		        	System.out.println(ex.getResponse());
		        	System.out.println(ex.getResponseCode());
		        	System.out.println(ex.getResponseMessage());
		        	continue;
		        }	        
		        
	        	break;
	        }
        }        	        

    }    

    
    public StatisticResult singleStatistic(CountryConfiguration cc, Code nutsLauCode, Code naceCode, Code foundingDate, Code dissolutionDate) {
        StatisticResult res = null;
        			
		List<String> nutsUris = (nutsLauCode != null && nutsLauCode.isNuts()) ? nutsUris = nutsService.getLocalNutsLeafUrisDB(cc, nutsLauCode) : null ;
		List<String> lauUris = (nutsLauCode != null && nutsLauCode.isLau()) ? Arrays.asList(nutsService.getLocalLauUri(cc, nutsLauCode)) : null ;    		
    	List<String> naceUris = naceCode != null ? naceService.getLocalNaceLeafUris(cc, naceCode) : null ;
    	
        String query = SparqlQuery.buildCoreQuery(cc, true, false, nutsUris, lauUris, naceUris, foundingDate, dissolutionDate).countSelectQuery() ;
//	    System.out.println(query);
        
        int tries = 0;
        while (tries < 3) {
        	tries++;
        	
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	        	ResultSet rs = qe.execSelect();
	        	
	        	while (rs.hasNext()) {
	        		QuerySolution sol = rs.next();
	
	        		int count = sol.get("count").asLiteral().getInt();
	        		if (count > 0) {
	        			StatisticResult sr = new StatisticResult();
	        			sr.setCountry(cc.getCountryCode());
	        			sr.setCount(count);
	        			sr.setComputed(new java.util.Date());
	        			res = sr;
	        		}
	        	}
	        } catch (QueryExceptionHTTP ex) {
	        	System.out.println(ex.getMessage());
	        	System.out.println(ex.getResponse());
	        	System.out.println(ex.getResponseCode());
	        	System.out.println(ex.getResponseMessage());
	        	
	        	continue;
	        }
	        
	        break;
        }
    
    	return res;

    }
    
//    public static void main(String[] args) {
//    	Date date = Date.valueOf("2020-07-31");
//    	
//    	Calendar cal = Calendar.getInstance();
//    	cal.setTime(new Date(date.getTime()));
//
//    	System.out.println(cal.getTime());
//
//    	System.out.println();
//    	System.out.println(round(cal, Code.date1M, false).getTime());
//    	System.out.println(round(cal, Code.date1M, true).getTime());
//    	System.out.println();
//    	System.out.println(round(cal, Code.date3M, false).getTime());
//    	System.out.println(round(cal, Code.date3M, true).getTime());
//    	System.out.println();
//    	System.out.println(round(cal, Code.date1Y, false).getTime());
//    	System.out.println(round(cal, Code.date1Y, true).getTime());
//    	System.out.println();
//    	System.out.println(round(cal, Code.date10Y, false).getTime());
//    	System.out.println(round(cal, Code.date10Y, true).getTime());
//    }
    
    private void dateStatistics(CountryConfiguration cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate,  List<StatisticResult> res) {
//    	System.out.println(">> " + root);
    	
    	if (root.isDateInterval1D()) {
    		return;
    	}
    	
		Calendar minDate = sh.minDate;
		Calendar maxDate = sh.maxDate;

		if (root.getDateFrom() != null) {
			Calendar minRootDate = Calendar.getInstance();
			minRootDate.setTime(root.getDateFrom());
			
			if (minRootDate.after(minDate)) {
				minDate = minRootDate; 
			}
		}
		
		if (root.getDateTo() != null) {
			Calendar maxRootDate = Calendar.getInstance();
			maxRootDate.setTime(root.getDateTo());
			
			if (maxRootDate.before(maxDate)) {
				maxDate = maxRootDate; 
			}
		}
		
		String interval = root.getDateInterval();
		String nextInterval = Code.nextDateLevel(interval);
		
//		System.out.println("\t\t\t\t\t" + root);
//		System.out.println("\t\t\t\t\t" + dateFormat.format(minDate.getTime()) + " >>> " + dateFormat.format(maxDate.getTime()) );
		
		minDate = round(minDate, interval, false);
		maxDate = round(maxDate, interval, true);

//		Code parentCode = Code.createDateCode(new Date(minDate.getTime().getTime()), new Date(maxDate.getTime().getTime()), interval);
   		
		System.out.println(dateFormat.format(minDate.getTime()) + " >>> " + dateFormat.format(maxDate.getTime()) );
   		
		Calendar fromDate = (Calendar)minDate.clone();

   		while (fromDate.before(maxDate)) {

   			Calendar toDate = (Calendar)fromDate.clone();
   			if (interval.equals("1M")) {
   				toDate.add(Calendar.MONTH, 1);
   				toDate.add(Calendar.DAY_OF_MONTH, -1);
   			} else if (interval.equals("3M")) {
   	   			toDate.add(Calendar.MONTH, 3);
   	   			toDate.add(Calendar.DAY_OF_MONTH, -1);
   			} else if (interval.equals("1Y")) {
   	   			toDate.add(Calendar.YEAR, 1);
   	   			toDate.add(Calendar.DAY_OF_MONTH, -1);
   			} else if (interval.equals("10Y")) {
   	   			toDate.add(Calendar.YEAR, 10);
   	   			toDate.add(Calendar.DAY_OF_MONTH, -1);
   			}
   			
   			SparqlQuery sparql = null;
   			if (dimension == Dimension.FOUNDING) {
   				sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, Code.createDateCode(new Date(fromDate.getTime().getTime()), new Date(toDate.getTime().getTime())), dissolutionDate); 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				sparql = SparqlQuery.buildCoreQuery(cc, false, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, foundingDate, Code.createDateCode(new Date(fromDate.getTime().getTime()), new Date(toDate.getTime().getTime())));
   			}
   			
	        String query = sparql.countSelectQuery() ;
	
//	        System.out.println(dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime()) + " " + resInterval);
//	        System.out.println(query);
	
	        int tries = 0;
	        while (tries < 3) {
	        	tries++;
		        
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	            	ResultSet rs = qe.execSelect();
	            	
	            	while (rs.hasNext()) {
	            		QuerySolution sol = rs.next();
	
	            		int count = sol.get("count").asLiteral().getInt();
	            		
	            		if (count > 0) {
	            			StatisticResult sr = new StatisticResult();
	            			sr.setCountry(cc.getCountryCode());
	            			sr.setCode(Code.createDateCode(new Date(fromDate.getTime().getTime()).toString(), new Date(toDate.getTime().getTime()).toString(), nextInterval));
	            			sr.setCount(count);
	            			if (!root.getDateInterval().equals(Code.date10Y)) {
	            				sr.setParentCode(root);
	            			}
	            			sr.setComputed(new java.util.Date());
	            			
//	            			System.out.println("\t\t" + Code.createDateCode(new Date(fromDate.getTime().getTime()).toString(), new Date(toDate.getTime().getTime()).toString(), nextInterval));
	            			res.add(sr);
	            		}
	            	}
		        } catch (QueryExceptionHTTP ex) {
		        	System.out.println(ex.getMessage());
		        	System.out.println(ex.getResponse());
		        	System.out.println(ex.getResponseCode());
		        	System.out.println(ex.getResponseMessage());
		        	
		        	continue;
		        }
	            
	            break;
	        }
            
            fromDate = toDate;
            fromDate.add(Calendar.DAY_OF_MONTH, 1);
   		}

    }        
    
//    public void dateStatistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCode, List<Code> naceCode, Code foundingDate, Code dissolutionDate,  List<StatisticResult> res) {
//    	System.out.println(">> " + root);
//    	
//    	if (root.isDateInterval1M()) {
//    		return;
//    	}
//    	
//    	Map<CountryConfiguration, PlaceSelection> requestMap;
//    	if (nutsLauCode != null) {
//    		requestMap = nutsService.getEndpointsByNuts(nutsLauCode);
//    	} else {
//    		requestMap = nutsService.getEndpointsByNuts(); 
//    	}
//    	
//    	PlaceSelection regions = requestMap.get(cc);
//
//    	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCode);
//    	List<String> nutsLeafUris = regions == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, regions.getNuts3());
//    	List<String> lauUris = regions == null ? null : nutsService.getLocalLauUris(cc, regions.getLau());
//    	
//    	SparqlQuery sparql = null; 
//		String query = null;
//		
//		if (dimension == Dimension.FOUNDING) {
//			sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, null, dissolutionDate);
//			query = sparql.minFoundingDateQuery(cc); 
//		} else if (dimension == Dimension.DISSOLUTION) {
//			sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, null);
//			query = sparql.minDissolutionDateQuery(cc);
//		}
//
//   		Calendar minDate = null;
//   		if (root.getDateFrom() == null) {
//	   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
//		    	ResultSet rs = qe.execSelect();
//		    	
//		    	while (rs.hasNext()) {
//		    		QuerySolution sol = rs.next();
//		
//		    		minDate = ((XSDDateTime)sol.get("date").asLiteral().getValue()).asCalendar();
//		    	}
//		    }
//   		} else {
//   			minDate = Calendar.getInstance();
//			minDate.setTime(root.getDateFrom());
//   		}
//   		
//   		Calendar maxDate = Calendar.getInstance();
//   		if (root.getDateTo() != null) {
//			maxDate.setTime(root.getDateTo());
//   		}
//   		
//   		System.out.println(dateFormat.format(minDate.getTime()) + " >>> " + dateFormat.format(maxDate.getTime()) );
//   		Calendar endDate = maxDate;
//   		endDate.add(Calendar.DAY_OF_MONTH, 1);
//   		
//   		String interval = root.getDateInterval();
//   		while (endDate.after(minDate)) {
//   			String resInterval = null;
//   			Calendar startDate = (Calendar)endDate.clone();
////   			startDate.add(Calendar.DAY_OF_MONTH, -1);
//   			if (interval == null) {
//   				startDate.add(Calendar.YEAR, -10);
////   				startDate.add(Calendar.DAY_OF_MONTH, 1);
//   				resInterval = "10Y";
//   			} else if (interval.equals("10Y")) {
//   				startDate.add(Calendar.YEAR, -1);
////   				startDate.add(Calendar.DAY_OF_MONTH, 1);
//   				resInterval = "1Y";
//   			} else if (interval.equals("1Y")) {
//   				startDate.add(Calendar.MONTH, -1);
////   				startDate.add(Calendar.DAY_OF_MONTH, 1);
//   				resInterval = "1M";
//   			}
//   			
//   			if (startDate.before(minDate)) {
//   				startDate = minDate;
//   				
//   				Calendar startDate2 = (Calendar)startDate.clone();
//   				startDate2.add(Calendar.YEAR, 1);
//   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
//   					resInterval = "1Y";
//   				} else {
//   					startDate2.add(Calendar.YEAR, -1);
//   					startDate2.add(Calendar.MONTH, 1);
//   	   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
//   	   					resInterval = "1M";
//   	   				}
//   				}
//   			}
//   			
//   			endDate.add(Calendar.DAY_OF_MONTH, -1);
//   			
//   			
//   			if (dimension == Dimension.FOUNDING) {
//   				sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, Code.createDateCode(new Date(startDate.getTime().getTime()), new Date(endDate.getTime().getTime())), dissolutionDate); 
//   			} else if (dimension == Dimension.DISSOLUTION) {
//   				sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, Code.createDateCode(new Date(startDate.getTime().getTime()), new Date(endDate.getTime().getTime())));
//   			}
//   			
//	        query = sparql.countSelectQuery() ;
//	
////	        System.out.println(dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime()) + " " + resInterval);
//	        System.out.println("DS " + query);
//	
//	        int tries = 0;
//	        while (tries < 3) {
//	        	tries++;
//		        
//	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
//	            	ResultSet rs = qe.execSelect();
//	            	
//	            	while (rs.hasNext()) {
//	            		QuerySolution sol = rs.next();
//	
//	//            		System.out.println(sol);
//	            		int count = sol.get("count").asLiteral().getInt();
//	            		
//	//        			System.out.println(Code.createDateCode(new Date(startDate.getTime().getTime()).toString(), new Date(endDate.getTime().getTime()).toString(), resInterval) + " " + count);
//	            		if (count > 0) {
//	            			StatisticResult sr = new StatisticResult();
//	            			sr.setCountry(cc.getCountryCode());
//	            			sr.setCode(Code.createDateCode(new Date(startDate.getTime().getTime()).toString(), new Date(endDate.getTime().getTime()).toString(), resInterval));
//	            			sr.setCount(count);
//	            			sr.setParentCode(root);
//	            			res.add(sr);
//	            		}
//	            	}
//		        } catch (QueryExceptionHTTP ex) {
//		        	System.out.println(ex.getMessage());
//		        	System.out.println(ex.getResponse());
//		        	System.out.println(ex.getResponseCode());
//		        	System.out.println(ex.getResponseMessage());
//		        	
//		        	continue;
//		        }
//	            
//	            break;
//	        }
//            
//            endDate = startDate;
//   		}
//
//    }        

    private static Calendar round(Calendar idate, String accuracy, boolean up) {

    	Calendar date = Calendar.getInstance();
    	date.setTime(idate.getTime());
    	
    	int year = date.get(Calendar.YEAR);
    	int month = date.get(Calendar.MONTH);
    	
    	if (accuracy.equals(Code.date1M)) {
    		if (up) {
    			if (month == 0 || month == 2 || month == 4 || month == 6 || month == 7 || month == 9 || month == 11) {
	    			date.set(Calendar.DAY_OF_MONTH, 31);
    			} else if (month == 1) {
    				date.set(Calendar.MONTH, 2);
    				date.set(Calendar.DAY_OF_MONTH, 1);
    				date.add(Calendar.DAY_OF_MONTH, -1);
    			} else if (month == 3 || month == 5 || month == 8 || month == 10) {
    				date.set(Calendar.DAY_OF_MONTH, 30);
    			}
    		} else {
   				date.set(Calendar.DAY_OF_MONTH, 1);
    		}
    	} else if (accuracy.equals(Code.date3M)) {
    		if (up) {
    			if (month <= 2) {
    				date.set(Calendar.MONTH, 2);
	    			date.set(Calendar.DAY_OF_MONTH, 31);
    			} else if (month <= 5) {
    				date.set(Calendar.MONTH, 5);
    				date.set(Calendar.DAY_OF_MONTH, 30);
    			} else if (month <= 8) {
    				date.set(Calendar.MONTH, 8);
    				date.set(Calendar.DAY_OF_MONTH, 30);
    			} else if (month <= 11) {
    				date.set(Calendar.MONTH, 11);
    				date.set(Calendar.DAY_OF_MONTH, 31);
    			}
    		} else {
   				if (month <= 2) {
    				date.set(Calendar.MONTH, 0);
    			} else if (month <= 5) {
    				date.set(Calendar.MONTH, 3);
    			} else if (month <= 8) {
    				date.set(Calendar.MONTH, 6);
    			} else if (month <= 11) {
    				date.set(Calendar.MONTH, 9);
    			}
   				date.set(Calendar.DAY_OF_MONTH, 1);
    		}
    	} else if (accuracy.equals(Code.date1Y)) {
    		if (up) {
   				date.set(Calendar.MONTH, 11);
    			date.set(Calendar.DAY_OF_MONTH, 31);
    		} else {
   				date.set(Calendar.MONTH, 0);
    			date.set(Calendar.DAY_OF_MONTH, 1);
    		}
    	
    	} else if (accuracy.equals(Code.date10Y)) {
    		if (up) {
   				date.set(Calendar.YEAR, year + 9 - year % 10);
   				date.set(Calendar.MONTH, 11);
       			date.set(Calendar.DAY_OF_MONTH, 31);
   			} else {
   				date.set(Calendar.YEAR, year - year % 10);
   				date.set(Calendar.MONTH, 0);
       			date.set(Calendar.DAY_OF_MONTH, 1);
   			}
    	}
    	
    	return date;
    }
}
