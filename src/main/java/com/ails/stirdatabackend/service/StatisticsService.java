package com.ails.stirdatabackend.service;

import java.sql.Date;
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
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
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
import com.ails.stirdatabackend.repository.CountriesDBRepository;
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
    @Qualifier("default-from-date")
    private Date defaultFromDate;

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private PlacesDBRepository placeRepository;

    @Autowired
    private CountriesDBRepository countriesDBRepository;

    private int batchSize = 500;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

    public void computeStatistics(CountryDB cc, Set<Dimension> stats) {
    	computeStatistics(cc, stats, false);
    }
    
	public void computeStatistics(CountryDB cc, Set<Dimension> stats, boolean force) {
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

    	if (!force) {
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
    	}
    	
    	computeAndSaveAllStatistics(cc, dims);
    	
	}

    private PlaceDB lookupPlace(Map<Code, PlaceDB> placeMap, Code code) {
    	PlaceDB res = placeMap.get(code);
    	if (res == null) {
    		res = placeRepository.findByCode(code);
    		placeMap.put(code, res);
    	}
    	
    	return res;
    }
    
    private void computeAndSaveAllStatistics(CountryDB cc, Set<Dimension> dimensions) {
    	
    	Map<Code, PlaceDB> placeMap = new HashMap<>();
		
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

//    	// groupby
//    	if (cc.isNuts() && dimensions.contains(Dimension.NUTSLAU)) {
//    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode());
//    		
//    		PlaceTree pt = new PlaceTree(nutsService, cc);
//    		
//    		StatisticsHelper sh = new StatisticsHelper(cc, null);
//    		
//    		List<StatisticResult> nuts = new ArrayList<>();
////    		nuts.addAll(groupByPlaceStatistics(cc, false, true, sh, null, null));
//    		nuts.addAll(groupByPlaceStatistics(cc, true, false, sh, null, null));
//    		
////    		statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU);
//
//    		for (StatisticResult sr : nuts) {
//    			Statistic stat = new Statistic();
//    			stat.setCountry(cc.getCode());
//    			stat.setDimension(Dimension.NUTSLAU);
//    			stat.setPlace(sr.getCode().toString());
////    			if (sr.getParentCode() != null) {
////    				stat.setParentPlace(sr.getParentCode().toString());
////    			}
//    			stat.setUpdated(sr.getComputed());
//    			stat.setReferenceDate(cc.getLastUpdated());
//    			stat.setCount(sr.getCount());
//    			
//    			System.out.println(stat.getPlace() + " " + stat.getCount());
////    			statisticsRepository.save(stat);
//    		}
//
//    		
//    		Map<Code, PlaceNode> map = pt.getLeaves();
//    		
//    		for (StatisticResult sr : nuts) {
////    			System.out.println(sr.getCode());
//    			if (map.get(sr.getCode()) != null) {
//    				map.get(sr.getCode()).setCount(sr.getCount());
//    			}
//    		}
//    		
//    		pt.getRoot().sum();
//    		System.out.println(pt.getRoot().toString());
//    		
//    		
//    		
//    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode() + " completed.");
//    	}
    	
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
    		
	   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.FOUNDING, null, null);
	   		sh.minMaxDates(cc, Dimension.FOUNDING, null, null);

	   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
	   		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.FOUNDING, root, null, null, null, null, true, sh);
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
			
	   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.DISSOLUTION, null, null);
	   		sh.minMaxDates(cc, Dimension.DISSOLUTION, null, null);

	   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
    		
    		List<StatisticResult> nuts = dateStatistics(cc, Dimension.DISSOLUTION, root, null, null, null, null, true, sh);
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
			
			logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode());

			for (StatisticResult iter : nuts) {
				
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
	
	    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, nutsLauList, null, null, null, true, sc);
				statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, iter.getCode().toString());
	    		
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < nutsNace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = nutsNace.get(i);

	    			Statistic stat = new Statistic();
	    			stat.setCountry(cc.getCode());
	    			stat.setDimension(Dimension.NUTSLAU_NACE);
	    			stat.setPlace(iter.getCode().toString());
	    			if (iter.getParentCode() != null) {
	    				stat.setParentPlace(iter.getParentCode().toString());
	    			}
	    			stat.setActivity(sr.getCode().toString());
	    			if (sr.getParentCode() != null) {
	    				stat.setParentActivity(sr.getParentCode().toString());
	    			}
	    			stat.setUpdated(sr.getComputed());
	    			stat.setReferenceDate(cc.getLastUpdated());	    			
	    			stat.setCount(sr.getCount());
	    			
	//	    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount() + " " + stat.getUpdated() + " " + stat.getReferenceDate());
//	    			statisticsRepository.save(stat);
	    			list.add(stat);
	    		}
	      		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

			}

			for (List<Code> ilist : new List[] { nuts3, lau }) {
				if (ilist.isEmpty()) {
					continue;
				}
				
				System.out.println("With NUTS3/LAU " + ilist);
				
	    		List<StatisticResult> listNace = statistics(cc, Dimension.NACE, null, ilist, null, null, null, true, sc);
	    		for (Code c : ilist) {
	    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_NACE, c.toString());
	    		}
	    		
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < listNace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = listNace.get(i);
	    			PlaceDB placedb = lookupPlace(placeMap, sr.getGroupByCode());
	    			if (placedb == null) {
	    				continue;
	    			} 

	    			Statistic stat = new Statistic();
	    			stat.setCountry(cc.getCode());
	    			stat.setDimension(Dimension.NUTSLAU_NACE);
	    			stat.setPlace(sr.getGroupByCode().toString());
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
//		    		statisticsRepository.save(stat);
	    			list.add(stat);
	    		}	      		
	      		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

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
			
//			logger.info("Deleting " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode());
//			statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU_FOUNDING);
			
    		logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode());
	   		
    		for (StatisticResult iter : nuts) {
				
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());

		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.FOUNDING, nutsLauList, null);
		   		sh.minMaxDates(cc, Dimension.FOUNDING, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);

				List<StatisticResult> nutsDate = dateStatistics(cc, Dimension.FOUNDING, root, nutsLauList, null, null, null, true, sh);
    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_FOUNDING, iter.getCode().toString());

    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < nutsDate.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = nutsDate.get(i);
	      			
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
	    		}
    			
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);
			}
    		

			for (List<Code> ilist : new List[] { nuts3, lau }) {
				if (ilist.isEmpty()) {
					continue;
				}
				
				System.out.println("With NUTS3/LAU " + ilist);
				
		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.FOUNDING, ilist, null);
		   		sh.minMaxDates(cc, Dimension.FOUNDING, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);

	    		List<StatisticResult> listNace = dateStatistics(cc, Dimension.FOUNDING, root, ilist, null, null, null, true, sh);
	    		
	    		System.out.println("Computed");
	    		for (Code c : ilist) {
	    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_FOUNDING, c.toString());
	    		}
	    		
	    		System.out.println("Saving results " + listNace.size());
	    		
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < listNace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = listNace.get(i);
	    			
	    			PlaceDB placedb = lookupPlace(placeMap, sr.getGroupByCode());
	    			if (placedb == null) {
	    				continue;
	    			}
	    			
//	      			System.out.println("ADDING " +sr.getGroupByCode() + " " + sr.getCode().getDateFrom() + " " + sr.getCode().getDateTo().toString() + " " + Code.previousDateLevel(sr.getCode().getDateInterval()));
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
        			stat.setDimension(Dimension.NUTSLAU_FOUNDING);
        			stat.setPlace(sr.getGroupByCode().toString());
	    			if (placedb.getParent() != null) {
	    				stat.setParentPlace(placedb.getParent().getCode().toString());
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
        		}
	      		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

			}			
			
			logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode() + " completed.");
		}

//	 	THIS IS WITH NO GROUPING
//    	if (cc.isNuts() && cc.isFoundingDate() && dimensions.contains(Dimension.NUTSLAU_FOUNDING)) {
//
//    		List<StatisticResult> nuts = new ArrayList<>(); 
//    		
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
//    		logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode());
//
//			for (StatisticResult iter : nuts) {
//				System.out.println("With NUTS " + iter.getCode());
//				
//				List<Code> nutsLauList = new ArrayList<>();
//				nutsLauList.add(iter.getCode());
//    		
//    			nuts = dateStatistics(cc, Dimension.FOUNDING, null, nutsLauList, null, null, null, true);
//    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_FOUNDING, iter.getCode().toString());
//    			
//        		for (StatisticResult sr : nuts) {
//        			Statistic stat = new Statistic();
//        			stat.setCountry(cc.getCode());
//        			stat.setDimension(Dimension.NUTSLAU_FOUNDING);
//        			stat.setPlace(iter.getCode().toString());
//	    			if (iter.getParentCode() != null) {
//	    				stat.setParentPlace(iter.getParentCode().toString());
//	    			}	        			
//        			stat.setFromDate(sr.getCode().getDateFrom().toString());
//        			stat.setToDate(sr.getCode().getDateTo().toString());
//        			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
//        			if (sr.getParentCode() != null) {
//        				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
//        				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
//        			}
//        			stat.setUpdated(sr.getComputed());
//        			stat.setReferenceDate(cc.getLastUpdated());
//        			stat.setCount(sr.getCount());
//        			
//	        		statisticsRepository.save(stat);
//        		}
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode() + " completed.");
//		}

    	if (cc.isNuts() && cc.isDissolutionDate() && dimensions.contains(Dimension.NUTSLAU_DISSOLUTION)) {

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

//			logger.info("Deleting " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode());
//			statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION);
			
			logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode());

    		for (StatisticResult iter : nuts) {
				
				System.out.println("With NUTS " + iter.getCode());
				
				List<Code> nutsLauList = new ArrayList<>();
				nutsLauList.add(iter.getCode());
				
		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.DISSOLUTION, nutsLauList, null);
		   		sh.minMaxDates(cc, Dimension.DISSOLUTION, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
	
				List<StatisticResult> nutsDate = dateStatistics(cc, Dimension.DISSOLUTION, root, nutsLauList, null, null, null, true, sh);
    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION, iter.getCode().toString());

    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < nutsDate.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = nutsDate.get(i);
	    			
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
	    		}
	      		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

			}
    		
			for (List<Code> ilist : new List[] { nuts3, lau }) {
				if (ilist.isEmpty()) {
					continue;
				}
				
				System.out.println("With NUTS3/LAU " + ilist);
				
		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.DISSOLUTION, ilist, null);
		   		sh.minMaxDates(cc, Dimension.DISSOLUTION, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
		   		
	    		List<StatisticResult> listNace = dateStatistics(cc, Dimension.DISSOLUTION, root, ilist, null, null, null, true, sh);
	    		
	    		System.out.println("Computed");
	    		for (Code c : ilist) {
	    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION, c.toString());
	    		}
	    		System.out.println("Saving results " + listNace.size());
	    		
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < listNace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	        			if (counter % 10000 == 0) {
	        				System.out.print(counter + " ");
	        			}    				    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = listNace.get(i);
	    			
	    			PlaceDB placedb = lookupPlace(placeMap, sr.getGroupByCode());
	    			if (placedb == null) {
	    				continue;
	    			}
	    			
//	      			System.out.println("ADDING " +sr.getGroupByCode() + " " + sr.getCode().getDateFrom() + " " + sr.getCode().getDateTo().toString() + " " + Code.previousDateLevel(sr.getCode().getDateInterval()));
        			Statistic stat = new Statistic();
        			stat.setCountry(cc.getCode());
        			stat.setDimension(Dimension.NUTSLAU_DISSOLUTION);
        			stat.setPlace(sr.getGroupByCode().toString());
	    			if (placedb.getParent() != null) {
	    				stat.setParentPlace(placedb.getParent().getCode().toString());
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
	    		}
	      		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

			}		    		
			
			logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
		}
    	
//    	if (cc.isNuts() && cc.isDissolutionDate() && dimensions.contains(Dimension.NUTSLAU_DISSOLUTION)) {
//
//    		List<StatisticResult> nuts = new ArrayList<>(); 
//    		
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
//    		logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode());
//
//			for (StatisticResult iter : nuts) {
//				System.out.println("With NUTS " + iter.getCode());
//				
//				List<Code> nutsLauList = new ArrayList<>();
//				nutsLauList.add(iter.getCode());
//    		
//    			nuts = dateStatistics(cc, Dimension.DISSOLUTION, null, nutsLauList, null, null, null, true);
//    			statisticsRepository.deleteAllByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION, iter.getCode().toString());
//    			
//        		for (StatisticResult sr : nuts) {
//        			Statistic stat = new Statistic();
//        			stat.setCountry(cc.getCode());
//        			stat.setDimension(Dimension.NUTSLAU_DISSOLUTION);
//        			stat.setPlace(iter.getCode().toString());
//	    			if (iter.getParentCode() != null) {
//	    				stat.setParentPlace(iter.getParentCode().toString());
//	    			}	        			
//        			stat.setFromDate(sr.getCode().getDateFrom().toString());
//        			stat.setToDate(sr.getCode().getDateTo().toString());
//        			stat.setDateInterval(Code.previousDateLevel(sr.getCode().getDateInterval()));
//        			if (sr.getParentCode() != null) {
//        				stat.setParentFromDate(sr.getParentCode().getDateFrom().toString());
//        				stat.setParentToDate(sr.getParentCode().getDateTo().toString());
//        			}
//        			stat.setUpdated(sr.getComputed());
//        			stat.setReferenceDate(cc.getLastUpdated());
//        			stat.setCount(sr.getCount());
//        			
//	        		statisticsRepository.save(stat);
//        		}
//			}
//			
//			logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
//		}
    	
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

		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.FOUNDING, null, naceList);
		   		sh.minMaxDates(cc, Dimension.FOUNDING, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);

    			nace = dateStatistics(cc, Dimension.FOUNDING, root, null, naceList, null, null, true, sh);
    			statisticsRepository.deleteAllByCountryAndDimensionAndActivity(cc.getCode(), Dimension.NACE_FOUNDING, iter.getCode().toString());
    			
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < nace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = nace.get(i);
        			
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
        		}
        		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

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

		   		StatisticsHelper sh = new StatisticsHelper(cc, Dimension.DISSOLUTION, null, naceList);
		   		sh.minMaxDates(cc, Dimension.DISSOLUTION, null, null);
		   		if (!sh.hasMinMaxDate()) {
		   			continue;
		   		}

		   		Code root = Code.createDateCode(defaultFromDate, new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);

    			nace = dateStatistics(cc, Dimension.DISSOLUTION, root, null, naceList, null, null, true, sh);
    			statisticsRepository.deleteAllByCountryAndDimensionAndActivity(cc.getCode(), Dimension.NACE_DISSOLUTION, iter.getCode().toString());
    			
    			int counter = 0;
    			List<Statistic> list = new ArrayList<>();
    			
    			for (int i = 0; i < nace.size(); i++) {
	    			if (i % batchSize == 0) {
	    				counter += list.size();
	    				if (counter % 10000 == 0) {
	    					System.out.print(counter + " ");
	    				}	    				
	    				statisticsRepository.saveAll(list);
	    				list = new ArrayList<>();
	    			}
	    			
	    			StatisticResult sr = nace.get(i);
	    			
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
        			
//	        		statisticsRepository.save(stat);
        			list.add(stat);
        		}
        		
        		counter += list.size();
       			System.out.println(counter + " ");
        		statisticsRepository.saveAll(list);

			}
			
			logger.info("Computing " + Dimension.NACE_DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
		}
    	
		cc.setLastAccessedEnd(new java.util.Date());
		
		countriesDBRepository.save(cc);
		
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
		private PlaceNode pn;
		private Map<Code, Code> parentMap;
		private Set<PlaceNode> leaves;
		
//		private List<Code> lauWithParent;
		private List<Code> nuts3WithParent;
   		
   		public StatisticsCache(CountryDB cc) {
   			super();
   			this.cc = cc;
   			
//   			if (cc.isNace() || cc.isLau()) {
//   				parentMap = new HashMap<>();
//   				pn = nutsService.buildPlaceTree(cc, false);
//   				
//   				List<PlaceNode> nodes = new ArrayList<>();
//   				nodes.add(pn);
//   				for (int i = 0; i < nodes.size(); i++) {
//   					PlaceNode current = nodes.get(i);
//   					if (current.getNext() != null) {
//   						for (PlaceNode node : current.getNext()) {
//   							if (current.getNode() != null) {
//   								parentMap.put(node.getNode().getCode(), current.getNode().getCode());
//   							} else {
//   								parentMap.put(node.getNode().getCode(), null);
//   							}
//   							nodes.add(node);
//   						}
//   					}
//   				}
//   				
////  				for (Map.Entry<Code, Code> entry : parentMap.entrySet()) {
////   					System.out.println(entry.getKey() + " " + entry.getValue());
////   				}
//   			}
   		}
   		
   		public Code getPlaceParent(Code code) {
   			return parentMap.get(code);
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
	    
	    public StatisticsHelper(CountryDB cc, List<Code> naceCodes) {
	    	naceLeafUris = naceService.getLocalNaceLeafUris(cc, naceCodes);
	    }

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
   		        
   		    if (places != null) {
		        if (places.getNuts3() != null) {
	   		        nuts3Roots = places.getNuts3().size() > 0;
	   		        for (Code c : places.getNuts3()) {
	   		        	if (c.getNutsLevel() != 3) {
	   		        		nuts3Roots = false;
	   		        		break;
	   		        	}
	   		        }
		        }
		        
		        if (places.getLau() != null) {
	   		        lauRoots = places.getLau().size() > 0;
	   		        for (Code c : places.getLau()) {
	   		        	if (!c.isLau()) {
	   		        		lauRoots = false;
	   		        		break;
	   		        	}
	   		        }
		        }
   		    } else {
   		    	nuts3Roots = false;
   		    	lauRoots = false;
   		    }
   		}


   		public void minMaxDates(CountryDB cc, Dimension dimension, Code foundingDate, Code dissolutionDate) {
   		    SparqlQuery sparql;
	    	
   	    	Calendar[] minMaxDate = null; 
   	    	
   	    	if (lauUris != null && lauUris.size() > 1000) { // split if too many laus
   	    		for (int i = 0; i < (lauUris.size() / 1000) + 1; i++) {
//   	    			System.out.println(i + " " + (lauUris.size() / 1000));
   	    			List<String> plauUris = new ArrayList<>();
   	    			
   	    			if (i*1000 < lauUris.size()) {
	   	    			for (int j = i*1000; j < Math.min(lauUris.size(), i*1000 + 1000); j++) {
	   	    				plauUris.add(lauUris.get(j));   	    			
	   	    			}
	   	    			
	   	    			if (dimension == Dimension.FOUNDING) {
	   	    				sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsLeafUris, plauUris, naceLeafUris, null, dissolutionDate);
	   	    				minMaxDate = sparql.minMaxFoundingDate(cc); 
	   	    			} else if (dimension == Dimension.DISSOLUTION) {
	   	    				sparql = SparqlQuery.buildCoreQuery(cc, false, false, nutsLeafUris, plauUris, naceLeafUris, foundingDate, null);
	   	    				minMaxDate = sparql.minMaxDissolutionDate(cc);
	   	    			}
	   	    			
	   	    			if (minDate == null || minDate.after(minMaxDate[0])) {
	   	    				minDate = minMaxDate[0];
	   	    			}
	   	    			
	   	    			if (maxDate == null || maxDate.before(minMaxDate[1])) {
	   	    				maxDate = minMaxDate[1];
	   	    			}
   	    			}
   	    			
//   	    			System.out.println(" MIN " + minDate);
//   	    			System.out.println(" MAX " + maxDate);
   	    			
   	    		}

   			
   	    	} else {
   	   			if (dimension == Dimension.FOUNDING) {
   	   				sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsLeafUris, lauUris, naceLeafUris, null, dissolutionDate);
   	   				minMaxDate = sparql.minMaxFoundingDate(cc); 
   	   			} else if (dimension == Dimension.DISSOLUTION) {
   	   				sparql = SparqlQuery.buildCoreQuery(cc, false, false, nutsLeafUris, lauUris, naceLeafUris, foundingDate, null);
   	   				minMaxDate = sparql.minMaxDissolutionDate(cc);
   	   			}   

   	    	}
   			
			minDate = minMaxDate[0];
   			maxDate = minMaxDate[1];
   			
//   			System.out.println(" MIN " + minDate);
//   			System.out.println(" MAX " + maxDate);
   		}
   		
   		public boolean hasMinMaxDate() {
   			return minDate != null && maxDate != null;
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
   			if (sh.nuts3Roots || sh.lauRoots) {
   				Set<Code> used = new HashSet<>();
   				used.add(root);

	    		for (int i = 0; i < res.size(); i++) {
	    			if (used.add(res.get(i).getCode())) {
	    				statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
	    			}
	    		}
   			} else {
	    		for (int i = 0; i < res.size(); i++) {
	    			statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
	    		}
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
   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		sh.minMaxDates(cc, dimension, foundingDate, dissolutionDate);

   		if (root == null) {
   			root = Code.createDateCode(new java.sql.Date(sh.minDate.getTime().getTime()), new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
   		}
   		
   		return dateStatistics(cc, dimension, root, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, allLevels, sh);
   	}
   		
    
   	public List<StatisticResult> dateStatistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels, StatisticsHelper sh) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();

//   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
//   		sh.minMaxDates(cc, dimension, foundingDate, dissolutionDate);
//
//   		if (root == null) {
//   			root = Code.createDateCode(new java.sql.Date(sh.minDate.getTime().getTime()), new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
//   		}
   		
		dateStatistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res);
    		
   		if (allLevels) {
   			if (sh.nuts3Roots || sh.lauRoots) {
   				Set<Code> used = new HashSet<>();
   				used.add(root);

	    		for (int i = 0; i < res.size(); i++) {
	    			if (used.add(res.get(i).getCode())) {
	    				dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
	    			}
	    		}
   			} else {
	    		for (int i = 0; i < res.size(); i++) {
	    			dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
	    		}
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
   	
//   	private static int c = 0;
    private void statistics(CountryDB cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {
//    	System.out.println(">>> " + root + " " + dimension);
	    List<Code> iterCodes = null;
	    
        if (dimension == Dimension.NUTSLAU) {
        	List<PlaceDB> place = nutsService.getNextNutsLauLevelListDb(root == null ? Code.createNutsCode(cc.getCode()) : root);
        	iterCodes = place.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        } else if (dimension == Dimension.NACE) {
        	List<ActivityDB> activities = naceService.getNextNaceLevelListDb(root);
        	iterCodes = activities.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        	
        } 
        
//        System.out.println(">>> >> " + iterCodes);
    	for (Code code : iterCodes) {
//    		System.out.println("CODE " + code + " " + code.isNuts() + " " + code.isLau());
    		System.out.println("> " + code);
    		SparqlQuery sparql = null; 
    		
    		String query = null;
    		
    		if (dimension == Dimension.NUTSLAU) {
	        	if (code.isNuts()) {
	        		
	        		Set<String> nutsUris = new HashSet<>();
	        		nutsUris.addAll(nutsService.getLocalNutsLeafUrisDB(cc, code));
	        		if (sh.nutsLeafUris != null) {
	        			nutsUris.retainAll(sh.nutsLeafUris);
	        		}
	        		
	        		if (nutsUris.isEmpty()) {
	        			continue;
	        		}
	        		
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsUris, sh.lauUris, sh.naceLeafUris, foundingDate, dissolutionDate);
	        		
	        	} else if (code.isLau()) {

	        		Set<String> lauUris  = new HashSet<>();
	        		lauUris.add(nutsService.getLocalLauUri(cc, code));
	        		if (sh.lauRoots) {
	        			lauUris.addAll(sh.lauUris);
	        		}
	        		
	        		if (lauUris.isEmpty()) {
	        			continue;
	        		}
	        		
	        		sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, lauUris, sh.naceLeafUris, foundingDate, dissolutionDate);
	        	}
	        	
	        	query = sparql.countSelectQuery() ;
	        } else if (dimension == Dimension.NACE) {
//	        	List<String> naceLeafUris = (sc == null) ? naceService.getLocalNaceLeafUris(cc, code) : sc.nacelookup(code);
	        	
        		Set<String> naceUris  = new HashSet<>();
        		naceUris.addAll((sc == null) ? naceService.getLocalNaceLeafUris(cc, code) : sc.nacelookup(code));
        		if (sh.naceLeafUris != null) {
        			naceUris.retainAll(sh.naceLeafUris);
        		}
        		
            	if (naceUris.isEmpty()) {
            		continue;
            	}
            	
//            	System.out.println(sh.nuts3Roots + " " + sh.lauRoots);
            	if (sh.nuts3Roots || sh.lauRoots) {
            		sparql = SparqlQuery.buildCoreQueryGroupPlace(cc, true, false, sh.nutsLeafUris, sh.lauUris, naceUris, foundingDate, dissolutionDate);
            		if (sh.nuts3Roots) {
            			query = sparql.countSelectQueryGroupByNuts3();
            		} else {
            			query = sparql.countSelectQueryGroupByLau();
            		}
            	} else {
            		sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, naceUris, foundingDate, dissolutionDate);
            		query = sparql.countSelectQuery() ;
            	}
	        }
	  
//    		String query = sparql.countSelectQuery() ;
	
//	        System.out.println(uri);
//	        System.out.println(cc.getDataEndpoint());
//	        System.out.println(QueryFactory.create(query));
//	        long start = System.currentTimeMillis();
	        
	        int tries = 0;
	        while (tries < 3) {
	        	tries++;
	        	
		        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
	            	ResultSet rs = qe.execSelect();
	            	
	            	while (rs.hasNext()) {
	            		QuerySolution sol = rs.next();
	
	//            		System.out.println(sol);
	            		int count = sol.get("count").asLiteral().getInt();
	            		
	            		if (count > 0) {
	            			if (sh.nuts3Roots) {
	            				String uri = sol.get("nuts3").asResource().toString();
	            			
		            			StatisticResult sr = new StatisticResult();
		            			sr.setCountry(cc.getCode());
		            			sr.setCode(code);
		            			sr.setParentCode(root);
		            			sr.setCount(count);
		            			sr.setComputed(new java.util.Date());
	            				sr.setGroupByCode(nutsService.getNutsCodeFromLocalUri(cc, uri));
		            			res.add(sr);
	            			} else if (sh.lauRoots) {
	            				String uri = sol.get("lau").asResource().toString();
		            			
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
	            		
//	               		System.out.println("\t" + code.getCode() + " " + count + " " +  (System.currentTimeMillis() - start));
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
    
    private List<StatisticResult> groupByPlaceStatistics(CountryDB cc, boolean nuts3, boolean lau, StatisticsHelper sh, Code foundingDate, Code dissolutionDate) {
    	SparqlQuery sparql = SparqlQuery.buildCoreQueryGroupPlace(cc, true, nuts3, lau, sh.naceLeafUris, foundingDate, dissolutionDate);
    	String query = null;
    	if (nuts3) {
    		query = sparql.countSelectQueryGroupByNuts3();
    	} else if (lau) {
            query = sparql.countSelectQueryGroupByLau();
    	}

    	List<StatisticResult> res = new ArrayList<>();
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
    		ResultSet rs = qe.execSelect();
	            	
    		while (rs.hasNext()) {
    			QuerySolution sol = rs.next();
	
//            	System.out.println(sol);
	            int count = sol.get("count").asLiteral().getInt();
	            		
	            if (count > 0) {
            		StatisticResult sr = new StatisticResult();

	            	if (nuts3) {
	            		String uri = sol.get("nuts3").asResource().toString();
	            		sr.setCode(nutsService.getNutsCodeFromLocalUri(cc, uri));
        			} else if (lau) {
        				String uri = sol.get("lau").asResource().toString();
            			sr.setCode(nutsService.getLauCodeFromLocalUri(cc, uri));
            		}
        			sr.setCountry(cc.getCode());
        			sr.setCount(count);
        			sr.setComputed(new java.util.Date());
        			res.add(sr);
	            }
	        }
        }     
    	
    	return res;
    }    
    

    
    public StatisticResult singleStatistic(CountryDB cc, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate) {
        StatisticResult res = null;

   		StatisticsHelper sh = new StatisticsHelper(cc, null, nutsLauCodes, naceCodes);

//		List<String> nutsUris = (nutsLauCode != null && nutsLauCode.isNuts()) ? nutsUris = nutsService.getLocalNutsLeafUrisDB(cc, nutsLauCode) : null ;
//		List<String> lauUris = (nutsLauCode != null && nutsLauCode.isLau()) ? Arrays.asList(nutsService.getLocalLauUri(cc, nutsLauCode)) : null ;    		
//    	List<String> naceUris = naceCode != null ? naceService.getLocalNaceLeafUris(cc, naceCode) : null ;
    	
        String query = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, foundingDate, dissolutionDate).countSelectQuery() ;
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
//    	System.out.println(">> " + root + " " + dimension);
    	
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
//		System.out.println("\t\t\t\t\t" + minDate + " >>> " + maxDate );
		
		if (minDate == null || maxDate == null) {
			return;
		}
		
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
   			String query = null;
   			if (dimension == Dimension.FOUNDING) {
   				if (sh.nuts3Roots || sh.lauRoots) {
            		sparql = SparqlQuery.buildCoreQueryGroupPlace(cc, true, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())), dissolutionDate);
            		if (sh.nuts3Roots) {
            			query = sparql.countSelectQueryGroupByNuts3();
            		} else {
            			query = sparql.countSelectQueryGroupByLau();
            		}
            	} else {
            		sparql = SparqlQuery.buildCoreQuery(cc, true, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())), dissolutionDate);
            		query = sparql.countSelectQuery() ;
            	}
   				
   				
   				 
   			} else if (dimension == Dimension.DISSOLUTION) {
   				if (sh.nuts3Roots || sh.lauRoots) {
            		sparql = SparqlQuery.buildCoreQueryGroupPlace(cc, false, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, foundingDate, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())));
            		if (sh.nuts3Roots) {
            			query = sparql.countSelectQueryGroupByNuts3();
            		} else {
            			query = sparql.countSelectQueryGroupByLau();
            		}
            	} else {
            		sparql = SparqlQuery.buildCoreQuery(cc, false, false, sh.nutsLeafUris, sh.lauUris, sh.naceLeafUris, foundingDate, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())));
            		query = sparql.countSelectQuery() ;
            	}
   			}
   			
	
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
	            			if (sh.nuts3Roots) {
	            				String uri = sol.get("nuts3").asResource().toString();
	            			
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
	            				sr.setGroupByCode(nutsService.getNutsCodeFromLocalUri(cc, uri));
		            			res.add(sr);
	            			} else if (sh.lauRoots) {
	            				String uri = sol.get("lau").asResource().toString();
		            			
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
	            				sr.setGroupByCode(nutsService.getLauCodeFromLocalUri(cc, uri));
		            			res.add(sr);
		            			
	            			} else {
		            			
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
	            			
//		            			System.out.println("\t\t" + Code.createDateCode(new Date(fromDate.getTime().getTime()).toString(), new Date(toDate.getTime().getTime()).toString(), nextInterval));
		            			res.add(sr);
	            			}
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
