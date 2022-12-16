package com.ails.stirdatabackend.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.service.CountriesService;
import com.ails.stirdatabackend.service.DataStoring;
import com.ails.stirdatabackend.service.StatisticsService;

@Component
public class ScheduledTasks {

	Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	
    @Autowired
    private CountriesService countriesService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DataStoring ds;
    
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;	
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Set<String> updatedCountries = new LinkedHashSet<>();
    
    private ReadWriteLock updateCCLock = new ReentrantReadWriteLock();
    
	@Autowired
	ApplicationContext context;
    
	@Scheduled(fixedRate = 86400000)
////    @Scheduled(fixedRate = 60000)
	public void updateCountries() {
		logger.info("Running scheduled update countries task");
	
		CountryConfigurationsBean ccb = (CountryConfigurationsBean)context.getBean("country-configurations");
		
		for (CountryDB cc : new ArrayList<>(countryConfigurations.values())) {
			Country country = new Country();
			country.setDcat(cc.getDcat());
			
			CountryDB updatedcc = countriesService.updateCountry(country);
			
			if (updatedcc != null) {
				if (updatedcc.modified) {
					Lock writeLock = lock.writeLock();
					writeLock.lock();
					updatedCountries.add(updatedcc.getCode());
					writeLock.unlock();
					
					Lock ccLock = updateCCLock.writeLock();
					ccLock.lock();
					ccb.put(cc.getCode(), updatedcc);
					ccLock.unlock();
				}
			} else {
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
    
	@Scheduled(fixedRate = 30*86400000)
//    @Scheduled(fixedRate = 40000)
	public void updateStatistics() {
		logger.info("Running scheduled update statistics task. Pending countries: " + updatedCountries);
	
		if (updatedCountries.size() > 0) { 
			CountryConfigurationsBean ccb = (CountryConfigurationsBean)context.getBean("country-configurations");
			
			Lock writeLock = lock.writeLock();
			writeLock.lock();
			String ccCode = updatedCountries.iterator().next();
			updatedCountries.remove(ccCode);
			writeLock.unlock();
	
			CountryDB cc = countryConfigurations.get(ccCode);
			if (cc != null) {
				Set<Dimension> dimensions = new LinkedHashSet<>();
				dimensions.add(Dimension.DATA); // should always be included
				dimensions.add(Dimension.NACE);
				dimensions.add(Dimension.NUTSLAU);
				dimensions.add(Dimension.FOUNDING);
				dimensions.add(Dimension.DISSOLUTION);
				dimensions.add(Dimension.NUTSLAU_NACE);
				dimensions.add(Dimension.NUTSLAU_FOUNDING);
				dimensions.add(Dimension.NUTSLAU_DISSOLUTION);
				dimensions.add(Dimension.NACE_FOUNDING);
				dimensions.add(Dimension.NACE_DISSOLUTION);
				
				statisticsService.computeStatistics(cc, dimensions); 
				
				ds.copyStatisticsFromMongoToRDBMS(cc);
				
				Lock ccLock = updateCCLock.writeLock();
				ccLock.lock();
				ccb.put(cc.getCode(), cc);
				ccLock.unlock();
			}
		}
	}

}
