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

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    public void computeAndSaveAllStatistics(CountryConfiguration cc, Code date, boolean cdata, boolean cnuts, boolean cnace, boolean cfounding, boolean cdissolution, boolean cnuts_nace) {
    	
    	StatisticsCache sc = new StatisticsCache(cc);
    	
    	if (cdata) {
	    	logger.info("Computing DATA statistics for " + cc.getCountryCode());
	    	
	    	String query = SparqlQuery.buildCoreQuery(cc, false, null, null, null, null, null).countSelectQuery();
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
	        			stat.setUpdated(cc.getLastUpdated());
	        			stat.setCount(count);
	        			
	//        			System.out.println(stat.getCountry() + " " + stat.getCount());
	        			statisticsRepository.save(stat);
	        		}
	        	}
	        }
    	}
    	
    	if (cnuts && cc.isNuts()) {
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
    			stat.setUpdated(cc.getLastUpdated());
    			stat.setCount(sr.getCount());
    			
//    			System.out.println(stat.getPlace() + " " + stat.getCount());
    			statisticsRepository.save(stat);
    			
    		}
    		
    		if (cc.isNace() && cnuts_nace) {

    			for (StatisticResult iter : nuts) {
    				System.out.println("With NUTS " + iter.getCode());
//    				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCountryCode(), Dimension.NUTSLAU_NACE, iter.getUri());
    				
    				List<Code> nutsLauList = new ArrayList<>();
    				nutsLauList.add(iter.getCode());
    				
    	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, nutsLauList, null, null, null, true, sc);
    	    		
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
    	    			stat.setUpdated(cc.getLastUpdated());
    	    			stat.setCount(sr.getCount());
    	    			
//    	    			System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount());
    	    			statisticsRepository.save(stat);
    	    		}
    			}
    		}
		}
//		
		if (cnace && cc.isNace()) {
    		
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
				stat.setUpdated(cc.getLastUpdated());
				stat.setCount(sr.getCount());
				
//				System.out.println(stat.getActivity() + " " + stat.getCount());
				statisticsRepository.save(stat);
			}
			
		}
//		
		if (cfounding && cc.isFoundingDate()) {
    		logger.info("Computing founding date statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nuts;
			nuts = dateStatistics(cc, Dimension.FOUNDING, date, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.FOUNDING);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCountryCode());
    			stat.setDimension(Dimension.FOUNDING);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(sr.getCode().getDateInterval());
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getFromDate().toString());
    			}
    			stat.setUpdated(cc.getLastUpdated());
    			stat.setCount(sr.getCount());
    			
    			statisticsRepository.save(stat);
    		}
		}
		
		if (cdissolution && cc.isDissolutionDate()) {
    		logger.info("Computing dissolution date statistics for " + cc.getCountryCode());
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.DISSOLUTION, date, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCountryCode(), Dimension.DISSOLUTION);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCountryCode());
    			stat.setDimension(Dimension.DISSOLUTION);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(sr.getCode().getDateInterval());
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getFromDate().toString());
    			}
    			stat.setUpdated(cc.getLastUpdated());
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
	    	
   		public StatisticsHelper(CountryConfiguration cc, Dimension dimension, List<Code> nutsLauCodes, List<Code> naceCodes) {
   	    	Map<CountryConfiguration, PlaceSelection> countryPlaceMap;
   	    	if (nutsLauCodes != null) {
   	    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
   	    	} else {
   	    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
   	    	}
   	    	
   	    	PlaceSelection places = countryPlaceMap.get(cc);
   	    	
//   	        if (dimension == Dimension.NUTS || dimension == Dimension.LAU) {
   	    	if (dimension == Dimension.NUTSLAU) {
   	        	naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCodes);
   	        } else if (dimension == Dimension.NACE) {
   		        nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, places.getNuts3()); 
   		        lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places.getLau());
   	        } 

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
   		
   		boolean ok = false;
   		int tries = 0;
   		while (!ok && tries < 3) {
   			try {
				statistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res, sc);
		    		
		   		if (allLevels) {
		    		for (int i = 0; i < res.size(); i++) {
		    			statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
		    		}
				}
		   		
		   		ok = true;
		   		
   			} catch (QueryExceptionHTTP ex) { // why does this happen?
   				tries++;
   			}
   		}
   		
   		return res;
   	}
   	
   	
   	
   	public List<StatisticResult> dateStatistics(CountryConfiguration cc, Dimension dimension, Code date, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
   		
		dateStatistics(cc, dimension, date, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, res);
    		
   		if (allLevels) {
    		for (int i = 0; i < res.size(); i++) {
    			dateStatistics(cc, dimension, res.get(i).getCode(), nutsLauCodes, naceCodes, foundingDate, dissolutionDate, res);
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
	        		sparql = SparqlQuery.buildCoreQuery(cc, false, nutsService.getLocalNutsLeafUrisDB(cc, code), null, sh.naceLeafUris, foundingDate, dissolutionDate);
	        	} else if (code.isLau()) {
	        		sparql = SparqlQuery.buildCoreQuery(cc, false, null, Arrays.asList(nutsService.getLocalLauUri(cc, code)), sh.naceLeafUris, foundingDate, dissolutionDate);
	        	}
	        } else if (dimension == Dimension.NACE) {
	        	List<String> naceLeafUris = (sc == null) ? naceService.getLocalNaceLeafUris(cc, code) : sc.nacelookup(code);
	        	
            	if (naceLeafUris.isEmpty()) {
            		continue;
            	}
            	
		        sparql = SparqlQuery.buildCoreQuery(cc, false, sh.nutsLeafUris, sh.lauUris, naceLeafUris, foundingDate, dissolutionDate); 
	        }
	  
	        String query = sparql.countSelectQuery() ;
	
//	        System.out.println(uri);
//	        System.out.println(cc.getDataEndpoint());
//	        System.out.println(query);
//	        long start = System.currentTimeMillis();
	        
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		QuerySolution sol = rs.next();

//            		System.out.println(sol);
            		int count = sol.get("count").asLiteral().getInt();
            		
//            		System.out.println(code.getCode() + " " + count);

            		if (count > 0) {
            			StatisticResult sr = new StatisticResult();
            			sr.setCountry(cc.getCountryCode());
            			sr.setCode(code);
            			sr.setParentCode(root);
            			sr.setCount(count);
            			res.add(sr);
            		}
            	}
	        } catch (QueryExceptionHTTP ex) {
	        	System.out.println(ex.getMessage());
	        	System.out.println(ex.getResponse());
	        	System.out.println(ex.getResponseCode());
	        	System.out.println(ex.getResponseMessage());
	        	
	        	throw ex;
	        }
        }        	        

    }    

    
    public StatisticResult singleStatistic(CountryConfiguration cc, Code nutsLauCode, Code naceCode, Code foundingDate, Code dissolutionDate) {
        StatisticResult res = null;
        			
		List<String> nutsUris = (nutsLauCode != null && nutsLauCode.isNuts()) ? nutsUris = nutsService.getLocalNutsLeafUrisDB(cc, nutsLauCode) : null ;
		List<String> lauUris = (nutsLauCode != null && nutsLauCode.isLau()) ? Arrays.asList(nutsService.getLocalLauUri(cc, nutsLauCode)) : null ;    		
    	List<String> naceUris = naceCode != null ? naceService.getLocalNaceLeafUris(cc, naceCode) : null ;
    	
        String query = SparqlQuery.buildCoreQuery(cc, false, nutsUris, lauUris, naceUris, foundingDate, dissolutionDate).countSelectQuery() ;
//	    System.out.println(query);
        
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next();

        		int count = sol.get("count").asLiteral().getInt();
        		if (count > 0) {
        			StatisticResult sr = new StatisticResult();
        			sr.setCountry(cc.getCountryCode());
        			sr.setCount(count);
        			res = sr;
        		}
        	}
        } catch (QueryExceptionHTTP ex) {
        	System.out.println(ex.getMessage());
        	System.out.println(ex.getResponse());
        	System.out.println(ex.getResponseCode());
        	System.out.println(ex.getResponseMessage());
        	
        	throw ex;
        }
    
    	return res;

    }    
    
    public void dateStatistics(CountryConfiguration cc, Dimension dimension, Code root, List<Code> nutsLauCode, List<Code> naceCode, Code foundingDate, Code dissolutionDate,  List<StatisticResult> res) {
//    	System.out.println(">> " + root);
    	
    	if (root.isDateInterval1M()) {
    		return;
    	}
    	
    	Map<CountryConfiguration, PlaceSelection> requestMap;
    	if (nutsLauCode != null) {
    		requestMap = nutsService.getEndpointsByNuts(nutsLauCode);
    	} else {
    		requestMap = nutsService.getEndpointsByNuts(); 
    	}
    	
    	PlaceSelection regions = requestMap.get(cc);

    	List<String> naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCode);
    	List<String> nutsLeafUris = regions == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, regions.getNuts3());
    	List<String> lauUris = regions == null ? null : nutsService.getLocalLauUris(cc, regions.getLau());
    	
    	String prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    	SparqlQuery sparql = null; 
		String query = null;
		
		if (dimension == Dimension.FOUNDING) {
			sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, null, dissolutionDate);
   			query = prefix + "SELECT (MIN(?foundingDate) AS ?date) WHERE { " + sparql.getWhere() + " " + cc.getFoundingDateSparql() + " } " ;
		} else if (dimension == Dimension.DISSOLUTION) {
			sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, null);
			query = prefix + "SELECT (MIN(?dissolutionDate) AS ?date) WHERE { " + sparql.getWhere() + " " + cc.getDissolutionDateSparql() + " } " ;
		}

   		Calendar minDate = null;
   		if (root.getDateFrom() == null) {
	   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
		    	ResultSet rs = qe.execSelect();
		    	
		    	while (rs.hasNext()) {
		    		QuerySolution sol = rs.next();
		
		    		minDate = ((XSDDateTime)sol.get("date").asLiteral().getValue()).asCalendar();
		    	}
		    }
   		} else {
   			minDate = Calendar.getInstance();
			minDate.setTime(root.getDateFrom());
   		}
   		
   		Calendar maxDate = Calendar.getInstance();
   		if (root.getDateTo() != null) {
			maxDate.setTime(root.getDateTo());
   		}
   		
//   		System.out.println(dateFormat.format(minDate.getTime()) + " >>> " + dateFormat.format(maxDate.getTime()) );
   		Calendar endDate = maxDate;
   		endDate.add(Calendar.DAY_OF_MONTH, 1);
   		
   		String interval = root.getDateInterval();
   		while (endDate.after(minDate)) {
   			String resInterval = null;
   			Calendar startDate = (Calendar)endDate.clone();
//   			startDate.add(Calendar.DAY_OF_MONTH, -1);
   			if (interval == null) {
   				startDate.add(Calendar.YEAR, -10);
//   				startDate.add(Calendar.DAY_OF_MONTH, 1);
   				resInterval = "10Y";
   			} else if (interval.equals("10Y")) {
   				startDate.add(Calendar.YEAR, -1);
//   				startDate.add(Calendar.DAY_OF_MONTH, 1);
   				resInterval = "1Y";
   			} else if (interval.equals("1Y")) {
   				startDate.add(Calendar.MONTH, -1);
//   				startDate.add(Calendar.DAY_OF_MONTH, 1);
   				resInterval = "1M";
   			}
   			
   			if (startDate.before(minDate)) {
   				startDate = minDate;
   				
   				Calendar startDate2 = (Calendar)startDate.clone();
   				startDate2.add(Calendar.YEAR, 1);
   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
   					resInterval = "1Y";
   				} else {
   					startDate2.add(Calendar.YEAR, -1);
   					startDate2.add(Calendar.MONTH, 1);
   	   				if (startDate2.after((endDate)) || startDate2.equals(endDate)) {
   	   					resInterval = "1M";
   	   				}
   				}
   			}
   			
   			endDate.add(Calendar.DAY_OF_MONTH, -1);
   			
   			if (dimension == Dimension.FOUNDING) {
   				sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, Code.createDateCode(new Date(startDate.getTime().getTime()), new Date(endDate.getTime().getTime())), dissolutionDate); 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				sparql = SparqlQuery.buildCoreQuery(cc, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, Code.createDateCode(new Date(startDate.getTime().getTime()), new Date(endDate.getTime().getTime())));
   			}
   			
	        query = sparql.countSelectQuery() ;
	
//	        System.out.println(dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime()) + " " + resInterval);
//	        System.out.println(query);
	
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		QuerySolution sol = rs.next();

//            		System.out.println(sol);
            		int count = sol.get("count").asLiteral().getInt();
            		
//        			System.out.println(Code.createDateCode(new Date(startDate.getTime().getTime()).toString(), new Date(endDate.getTime().getTime()).toString(), resInterval) + " " + count);
            		if (count > 0) {
            			StatisticResult sr = new StatisticResult();
            			sr.setCountry(cc.getCountryCode());
            			sr.setCode(Code.createDateCode(new Date(startDate.getTime().getTime()).toString(), new Date(endDate.getTime().getTime()).toString(), resInterval));
            			sr.setCount(count);
            			sr.setParentCode(root);
            			res.add(sr);
            		}
            	}
	        }
            
            endDate = startDate;
   		}

    }        
}
