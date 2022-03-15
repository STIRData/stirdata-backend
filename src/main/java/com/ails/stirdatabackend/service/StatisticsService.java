package com.ails.stirdatabackend.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticResult;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.service.NutsService.PlaceSelection;

@Service
public class StatisticsService {
	
	private final static Logger logger = LoggerFactory.getLogger(StatisticsService.class);
	
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private PlacesDBRepository placeRepository;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

	public void computeStatistics(CountryDB cc, Set<Dimension> stats) {
		Set<Dimension> dims = new HashSet<>();
		
		if (stats.contains(Dimension.DATA)) {
			dims.add(Dimension.DATA);
		}
    	
    	if (cc.isNuts()) {
    		if (stats.contains(Dimension.NUTSLAU)) {
    			dims.add(Dimension.NUTSLAU);
    		}

        	if (cc.isNace() && stats.contains(Dimension.NUTSLAU_NACE)) {
        		dims.add(Dimension.NUTSLAU_NACE);
        	}
        	if (cc.isFoundingDate() && stats.contains(Dimension.NUTSLAU_FOUNDING)) {
            	dims.add(Dimension.NUTSLAU_FOUNDING);
        	}
        	if (cc.isDissolutionDate() && stats.contains(Dimension.NUTSLAU_DISSOLUTION)) {
        		dims.add(Dimension.NUTSLAU_DISSOLUTION);
        	}
    	}
    	
    	if (cc.isNace()) {
    		if (stats.contains(Dimension.NACE)) {
    			dims.add(Dimension.NACE);
    		}
        	
        	if (cc.isFoundingDate() && stats.contains(Dimension.NACE_FOUNDING)) {
            	dims.add(Dimension.NACE_FOUNDING);
        	}
        	if (cc.isDissolutionDate() && stats.contains(Dimension.NACE_DISSOLUTION)) {
        		dims.add(Dimension.NACE_DISSOLUTION);
        	}
    	}
    	
    	if (cc.isFoundingDate() && stats.contains(Dimension.FOUNDING)) {
        	dims.add(Dimension.FOUNDING);
    	}
    	
    	if (cc.isDissolutionDate() && stats.contains(Dimension.DISSOLUTION)) {
        	dims.add(Dimension.DISSOLUTION);
    	}

    	
		for (Statistic s : statisticsRepository.groupCountDimensionsByCountry(cc.getCode()) ) {
			Dimension d = s.getDimension(); 	
			
			Calendar ccDate = Calendar.getInstance();
			java.util.Date sqlDate = cc.getStatsDate(d);
			
			if (sqlDate != null) {
				ccDate.setTimeInMillis(sqlDate.getTime());
					
				if (ccDate.getTime().equals(cc.getLastUpdated())) {
					dims.remove(d);
					logger.info("Statistics for " + cc.getCode() + " / " + d + " already computed.");
				}
			}
				
		}
			
    	computeAndSaveAllStatistics(cc, dims);
    	
	}

    private void computeAndSaveAllStatistics(CountryDB cc, Set<Dimension> dimensions) {
    	
//    	logger.info("Statistics to compute " + dimensions);
    	
    	StatisticsCache sc = new StatisticsCache(cc);
    	
    	if (dimensions.contains(Dimension.DATA)) {
	    	
    		logger.info("Computing " + Dimension.DATA + " statistics for " + cc.getCode());
	    	
	    	String query = SparqlQuery.buildCoreQuery(cc, true, false, null, null, null, null, null).countSelectQuery();
	    	statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.DATA);
	    	
	    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	        	ResultSet rs = qe.execSelect();
	        	
	        	while (rs.hasNext()) {
	        		QuerySolution sol = rs.next();
	
	        		int count = sol.get("count").asLiteral().getInt();
	        		if (count > 0) {
	        			Statistic stat = new Statistic();
	        			stat.setCountry(cc.getCode());
	        			stat.setDimension(Dimension.DATA);
	        			stat.setUpdated(new java.util.Date());
	        			stat.setReferenceDate(cc.getLastUpdated());
	        			stat.setCount(count);
	        			
	//        			System.out.println(stat.getCountry() + " " + stat.getCount());
	        			statisticsRepository.save(stat);
	        		}
	        	}
	        }
	    	
	    	logger.info("Computing " + Dimension.DATA + " statistics for " + cc.getCode() + " completed.");
    	}
    	
    	if (cc.isNuts() && dimensions.contains(Dimension.NUTSLAU)) {
    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode());
    		
    		List<StatisticResult> nuts = statistics(cc, Dimension.NUTSLAU, null, null, null, null, null, true, sc);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCode());
    			stat.setDimension(Dimension.NUTSLAU);
    			stat.setPlace(sr.getCode().toString());
    			if (sr.getParentCode() != null) {
    				stat.setParentPlace(sr.getParentCode().toString());
    			}
    			stat.setUpdated(sr.getComputed());
    			stat.setReferenceDate(cc.getLastUpdated());
    			stat.setCount(sr.getCount());
    			
//    			System.out.println(stat.getPlace() + " " + stat.getCount());
    			statisticsRepository.save(stat);
    		}
    		
    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode() + " completed.");
    	}
    	
		if (cc.isNace() && dimensions.contains(Dimension.NACE)) {
    		
    		logger.info("Computing " + Dimension.NACE + " statistics for " + cc.getCode());
    		
    		List<StatisticResult> nace = statistics(cc, Dimension.NACE, null, null, null, null, null, true, sc);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.NACE);
    		
			for (StatisticResult sr : nace) {
				Statistic stat = new Statistic();
				stat.setCountry(cc.getCode());
				stat.setDimension(Dimension.NACE);
				stat.setActivity(sr.getCode().toString());
				if (sr.getParentCode() != null) {
					stat.setParentActivity(sr.getParentCode().toString());
				}
				stat.setUpdated(sr.getComputed());
				stat.setReferenceDate(cc.getLastUpdated());
				stat.setCount(sr.getCount());
				
//				System.out.println(stat.getActivity() + " " + stat.getCount());
				statisticsRepository.save(stat);
			}
			
			logger.info("Computing " + Dimension.NACE + " statistics for " + cc.getCode() + " completed.");
			
		}
    	
		if (cc.isFoundingDate() && dimensions.contains(Dimension.FOUNDING)) {
    		
			logger.info("Computing " + Dimension.FOUNDING + " statistics for " + cc.getCode());
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.FOUNDING, null, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.FOUNDING);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCode());
    			stat.setDimension(Dimension.FOUNDING);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
    			}
    			stat.setUpdated(sr.getComputed());
    			stat.setReferenceDate(cc.getLastUpdated());
    			stat.setCount(sr.getCount());
    			
//    			System.out.println(stat.getFromDate() + " " + stat.getToDate() + " " + stat.getDateInterval());
    			
    			statisticsRepository.save(stat);
    		}
    		
    		logger.info("Computing " + Dimension.FOUNDING + " statistics for " + cc.getCode() + " completed.");
		}
		
		if (cc.isDissolutionDate() && dimensions.contains(Dimension.DISSOLUTION)) {
    		
			logger.info("Computing " + Dimension.DISSOLUTION + " statistics for " + cc.getCode());
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.DISSOLUTION, null, null, null, null, null, true);
    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.DISSOLUTION);
    		
    		for (StatisticResult sr : nuts) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCode());
    			stat.setDimension(Dimension.DISSOLUTION);
    			stat.setFromDate(sr.getCode().getDateFrom().toString());
    			stat.setToDate(sr.getCode().getDateTo().toString());
    			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
    			if (sr.getParentCode() != null) {
    				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
    				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
    			}
    			stat.setUpdated(sr.getComputed());
    			stat.setReferenceDate(cc.getLastUpdated());
    			stat.setCount(sr.getCount());
    			
    			statisticsRepository.save(stat);
    		}
    		
    		logger.info("Computing " + Dimension.DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
		}

    	
//    	if (cc.isNuts() && cc.isNace() && dimensions.contains(Dimension.NUTSLAU_NACE)) {
//
//    		List<StatisticResult> nuts = new ArrayList<>(); 
//			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU)) {
//				StatisticResult sr = new StatisticResult();
//				sr.setCode(new Code(stat.getPlace()));
//				sr.setCountry(stat.getCountry());
//				sr.setCount(stat.getCount());
//				sr.setComputed(stat.getUpdated());
//				if (stat.getParentPlace() != null) {
//					sr.setParentCode(new Code(stat.getParentPlace()));
//				}
//				nuts.add(sr);
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode());
//			
//			for (StatisticResult iter : nuts) {
//				System.out.println("With NUTS " + iter.getCode());
//				
//				List<Code> nutsLauList = new ArrayList<>();
//				nutsLauList.add(iter.getCode());
//				
//	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, nutsLauList, null, null, null, true, sc);
////				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
//	    		
//	      		for (StatisticResult sr : nutsNace) {
//	    			Statistic stat = new Statistic();
//	    			stat.setCountry(cc.getCode());
//	    			stat.setDimension(Dimension.NUTSLAU_NACE);
//	    			stat.setPlace(iter.getCode().toString());
//	    			if (iter.getParentCode() != null) {
//	    				stat.setParentPlace(iter.getParentCode().toString());
//	    			}
//	    			stat.setActivity(sr.getCode().toString());
//	    			if (sr.getParentCode() != null) {
//	    				stat.setParentActivity(sr.getParentCode().toString());
//	    			}
//	    			stat.setUpdated(sr.getComputed());
//	    			stat.setReferenceDate(cc.getLastUpdated());	    			
//	    			stat.setCount(sr.getCount());
//	    			
////    	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
////	    			statisticsRepository.save(stat);
//	    		}
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode() + " completed.");
//   		}
		
    	if (cc.isNuts() && cc.isNace() && dimensions.contains(Dimension.NUTSLAU_NACE)) {

    		List<StatisticResult> nuts = new ArrayList<>(); 
    		List<Code> nuts3 = new ArrayList<>();
    		List<Code> lau = new ArrayList<>();
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU)) {
				Code code = new Code(stat.getPlace());
				StatisticResult sr = new StatisticResult();
				sr.setCode(code);
				sr.setCountry(stat.getCountry());
				sr.setCount(stat.getCount());
				sr.setComputed(stat.getUpdated());
				if (stat.getParentPlace() != null) {
					sr.setParentCode(new Code(stat.getParentPlace()));
				}
				if (code.isLau()) {
					lau.add(code);
				} else if (code.getNutsLevel() == 3) {
					nuts3.add(code);
				} else {
					nuts.add(sr);
				}
			}
			
//			Collections.sort(nuts, new Comparator<StatisticResult>() {
//				@Override
//				public int compare(StatisticResult o1, StatisticResult o2) {
//					if (o1.getParentCode() == null && o2.getParentCode() == null) {
//						return 0;
//					} else if (o1.getParentCode() == null && o2.getParentCode() != null) {
//						return -1;
//					} else if (o1.getParentCode() != null && o2.getParentCode() == null) {
//						return 1;
//					} else {
//						return o1.getParentCode().toString().compareTo(o2.getParentCode().toString());
//					}
//				}
//			});
			
			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode());

//			for (StatisticResult iter : nuts) {
//				
//				System.out.println("With NUTS " + iter.getCode());
//				
//				List<Code> nutsLauList = new ArrayList<>();
//				nutsLauList.add(iter.getCode());
//
//	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, nutsLauList, null, null, null, true, sc);
////				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
//	    		
//	      		for (StatisticResult sr : nutsNace) {
//	    			Statistic stat = new Statistic();
//	    			stat.setCountry(cc.getCode());
//	    			stat.setDimension(Dimension.NUTSLAU_NACE);
//	    			stat.setPlace(iter.getCode().toString());
//	    			if (iter.getParentCode() != null) {
//	    				stat.setParentPlace(iter.getParentCode().toString());
//	    			}
//	    			stat.setActivity(sr.getCode().toString());
//	    			if (sr.getParentCode() != null) {
//	    				stat.setParentActivity(sr.getParentCode().toString());
//	    			}
//	    			stat.setUpdated(sr.getComputed());
//	    			stat.setReferenceDate(cc.getLastUpdated());	    			
//	    			stat.setCount(sr.getCount());
//	    			
////    	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
////	    			statisticsRepository.save(stat);
//	    		}
//			}
			
//			System.out.println("With NUTS " + nuts3);
//    		List<StatisticResult> nuts3Nace = statistics(cc, Dimension.NACE, null, nuts3, null, null, null, true, sc);
////				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
//    		
//      		for (StatisticResult sr : nuts3Nace) {
//    			Statistic stat = new Statistic();
//    			stat.setCountry(cc.getCode());
//    			stat.setDimension(Dimension.NUTSLAU_NACE);
//    			stat.setPlace(sr.getGroupByCode().toString());
//    			PlaceDB placedb = placeRepository.findByCode(sr.getGroupByCode());
//    			if (placedb.getParent() != null) {
//    				stat.setParentPlace(placedb.getParent().getCode().toString());
//    			}
//    			stat.setActivity(sr.getCode().toString());
//    			if (sr.getParentCode() != null) {
//    				stat.setParentActivity(sr.getParentCode().toString());
//    			}
//    			stat.setUpdated(sr.getComputed());
//    			stat.setReferenceDate(cc.getLastUpdated());	    			
//    			stat.setCount(sr.getCount());
//    			
////    	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
////	    			statisticsRepository.save(stat);
//    		}
      		
      		System.out.println("With NUTS " + lau);
    		List<StatisticResult> lauNace = statistics(cc, Dimension.NACE, null, lau, null, null, null, true, sc);
//				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
    		
      		for (StatisticResult sr : lauNace) {
    			Statistic stat = new Statistic();
    			stat.setCountry(cc.getCode());
    			stat.setDimension(Dimension.NUTSLAU_NACE);
    			stat.setPlace(sr.getGroupByCode().toString());
    			PlaceDB placedb = placeRepository.findByCode(sr.getGroupByCode());
    			if (placedb.getParent() != null) {
    				stat.setParentPlace(placedb.getParent().getCode().toString());
    			}
    			stat.setActivity(sr.getCode().toString());
    			if (sr.getParentCode() != null) {
    				stat.setParentActivity(sr.getParentCode().toString());
    			}
    			stat.setUpdated(sr.getComputed());
    			stat.setReferenceDate(cc.getLastUpdated());	    			
    			stat.setCount(sr.getCount());
    			
//    	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
//	    			statisticsRepository.save(stat);
    		}	      		
			
			
			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode() + " completed.");
   		}
    	
//    	if (cc.isNuts() && cc.isNace() && dimensions.contains(Dimension.NUTSLAU_NACE)) {
//
//    		List<StatisticResult> nace = new ArrayList<>(); 
//			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NACE)) {
//				StatisticResult sr = new StatisticResult();
//				sr.setCode(new Code(stat.getActivity()));
//				sr.setCountry(stat.getCountry());
//				sr.setCount(stat.getCount());
//				sr.setComputed(stat.getUpdated());
//				if (stat.getParentActivity() != null) {
//					sr.setParentCode(new Code(stat.getParentActivity()));
//				}
//				nace.add(sr);
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode());
//			
//			for (StatisticResult iter : nace) {
//				System.out.println("With NACE " + iter.getCode());
//				
//				List<Code> naceList = new ArrayList<>();
//				naceList.add(iter.getCode());
//				
//	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NUTSLAU, null, null, naceList, null, null, true, sc);
////				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
//	    		
//	      		for (StatisticResult sr : nutsNace) {
//	    			Statistic stat = new Statistic();
//	    			stat.setCountry(cc.getCode());
//	    			stat.setDimension(Dimension.NUTSLAU_NACE);
//	    			stat.setActivity(iter.getCode().toString());
//	    			if (iter.getParentCode() != null) {
//	    				stat.setParentActivity(iter.getParentCode().toString());
//	    			}
//	    			stat.setPlace(sr.getCode().toString());
//	    			if (sr.getParentCode() != null) {
//	    				stat.setParentPlace(sr.getParentCode().toString());
//	    			}
//	    			stat.setUpdated(sr.getComputed());
//	    			stat.setReferenceDate(cc.getLastUpdated());	    			
//	    			stat.setCount(sr.getCount());
//	    			
////    	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
////	    			statisticsRepository.save(stat);
//	    		}
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode() + " completed.");
//   		}		
    	
    	if (cc.isNuts() && cc.isFoundingDate() && dimensions.contains(Dimension.NUTSLAU_FOUNDING)) {

    		List<StatisticResult> nuts = new ArrayList<>(); 
    		
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU)) {
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
    		
    		logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode());

			for (StatisticResult iter : nuts) {
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
    		
    			nuts = dateStatistics(cc, Dimension.FOUNDING, null, nutsLauList, null, null, null, true);
    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_FOUNDING, iter.getCode().toString());
    			
        		for (StatisticResult sr : nuts) {
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
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
        			stat.setUpdated(sr.getComputed());
        			stat.setReferenceDate(cc.getLastUpdated());
        			stat.setCount(sr.getCount());
        			
	        		statisticsRepository.save(stat);
        		}
			}
			
			logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode() + " completed.");
		}
    	
    	if (cc.isNuts() && cc.isDissolutionDate() && dimensions.contains(Dimension.NUTSLAU_DISSOLUTION)) {

    		List<StatisticResult> nuts = new ArrayList<>(); 
    		
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU)) {
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
    		
    		logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode());

			for (StatisticResult iter : nuts) {
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
    		
    			nuts = dateStatistics(cc, Dimension.DISSOLUTION, null, nutsLauList, null, null, null, true);
    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION, iter.getCode().toString());
    			
        		for (StatisticResult sr : nuts) {
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
        			stat.setDimension(Dimension.NUTSLAU_DISSOLUTION);
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
        			stat.setUpdated(sr.getComputed());
        			stat.setReferenceDate(cc.getLastUpdated());
        			stat.setCount(sr.getCount());
        			
	        		statisticsRepository.save(stat);
        		}
			}
			
			logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
		}
    	
    	if (cc.isNace() && cc.isFoundingDate() && dimensions.contains(Dimension.NACE_FOUNDING)) {

    		List<StatisticResult> nace = new ArrayList<>(); 
    		
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NACE)) {
				StatisticResult sr = new StatisticResult();
				sr.setCode(new Code(stat.getActivity()));
				sr.setCountry(stat.getCountry());
				sr.setCount(stat.getCount());
				sr.setComputed(stat.getUpdated());
				if (stat.getParentActivity() != null) {
					sr.setParentCode(new Code(stat.getParentActivity()));
				}
				nace.add(sr);
			}    	
    		
    		logger.info("Computing " + Dimension.NACE_FOUNDING + " statistics for " + cc.getCode());

			for (StatisticResult iter : nace) {
				System.out.println("With NACE " + iter.getCode());
				
				List<Code> naceList = new ArrayList<>();
				naceList.add(iter.getCode());
    		
    			nace = dateStatistics(cc, Dimension.FOUNDING, null, null, naceList, null, null, true);
    			statisticsRepository.deleteAllByCountryAndDimensionAndActivity(cc.getCode(), Dimension.NACE_FOUNDING, iter.getCode().toString());
    			
        		for (StatisticResult sr : nace) {
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
        			stat.setDimension(Dimension.NACE_FOUNDING);
        			stat.setActivity(iter.getCode().toString());
	    			if (iter.getParentCode() != null) {
	    				stat.setParentActivity(iter.getParentCode().toString());
	    			}	        			
        			stat.setFromDate(sr.getCode().getDateFrom().toString());
        			stat.setToDate(sr.getCode().getDateTo().toString());
        			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
        			if (sr.getParentCode() != null) {
        				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
        				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
        			}
        			stat.setUpdated(sr.getComputed());
        			stat.setReferenceDate(cc.getLastUpdated());
        			stat.setCount(sr.getCount());
        			
	        		statisticsRepository.save(stat);
        		}
			}
			
			logger.info("Computing " + Dimension.NACE_FOUNDING + " statistics for " + cc.getCode() + " completed.");
		}
    	
    	if (cc.isNace() && cc.isDissolutionDate() && dimensions.contains(Dimension.NACE_DISSOLUTION)) {

    		List<StatisticResult> nace = new ArrayList<>(); 
    		
			for (Statistic stat : statisticsRepository.findByCountryAndDimension(cc.getCode(), Dimension.NACE)) {
				StatisticResult sr = new StatisticResult();
				sr.setCode(new Code(stat.getActivity()));
				sr.setCountry(stat.getCountry());
				sr.setCount(stat.getCount());
				sr.setComputed(stat.getUpdated());
				if (stat.getParentActivity() != null) {
					sr.setParentCode(new Code(stat.getParentActivity()));
				}
				nace.add(sr);
			}    	
    		
    		logger.info("Computing " + Dimension.NACE_DISSOLUTION + " statistics for " + cc.getCode());

			for (StatisticResult iter : nace) {
				System.out.println("With NACE " + iter.getCode());
				
				List<Code> naceList = new ArrayList<>();
				naceList.add(iter.getCode());
    		
    			nace = dateStatistics(cc, Dimension.DISSOLUTION, null, null, naceList, null, null, true);
    			statisticsRepository.deleteAllByCountryAndDimensionAndActivity(cc.getCode(), Dimension.NACE_DISSOLUTION, iter.getCode().toString());
    			
        		for (StatisticResult sr : nace) {
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
        			stat.setDimension(Dimension.NACE_DISSOLUTION);
        			stat.setActivity(iter.getCode().toString());
	    			if (iter.getParentCode() != null) {
	    				stat.setParentActivity(iter.getParentCode().toString());
	    			}	        			
        			stat.setFromDate(sr.getCode().getDateFrom().toString());
        			stat.setToDate(sr.getCode().getDateTo().toString());
        			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
        			if (sr.getParentCode() != null) {
        				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
        				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
        			}
        			stat.setUpdated(sr.getComputed());
        			stat.setReferenceDate(cc.getLastUpdated());
        			stat.setCount(sr.getCount());
        			
	        		statisticsRepository.save(stat);
        		}
			}
			
			logger.info("Computing " + Dimension.NACE_DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
		}
		
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
		
		private CountryDB cc;
   		
   		public StatisticsCache(CountryDB cc) {
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
	    
	    public boolean nuts3Roots;
	    public boolean lauRoots;
	    
	    Calendar minDate;
	    Calendar maxDate;
	    
   		public StatisticsHelper(CountryDB cc, Dimension dimension, List<Code> nutsLauCodes, List<Code> naceCodes) {
   	    	Map<CountryDB, PlaceSelection> countryPlaceMap;
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
   		        
   		        if (places.getNuts3() != null) {
	   		        nuts3Roots = places.getNuts3().size() > 0;
	   		        for (Code c : places.getNuts3()) {
	   		        	if (c.getNutsLevel() != 3) {
	   		        		nuts3Roots = false;
	   		        		break;
	   		        	}
	   		        }
   		        }
   		        
   		        if ( places.getLau() != null) {
	   		        lauRoots = places.getLau().size() > 0;
	   		        for (Code c : places.getLau()) {
	   		        	if (!c.isLau()) {
	   		        		lauRoots = false;
	   		        		break;
	   		        	}
	   		        }
   		        }

   		}


   		public void minMaxDates(CountryDB cc, Dimension dimension, Code foundingDate, Code dissolutionDate) {
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


   	public List<StatisticResult> statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		return statistics(cc, dimension, root, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, allLevels, null);
   	}
   	
   	public List<StatisticResult> statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels, StatisticsCache sc) {
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
   	
   	public List<StatisticResult> dateStatistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
   		
   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		sh.minMaxDates(cc, dimension, foundingDate, dissolutionDate);


   		if (root == null) {
   			root = Code.createDateCode(new java.sql.Date(sh.minDate.getTime().getTime()), new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
   		}
   		
		dateStatistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res);
    		
   		if (allLevels) {
    		for (int i = 0; i < res.size(); i++) {
    			dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
    		}
		}
   		
   		return res;
   	}
   	
   	public void statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {

   		if (!hasMore(dimension, root)) {
   			return;
   		}
   		
	   	statistics(cc, dimension, root, new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes), foundingDate, dissolutionDate, res, sc);
   	}
   	
   	private static int c = 0;
    private void statistics(CountryDB cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {
//    	System.out.println(">>> " + root + " " + root.isNuts() + " " + root.getNutsLevel());
	    List<Code> iterCodes = null;
	    
//	    if (root != null && root.isNuts() && root.getNutsLevel() < 3) {
//	    	System.out.println("SKIP " + root);
//	    	return;
//	    }
	    
        if (dimension == Dimension.NUTSLAU) {
        	List<PlaceDB> place = nutsService.getNextNutsLauLevelListDb(root == null ? Code.createNutsCode(cc.getCode()) : root);
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
    		
    		String query = null;
    		
        			
//	        if (dimension == Dimension.NUTS) {
    		if (dimension == Dimension.NUTSLAU) {
	        	if (code.isNuts()) {
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsService.getLocalNutsLeafUrisDB(cc, code), null, sh.naceLeafUris, foundingDate, dissolutionDate);
	        	} else if (code.isLau()) {
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, null, Arrays.asList(nutsService.getLocalLauUri(cc, code)), sh.naceLeafUris, foundingDate, dissolutionDate);
	        	}
	        	
	        	query = sparql.countSelectQuery() ;
	        } else if (dimension == Dimension.NACE) {
	        	List<String> naceLeafUris = (sc == null) ? naceService.getLocalNaceLeafUris(cc, code) : sc.nacelookup(code);
	        	
            	if (naceLeafUris.isEmpty()) {
            		continue;
            	}
            	
            	System.out.println(sh.nuts3Roots + " " + sh.lauRoots);
            	if (sh.nuts3Roots || sh.lauRoots) {
            		sparql = SparqlQuery.buildCoreQueryGroupPlace(cc, true, false, sh.nutsLeafUris, sh.lauUris, naceLeafUris, foundingDate, dissolutionDate);
            		if (sh.nuts3Roots) {
            			query = sparql.countSelectQueryGroupByNuts3();
            		} else {
            			query = sparql.countSelectQueryGroupByLau();
            		}
            	} else {
            		sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, naceLeafUris, foundingDate, dissolutionDate);
            		query = sparql.countSelectQuery() ;
            	}
	        }
	  
//    		String query = sparql.countSelectQuery() ;
	
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
	            		String uri = null;
	            		
	            		if (count > 0) {
	            			if (sh.nuts3Roots) {
	            				uri = sol.get("nuts3").asResource().toString();
	            			
		            			StatisticResult sr = new StatisticResult();
		            			sr.setCountry(cc.getCode());
		            			sr.setCode(code);
		            			sr.setParentCode(root);
		            			sr.setCount(count);
		            			sr.setComputed(new java.util.Date());
	            				sr.setGroupByCode(nutsService.getNutsCodeFromLocalUri(cc, uri));
		            			res.add(sr);
	            			} else if (sh.lauRoots) {
	            				uri = sol.get("lau").asResource().toString();
		            			
		            			StatisticResult sr = new StatisticResult();
		            			sr.setCountry(cc.getCode());
		            			sr.setCode(code);
		            			sr.setParentCode(root);
		            			sr.setCount(count);
		            			sr.setComputed(new java.util.Date());
	            				sr.setGroupByCode(nutsService.getLauCodeFromLocalUri(cc, uri));
		            			res.add(sr);
		            			
	            			} else {
		            			StatisticResult sr = new StatisticResult();
		            			sr.setCountry(cc.getCode());
		            			sr.setCode(code);
		            			sr.setParentCode(root);
		            			sr.setCount(count);
		            			sr.setComputed(new java.util.Date());
		            			res.add(sr);
	            			}
	            		}
	            		
	               		System.out.println(c++ +" "+ code.getCode() + " " + count + " " + (uri != null ? uri + " " : "") +  (System.currentTimeMillis() - start));
	               		

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
    
    
    

    
    public StatisticResult singleStatistic(CountryDB cc, Code nutsLauCode, Code naceCode, Code foundingDate, Code dissolutionDate) {
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
	        			sr.setCountry(cc.getCode());
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
    
    private void dateStatistics(CountryDB cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate,  List<StatisticResult> res) {
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
   		
		System.out.println(minDate.getTime() + " >>> " + maxDate.getTime() );
   		
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
   				sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())), dissolutionDate); 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				sparql = SparqlQuery.buildCoreQuery(cc, false, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, foundingDate, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())));
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
	            			sr.setCountry(cc.getCode());
	            			if (toDate.after(sh.maxDate)) {
	            				sr.setCode(Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()).toString(), new java.sql.Date(sh.maxDate.getTime().getTime()).toString(), nextInterval));
	            			} else {
	            				sr.setCode(Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()).toString(), new java.sql.Date(toDate.getTime().getTime()).toString(), nextInterval));
	            			}
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
