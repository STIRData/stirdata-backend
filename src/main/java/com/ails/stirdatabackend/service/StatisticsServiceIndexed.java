package com.ails.stirdatabackend.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.LogActionType;
import com.ails.stirdatabackend.model.LogState;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticResult;
import com.ails.stirdatabackend.model.UpdateLog;
import com.ails.stirdatabackend.model.UpdateLogAction;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.repository.UpdateLogRepository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Service
public class StatisticsServiceIndexed {
	
	private final static Logger logger = LoggerFactory.getLogger(StatisticsServiceIndexed.class);
	
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;

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
    private CountriesDBRepository countriesDBRepository;

    @Autowired
    private UpdateLogRepository updateLogRepository;
    
    private int batchSize = 500;
    
    @Autowired
    @Qualifier("elastic-client") 
    private ElasticsearchClient elasticClient;
    
    public void computeStatistics(CountryDB cc, Collection<Dimension> stats) {
		UpdateLog log = new UpdateLog();
		
		log.setType(LogActionType.COMPUTE_STATISTICS);
		log.setDcat(cc.getDcat());

		updateLogRepository.save(log);
		
    	computeStatistics(cc, stats, false, log);
    	
		if (log.getState() == LogState.RUNNING) {
			log.completed();
			updateLogRepository.save(log);
		}
    }
    
	public void computeStatistics(CountryDB cc, Collection<Dimension> stats, boolean force, UpdateLog log) {
		Set<Dimension> dims = new LinkedHashSet<>();
		
		if (stats.contains(Dimension.DATA)) {
			dims.add(Dimension.DATA);
		}
    	
    	if (cc.isNuts()) {
    		if (stats.contains(Dimension.NUTSLAU)) {
    			dims.add(Dimension.NUTSLAU);
    		}

        	if (cc.isNace() && stats.contains(Dimension.NUTSLAU_NACE)) {
        		dims.add(Dimension.NUTSLAU);
        		dims.add(Dimension.NUTSLAU_NACE);
        	}
        	if (cc.isFoundingDate() && stats.contains(Dimension.NUTSLAU_FOUNDING)) {
        		dims.add(Dimension.NUTSLAU);
            	dims.add(Dimension.NUTSLAU_FOUNDING);
        	}
        	if (cc.isDissolutionDate() && stats.contains(Dimension.NUTSLAU_DISSOLUTION)) {
        		dims.add(Dimension.NUTSLAU);
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
    		
    		boolean nutsLauComputed = false;
    		boolean computeNutsLauX = false;
    		
			for (Statistic s : statisticsRepository.groupCountDimensionsByCountry(cc.getCode()) ) {
				Dimension d = s.getDimension(); 	
				
				Calendar ccDate = Calendar.getInstance();
				java.util.Date sqlDate = cc.getStatsDate(d);
				
				if (sqlDate != null) {
					ccDate.setTimeInMillis(sqlDate.getTime());
//					System.out.println(d);
//					System.out.println(ccDate.getTime());
//					System.out.println(cc.getLastUpdated());
//					System.out.println(ccDate.getTime().equals(cc.getLastUpdated()));
						
					if (ccDate.getTime().equals(cc.getLastUpdated())) {
						if (d == Dimension.NUTSLAU) {
							nutsLauComputed = true;
						} else {
							dims.remove(d);
							logger.info("Statistics for " + cc.getCode() + " / " + d + " already computed.");
						}
					} else {
						if (d == Dimension.NUTSLAU_DISSOLUTION || d == Dimension.NUTSLAU_FOUNDING || d == Dimension.NUTSLAU_NACE) {
							computeNutsLauX = true;
						}
					}
				}
			}
			
			if (nutsLauComputed && !computeNutsLauX) {
				dims.remove(Dimension.NUTSLAU);
			}
    	}
    	
    	computeAndSaveAllStatistics(cc, dims, log);
    	
	}

    private Statistic lookupPlace(CountryDB cc, Map<Code, Statistic> placeMap, Code code) {
//    	PlaceDB res = placeMap.get(code);
//    	if (res == null) {
//    		res = placeRepository.findByCode(code);
//    		placeMap.put(code, res);
//    	}
//    	
//    	return res;
    	
    	Statistic res = placeMap.get(code);
    	if (res == null) {
    		List<Statistic> list = statisticsRepository.findByCountryAndDimensionAndPlace(cc.getCode(), Dimension.NUTSLAU, code.toString());
    		if (list.size() > 1) {
    			logger.error("There are more than one entries for place " + code.getCode() + " in NUTSLAU statistics.");
    		} 
    		
    		if (list.size() > 0) {
    			res = list.get(0);
    			placeMap.put(code, res);
    		}
    		
    	}
    	
    	return res;
    	
    }
    
    private void computeAndSaveAllStatistics(CountryDB cc, Set<Dimension> dimensions, UpdateLog log) {
    	
//    	Map<Code, PlaceDB> placeMap = new HashMap<>();
    	Map<Code, Statistic> placeMap = new HashMap<>();
		
//    	logger.info("Statistics to compute " + dimensions);
    	
    	StatisticsCache sc = new StatisticsCache(cc);
    	
    	UpdateLogAction action = null;
    	
    	if (dimensions.contains(Dimension.DATA)) {
    		
	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_DATA_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }

	        logger.info("Computing " + Dimension.DATA + " statistics for " + cc.getCode());

    		try {
    			ElasticQuery elastic = ElasticQuery.buildCoreQuery(cc, true, null, null, null, null, null, null, null);
		    	statisticsRepository.deleteAllByCountryAndDimension(cc.getCode(), Dimension.DATA);
		    	
		   		if (elastic != null) {
			    	CountRequest.Builder searchRequest = new CountRequest.Builder();
			    	searchRequest.index(cc.getIndexName());
			    	searchRequest.query(elastic.getQuery().build()._toQuery());

					long count = elasticClient.count(searchRequest.build()).count();
	        		if (count > 0) {
	        			Statistic stat = new Statistic();
	        			stat.setCountry(cc.getCode());
	        			stat.setDimension(Dimension.DATA);
	        			stat.setUpdated(new java.util.Date());
	        			stat.setReferenceDate(cc.getLastUpdated());
	        			stat.setCount((int)count);
	        			
//	        			System.out.println(stat.getCountry() + " " + stat.getCount());
		        		statisticsRepository.save(stat);
	        		}
		   		}	
		    	
		    	logger.info("Computing " + Dimension.DATA + " statistics for " + cc.getCode() + " completed.");
		        
		    	if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}
	    	}		    	
    	}

    	if (cc.isNuts() && dimensions.contains(Dimension.NUTSLAU)) {
    		
	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NUTS_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode());
	    		
	    		List<StatisticResult> nuts = statistics(cc, Dimension.NUTSLAU, null, null, false, null, null, null, true, sc);
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
	    			
//	    			System.out.println(stat.getPlace() + " " + stat.getCount());
	    			statisticsRepository.save(stat);
	    		}
	    		
	    		logger.info("Computing " + Dimension.NUTSLAU + " statistics for " + cc.getCode() + " completed (" + nuts.size() + ").");
	        
	    		if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}
	    	}
    	}
    	
    	
		if (cc.isNace() && dimensions.contains(Dimension.NACE)) {
    		
	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NACE_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
	    		logger.info("Computing " + Dimension.NACE + " statistics for " + cc.getCode());
	    		
	    		List<StatisticResult> nace = statistics(cc, Dimension.NACE, null, null, false, null, null, null, true, sc);
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
					
//					System.out.println(stat.getActivity() + " " + stat.getCount());
					statisticsRepository.save(stat);
				}
				
				logger.info("Computing " + Dimension.NACE + " statistics for " + cc.getCode() + " completed.");
		    	
				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
		}
		
		if (cc.isFoundingDate() && dimensions.contains(Dimension.FOUNDING)) {
    		
	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_FOUNDING_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {

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
	    			
//	    			System.out.println(stat.getFromDate() + " " + stat.getToDate() + " " + stat.getDateInterval() + " " + stat.getCount());
	    			statisticsRepository.save(stat);
	    		}
	    		
	    		logger.info("Computing " + Dimension.FOUNDING + " statistics for " + cc.getCode() + " completed.");

		    	if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	    		
	    	}
		}
		
		if (cc.isDissolutionDate() && dimensions.contains(Dimension.DISSOLUTION)) {
    		
	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_DISSOLUTION_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {

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
	    			
//	    			System.out.println(stat.getFromDate() + " " + stat.getToDate() + " " + stat.getDateInterval() + " " + stat.getCount());
	    			statisticsRepository.save(stat);
	    		}
	    		
	    		logger.info("Computing " + Dimension.DISSOLUTION + " statistics for " + cc.getCode() + " completed.");
	    		
		    	if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	    		
	    	}	    		
		}

    	if (cc.isNuts() && cc.isNace() && dimensions.contains(Dimension.NUTSLAU_NACE)) {

	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NUTS_NACE_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
	    		List<StatisticResult> nuts = new ArrayList<>();
	    		
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
					nuts.add(sr);
				}
				
				logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode());
	
				for (StatisticResult iter : nuts) {
					
					System.out.println("With NUTS " + iter.getCode());
					
		    		List<StatisticResult> nutsNace = statistics(cc, Dimension.NACE, null, Arrays.asList(new Code[] { iter.getCode() }), false, null, null, null, true, sc);
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
		    			
//			    		System.out.println(stat.getPlace() + " " + stat.getActivity()  + " " + stat.getCount());
		    			list.add(stat);
		    		}
		      		
	        		counter += list.size();
	       			System.out.println(counter + " ");
	        		statisticsRepository.saveAll(list);
	
				}
				
				logger.info("Computing " + Dimension.NUTSLAU_NACE + " statistics for " + cc.getCode() + " completed.");
				
				statisticsRepository.deleteAllByCountryAndDimensionAndNotReferenceDate(cc.getCode(), Dimension.NUTSLAU_NACE, cc.getLastUpdated());

				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
				
   		}
    	
    	
    	if (cc.isNuts() && cc.isFoundingDate() && dimensions.contains(Dimension.NUTSLAU_FOUNDING)) {

	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NUTS_FOUNDING_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
	    		List<StatisticResult> nuts = new ArrayList<>();

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
					
					nuts.add(sr);
				}
				
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
	        			
	        			list.add(stat);
		    		}
	    			
	        		counter += list.size();
	       			System.out.println(counter + " ");
	        		statisticsRepository.saveAll(list);
				}

				logger.info("Computing " + Dimension.NUTSLAU_FOUNDING + " statistics for " + cc.getCode() + " completed. " + cc.getLastUpdated());

				statisticsRepository.deleteAllByCountryAndDimensionAndNotReferenceDate(cc.getCode(), Dimension.NUTSLAU_FOUNDING, cc.getLastUpdated());

				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
				
		}


    	if (cc.isNuts() && cc.isDissolutionDate() && dimensions.contains(Dimension.NUTSLAU_DISSOLUTION)) {

	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NUTS_DISSOLUTION_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
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
					nuts.add(sr);
				}
	
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
	        			
	        			list.add(stat);
		    		}
		      		
	        		counter += list.size();
	       			System.out.println(counter + " ");
	        		statisticsRepository.saveAll(list);
	
				}
				
				logger.info("Computing " + Dimension.NUTSLAU_DISSOLUTION + " statistics for " + cc.getCode() + " completed. " + cc.getLastUpdated());

				statisticsRepository.deleteAllByCountryAndDimensionAndNotReferenceDate(cc.getCode(), Dimension.NUTSLAU_DISSOLUTION, cc.getLastUpdated());

				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
				
		}
    	
    	if (cc.isNace() && cc.isFoundingDate() && dimensions.contains(Dimension.NACE_FOUNDING)) {

	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NACE_FOUNDING_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
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
	        			
	        			list.add(stat);
	        		}
	        		
	        		counter += list.size();
	       			System.out.println(counter + " ");
	        		statisticsRepository.saveAll(list);
	
				}
				
				statisticsRepository.deleteAllByCountryAndDimensionAndNotReferenceDate(cc.getCode(), Dimension.NACE_FOUNDING, cc.getLastUpdated());

				logger.info("Computing " + Dimension.NACE_FOUNDING + " statistics for " + cc.getCode() + " completed. " + cc.getLastUpdated());
				
				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
				
		}
    	
    	if (cc.isNace() && cc.isDissolutionDate() && dimensions.contains(Dimension.NACE_DISSOLUTION)) {

	        if (log != null) {
	        	action = new UpdateLogAction(LogActionType.COMPUTE_NACE_DISSOLUTION_STATISTICS);
	        	log.addAction(action);
	        	updateLogRepository.save(log);
	        }
	        
	        try {
	
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
	        			
	        			list.add(stat);
	        		}
	        		
	        		counter += list.size();
	       			System.out.println(counter + " ");
	        		statisticsRepository.saveAll(list);
	
				}
				
				statisticsRepository.deleteAllByCountryAndDimensionAndNotReferenceDate(cc.getCode(), Dimension.NACE_DISSOLUTION, cc.getLastUpdated());

				logger.info("Computing " + Dimension.NACE_DISSOLUTION + " statistics for " + cc.getCode() + " completed. " + cc.getLastUpdated());
				
				if (log != null) {
		        	action.completed();
		        	updateLogRepository.save(log);
		        }
		        
	    	} catch (Exception ex) {
    			ex.printStackTrace();
	    		if (log != null) {
		    		action.failed(ex.getMessage());
//		    		log.failed();
		    		updateLogRepository.save(log);
	    		}	
	    	}
				
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
		
//		private CountryDB cc;
//		private PlaceNode pn;
//		private Map<Code, Code> parentMap;
//		private Set<PlaceNode> leaves;
//		
////		private List<Code> lauWithParent;
//		private List<Code> nuts3WithParent;
   		
   		public StatisticsCache(CountryDB cc) {
   			super();
//   			this.cc = cc;
   		}
   		
//   		public Code getPlaceParent(Code code) {
//   			return parentMap.get(code);
//   		}
//   		
//   		public List<String> nacelookup(Code naceCode) {
//
//   			List<String> leaves = get(naceCode);
//   			if (leaves == null) {
//   				leaves = naceService.getLocalNaceLeafUris(cc,  naceCode);
//   				put(naceCode, leaves);
//   			}
//   			
//   			return leaves;
//   		}
   	}
   	
   	
   	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
   	
   	private class StatisticsHelper {
   		public List<Code> nutsLauCodes;
   		public List<Code> naceCodes;
   		public Set<Code> statNutsLauCodes;

	    Calendar minDate;
	    Calendar maxDate;
	    
   		public StatisticsHelper(CountryDB cc, Dimension dimension, List<Code> nutsLauCodes, List<Code> naceCodes) {
   			this.naceCodes = naceCodes;
   			
    		if (nutsLauCodes != null) {

    			List<List<Code>> allStatCodes = new ArrayList<>();
    			List<Integer> allStatLevels = new ArrayList<>();
    			boolean emptyStat = false;
    			int maxStatLevel = -1;
    			
	    		for (Code code : nutsLauCodes) { 
	    			
	    			if (!code.isStat()) {
	    				if (this.nutsLauCodes == null) {
	    					this.nutsLauCodes = new ArrayList<>();
	    				}
	    				this.nutsLauCodes.add(code);
	    			} else {
	    				List<Code> cs = nutsService.getNutsCodesFromStat(code, cc);
	    				allStatCodes.add(cs);
	    				
	    				if (!cs.isEmpty()) {
	    					int level = cs.get(0).getNutsLevel();
    						allStatLevels.add(level);
    						maxStatLevel = Math.max(maxStatLevel, level);
	    				} else {
	    					allStatLevels.add(-1);
	    					emptyStat = true;
	    				}
	    			}
	    		}
	    		
	    		if (!allStatCodes.isEmpty()) {
	    			if (emptyStat) {
	    				statNutsLauCodes = new HashSet<>();
	    			} else {
	    				for (int i = 0; i < allStatCodes.size(); i++) {
	    					List<Code> currentStatCodes = allStatCodes.get(i);
	    					int currentLevel = allStatLevels.get(i);

	    					List<Code> actualStatCodes;
	    					
	    					if (currentLevel < maxStatLevel) {
	    						actualStatCodes = new ArrayList<>();
	    						for (Code c : currentStatCodes) {
	    							for (PlaceDB ch : nutsService.getChildren(nutsService.getByCode(c), maxStatLevel)) {
	    								actualStatCodes.add(ch.getCode());
	    							}
	    						}	
	    					} else {
	    						actualStatCodes = currentStatCodes;
	    					}
	    					
	    		    		if (statNutsLauCodes == null) {
	    		    			statNutsLauCodes = new HashSet<>();
	    		    			statNutsLauCodes.addAll(actualStatCodes);
	    		    		} else {
	    		    			statNutsLauCodes.retainAll(actualStatCodes);
	    					}
	    				}
		    			
	    			}
	    		}
	    	}
   		}

   		public void minMaxDates(CountryDB cc, Dimension dimension, Code foundingDate, Code dissolutionDate) {
   		    ElasticQuery elastic = null;
	    	
   		    SearchRequest.Builder searchRequest = new SearchRequest.Builder();
	    	searchRequest.index(cc.getIndexName());
	    	
   			if (dimension == Dimension.FOUNDING) {
   				
   				elastic = ElasticQuery.buildCoreQuery(cc, true, null, nutsLauCodes, statNutsLauCodes, null, naceCodes, null, dissolutionDate);

   				searchRequest.query(elastic.getQuery().build()._toQuery())
   					.aggregations("min-date", a -> a.min(v -> v.field("founding-date")))
   					.aggregations("max-date", a -> a.max(v -> v.field("founding-date")));
   				
//    	    	searchRequest.source(new SearchSourceBuilder()
//    	    			.query(elastic.getQuery())
//    	    			.aggregation(AggregationBuilders.min("min-date").field("founding-date"))
//    	    			.aggregation(AggregationBuilders.max("max-date").field("founding-date")));

   			} else if (dimension == Dimension.DISSOLUTION) {
   				elastic = ElasticQuery.buildCoreQuery(cc, true, null, nutsLauCodes, statNutsLauCodes, null, naceCodes, foundingDate, null);

//    	    	searchRequest.source(new SearchSourceBuilder()
//    	    			.query(elastic.getQuery())
//    	    			.aggregation(AggregationBuilders.min("min-date").field("dissolution-date"))
//    	    			.aggregation(AggregationBuilders.max("max-date").field("dissolution-date")));
   				
   				searchRequest.query(elastic.getQuery().build()._toQuery())
					.aggregations("min-date", a -> a.min(v -> v.field("dissolution-date")))
					.aggregations("max-date", a -> a.max(v -> v.field("dissolution-date")));


   			}   

   	    	try {
   	    			
// 	   	    	System.out.println(elastic.getQuery());

//   	   		Aggregations aggs = client.search(searchRequest, RequestOptions.DEFAULT).getAggregations();
   				Map<String, Aggregate> aggs = elasticClient.search(searchRequest.build(), Object.class).aggregations();
   	    			
//   	   			System.out.println(((Min)aggs.get("min-date")).getValueAsString());
//   	   			System.out.println(((Max)aggs.get("max-date")).getValueAsString());
   	   		
   	   			try {
	   	   			minDate = Calendar.getInstance();
//	   	   			minDate.setTime(sdf.parse(((Min)aggs.get("min-date")).getValueAsString()));
	   	   			minDate.setTime(sdf.parse(aggs.get("min-date").min().valueAsString()));
   				} catch (Exception ex) {
//   					ex.printStackTrace();
   		 			minDate = null;
   		 		}

   	   			try {
	   	   			maxDate = Calendar.getInstance();
//	   	   			maxDate.setTime(sdf.parse(((Max)aggs.get("max-date")).getValueAsString()));
	   	   			maxDate.setTime(sdf.parse(aggs.get("max-date").max().valueAsString()));
   				} catch (Exception ex) {
//   					ex.printStackTrace();
   		 			maxDate = null;
   		 		}

			} catch (Exception ex) {
	 			ex.printStackTrace();
	 		}
   			
//   			System.out.println(" MIN " + minDate);
//   			System.out.println(" MAX " + maxDate);
   		}
   		
   		public boolean hasMinMaxDate() {
   			return minDate != null && maxDate != null;
   		}
   	}

   	public List<StatisticResult> statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, boolean includeNutsLau, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		return statistics(cc, dimension, root, nutsLauCodes, includeNutsLau, naceCodes, foundingDate, dissolutionDate, allLevels, null);
   	}
   	
   	public List<StatisticResult> statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, boolean includeNutsLau, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels, StatisticsCache sc) {
   		
//   		System.out.println(">>>>>" + nutsLauCodes);
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();
   		
   		if (!hasMore(dimension, root)) {
	   		return res; 
   		}

   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		
		statistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res, sc);
		
   		if (allLevels) {
//   			if (sh.nuts3Roots || sh.lauRoots) {
   				Set<Code> used = new HashSet<>();
   				used.add(root);

	    		for (int i = 0; i < res.size(); i++) {
	    			if (used.add(res.get(i).getCode())) {
	    				statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
	    			}
	    		}
//   			} else {
//	    		for (int i = 0; i < res.size(); i++) {
//	    			statistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res, sc);
//	    		}
//   			}
		}
		   		
   		return res;
   	}
   	
   	public List<StatisticResult> dateStatistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, boolean includeNutsLau, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels) {
   		StatisticsHelper sh = new StatisticsHelper(cc, dimension, nutsLauCodes, naceCodes);
   		sh.minMaxDates(cc, dimension, foundingDate, dissolutionDate);

   		if (root == null) {
   			root = Code.createDateCode(new java.sql.Date(sh.minDate.getTime().getTime()), new java.sql.Date(sh.maxDate.getTime().getTime()), Code.date10Y);
   		}
   		
//   		System.out.println(root);
   		return dateStatistics(cc, dimension, root, nutsLauCodes, naceCodes, foundingDate, dissolutionDate, allLevels, sh);
   	}
   		
    
   	public List<StatisticResult> dateStatistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, boolean allLevels, StatisticsHelper sh) {
   		List<StatisticResult> res  = new ArrayList<StatisticResult>();

		dateStatistics(cc, dimension, root, sh, foundingDate, dissolutionDate, res);
    		
   		if (allLevels) {
//   			if (sh.nuts3Roots || sh.lauRoots) {
   				Set<Code> used = new HashSet<>();
   				used.add(root);

	    		for (int i = 0; i < res.size(); i++) {
	    			if (used.add(res.get(i).getCode())) {
	    				dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
	    			}
	    		}
//   			} else {
//	    		for (int i = 0; i < res.size(); i++) {
//	    			dateStatistics(cc, dimension, res.get(i).getCode(), sh, foundingDate, dissolutionDate, res);
//	    		}
//   			}
		}
   		
   		return res;
   	}
   	
//   	public void statistics(CountryDB cc, Dimension dimension, Code root, List<Code> nutsLauCodes, boolean includeNutsLau, List<Code> naceCodes, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {
//
//   		if (!hasMore(dimension, root)) {
//   			return;
//   		}
//   		
//	   	statistics(cc, dimension, root, new StatisticsHelper(cc, dimension, nutsLauCodes, includeNutsLau, naceCodes), foundingDate, dissolutionDate, res, sc);
//   	}
   	
//   	private static int c = 0;
    private void statistics(CountryDB cc, Dimension dimension, Code root, StatisticsHelper sh, Code foundingDate, Code dissolutionDate, List<StatisticResult> res, StatisticsCache sc) {
//    	System.out.println(">>> ELASTIC MS " + root + " " + dimension);
	    List<Code> iterCodes = null;
	    
        if (dimension == Dimension.NUTSLAU) {
        	List<PlaceDB> place = nutsService.getNextNutsLauLevelListDb(root == null ? Code.createNutsCode(cc.getCode()) : root, cc.isLau());
        	iterCodes = place.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        } else if (dimension == Dimension.NACE) {
        	List<ActivityDB> activities = naceService.getNextNaceLevelListDb(root);
        	iterCodes = activities.stream().map(item -> item.getCode()).collect(Collectors.toList());;
        } 
        
//        System.out.println(">>> >> " + iterCodes);
    	for (Code code : iterCodes) {
//    		System.out.println("CODE " + code + " " + code.isNuts() + " " + code.isLau());
//    		System.out.println("> " + code);
    		ElasticQuery elastic = null; 
    		
    		if (dimension == Dimension.NUTSLAU) {
        		elastic = ElasticQuery.buildCoreQuery(cc, true, code, sh.nutsLauCodes, sh.statNutsLauCodes, null, sh.naceCodes, foundingDate, dissolutionDate);
	        } else if (dimension == Dimension.NACE) {
           		elastic = ElasticQuery.buildCoreQuery(cc, true, null, sh.nutsLauCodes, sh.statNutsLauCodes, code, sh.naceCodes, foundingDate, dissolutionDate);
	        }
	  
//	        System.out.println(uri);
//    		System.out.println(elastic);
//	        long start = System.currentTimeMillis();
	        
    		if (elastic == null) {
       			continue;
       		}
    		
    		try {
    			
    	    	CountRequest.Builder searchRequest = new CountRequest.Builder();
    	    	searchRequest.index(cc.getIndexName());
    	    	searchRequest.query(elastic.getQuery().build()._toQuery());

    			long count = elasticClient.count(searchRequest.build()).count();

    			if (count > 0) {
	    			StatisticResult sr = new StatisticResult();
	    			sr.setCountry(cc.getCode());
	    			sr.setCode(code);
	    			sr.setParentCode(root);
	    			sr.setCount((int)count);
	    			sr.setComputed(new java.util.Date());
	        		
	    			res.add(sr);
    			}
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}

        }        	        
    }
    

    
    public StatisticResult singleStatistic(CountryDB cc, List<Code> nutsLauCodes,  List<Code> naceCodes, Code foundingDate, Code dissolutionDate) {

    	StatisticsHelper sh = new StatisticsHelper(cc, null, nutsLauCodes, naceCodes);

   		ElasticQuery elastic = ElasticQuery.buildCoreQuery(cc, true, null, sh.nutsLauCodes, sh.statNutsLauCodes, null, sh.naceCodes, foundingDate, dissolutionDate) ;
        
   		if (elastic == null) {
			StatisticResult sr = new StatisticResult();
			sr.setCountry(cc.getCode());
			sr.setCount(0);
			sr.setComputed(new java.util.Date());
			return sr;
   		}
   		
   		try {
	    	CountRequest.Builder searchRequest = new CountRequest.Builder();
	    	searchRequest.index(cc.getIndexName());
	    	searchRequest.query(elastic.getQuery().build()._toQuery());

			long count = elasticClient.count(searchRequest.build()).count();

				
//    		if (count > 0) {
    			StatisticResult sr = new StatisticResult();
    			sr.setCountry(cc.getCode());
    			sr.setCount((int)count);
    			sr.setComputed(new java.util.Date());
    			return sr;
//    		}
    		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
   		
   		return null;
    }
    
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
   		
//		System.out.println(minDate.getTime() + " >>> " + maxDate.getTime() );
   		
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
   			
   			ElasticQuery elastic = null;

   			if (dimension == Dimension.FOUNDING) {
   				elastic = ElasticQuery.buildCoreQuery(cc, true, null, sh.nutsLauCodes, sh.statNutsLauCodes, null, sh.naceCodes, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())), dissolutionDate);
   			} else if (dimension == Dimension.DISSOLUTION) {
   				elastic = ElasticQuery.buildCoreQuery(cc, false, null, sh.nutsLauCodes, sh.statNutsLauCodes, null, sh.naceCodes, foundingDate, Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()), new java.sql.Date(toDate.getTime().getTime())));
   			}
   			
    		if (elastic == null) {
       			continue;
       		}
   			
//	        System.out.println(dateFormat.format(startDate.getTime()) + " - " + dateFormat.format(endDate.getTime()) + " " + resInterval);
//	        System.out.println(query);
	
    		try {
    			
    	    	CountRequest.Builder searchRequest = new CountRequest.Builder();
    	    	searchRequest.index(cc.getIndexName());
    	    	searchRequest.query(elastic.getQuery().build()._toQuery());

    			long count = elasticClient.count(searchRequest.build()).count();
    				
//    			System.out.println(count);
    			
        		if (count > 0) {
        			StatisticResult sr = new StatisticResult();
        			sr.setCountry(cc.getCode());
        			if (toDate.after(sh.maxDate)) {
        				sr.setCode(Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()).toString(), new java.sql.Date(sh.maxDate.getTime().getTime()).toString(), nextInterval));
        			} else {
        				sr.setCode(Code.createDateCode(new java.sql.Date(fromDate.getTime().getTime()).toString(), new java.sql.Date(toDate.getTime().getTime()).toString(), nextInterval));
        			}
        			sr.setCount((int)count);
        			if (!root.getDateInterval().equals(Code.date10Y)) {
        				sr.setParentCode(root);
        			}
        			sr.setComputed(new java.util.Date());

        			res.add(sr);
        		}
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
    		
            
            fromDate = toDate;
            fromDate.add(Calendar.DAY_OF_MONTH, 1);
   		}

    }        
    

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
