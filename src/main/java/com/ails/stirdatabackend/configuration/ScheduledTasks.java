package com.ails.stirdatabackend.configuration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.LogActionType;
import com.ails.stirdatabackend.model.UpdateLog;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.UpdateLogRepository;
import com.ails.stirdatabackend.service.CountriesService;
import com.ails.stirdatabackend.service.DataStoring;
import com.ails.stirdatabackend.service.StatisticsServiceIndexed;

@Component
@Profile("PRODUCTION")
public class ScheduledTasks {

	Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	
    @Autowired
    private CountriesService countriesService;

    @Autowired
    private CountriesDBRepository countriesDBRepository;

    @Autowired
    private StatisticsServiceIndexed statisticsService;

    @Autowired
    private DataStoring ds;
    
    @Autowired
    private UpdateLogRepository updateLogRepository;
    
	@Value("${endpoint.data.EU}")
	private String dataEuropaSparql;
	
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;	
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, CountryDB> updatedCountries = new LinkedHashMap<>();
    
    private ReadWriteLock updateCCLock = new ReentrantReadWriteLock();
    
	@Autowired
	ApplicationContext context;
    
	@Scheduled(fixedRate = 604800000) // 7 days
////    @Scheduled(fixedRate = 60000)
	public void updateCountries() {
		logger.info("Running scheduled update countries task");
	
		checkDataPortal();
		
		CountryConfigurationsBean ccb = (CountryConfigurationsBean)context.getBean("country-configurations");
		
		for (CountryDB cc : countriesDBRepository.findAll()) {
			
			Country country = new Country();
			country.setDcat(cc.getDcat());
			
			CountryDB updatedcc = countriesService.updateCountry(country);
//			System.out.println(">>>> " + cc.getLastUpdated());
			
			if (updatedcc != null) { // country responds
				if (updatedcc.modified) { // country new or modified
					Lock writeLock = lock.writeLock();
					writeLock.lock();
					updatedCountries.put(updatedcc.getCode(), updatedcc); // schedule for statistics computation
					writeLock.unlock();

					Lock ccLock = updateCCLock.writeLock();
					ccLock.lock();

					if (updatedcc.getActiveLegalEntityCount() != null) { // old statistics exist -> activate country
						ccb.put(cc.getCode(), updatedcc);
					} else { // no statistics exist
						ccb.remove(cc.getCode());
					}
					
					ccLock.unlock();

				}
			
			} else { // country is not responding
				logger.info("Suspending country " + cc.getCode());

				Lock writeLock = lock.writeLock();
				writeLock.lock();
				updatedCountries.remove(cc.getCode());
				writeLock.unlock();
				
				Lock ccLock = updateCCLock.writeLock();
				ccLock.lock();
				ccb.remove(cc.getCode());
				ccLock.unlock();
			}
		}
		
	}
	
	@Scheduled(fixedRate = 604800000) // 7 days
//    @Scheduled(fixedRate = 40000)
	public void updateStatistics() {
		logger.info("Running scheduled update statistics task. Pending countries: " + updatedCountries);
	
		while (updatedCountries.size() > 0) { 
			
			Lock writeLock = lock.writeLock();
			writeLock.lock();
			Map.Entry<String, CountryDB> ccEntry = updatedCountries.entrySet().iterator().next();
			updatedCountries.remove(ccEntry.getKey());
			writeLock.unlock();
	
			CountryDB cc = ccEntry.getValue();
			if (cc != null) {
				Set<Dimension> dimensions = new LinkedHashSet<>();
				dimensions.add(Dimension.DATA); // should always be included
				dimensions.add(Dimension.NACE);
				dimensions.add(Dimension.NUTSLAU);
				dimensions.add(Dimension.FOUNDING);
				dimensions.add(Dimension.DISSOLUTION);
				dimensions.add(Dimension.NUTSLAU_NACE);
//				dimensions.add(Dimension.NUTSLAU_FOUNDING);
//				dimensions.add(Dimension.NUTSLAU_DISSOLUTION);
//				dimensions.add(Dimension.NACE_FOUNDING);
//				dimensions.add(Dimension.NACE_DISSOLUTION);
				
				statisticsService.computeStatistics(cc, dimensions); 
				
				ds.copyStatisticsFromMongoToRDBMS(cc);
				
				Lock ccLock = updateCCLock.writeLock();
				ccLock.lock();
				
				CountryConfigurationsBean ccb = (CountryConfigurationsBean)context.getBean("country-configurations");
				ccb.put(cc.getCode(), cc);
				
				ccLock.unlock();
			}
		}
	}
	
	private void checkDataPortal() {
		logger.info("Checking " + dataEuropaSparql + " for new countries.");
		UpdateLog log = new UpdateLog();
		
		log.setType(LogActionType.CHECK_FOR_NEW_COUNTRIES);
		
		updateLogRepository.save(log);
		
		String sparql =  
			"SELECT ?dataset WHERE { " +
			"  ?dataset a <http://www.w3.org/ns/dcat#Dataset> ; " +
		    "  <http://purl.org/dc/terms/conformsTo <https://stirdata.github.io/data-specification/> . } ";
		
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(dataEuropaSparql, QueryFactory.create(sparql, Syntax.syntaxARQ))) {
        	ResultSet rs = qe.execSelect();
        			
        	while (rs.hasNext()) {
        		String dcat = rs.next().get("dataset").toString();
        		
        		CountryDB cc = countriesDBRepository.findByDcat(dcat);
        		if (cc == null) {
        			cc = new CountryDB();
        			cc.setDcat(dcat);
        			
        			countriesDBRepository.save(cc);
        		}
        	}
        	
			log.completed();
			updateLogRepository.save(log);
        	
        } catch (Exception ex) {
        	log.failed();
        	updateLogRepository.save(log);
        }
	}

}
