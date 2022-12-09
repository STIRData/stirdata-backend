package com.ails.stirdatabackend.configuration;

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
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.service.CountriesService;

@Component
public class ScheduledTasks {

	Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	
    @Autowired
    private CountriesService countriesService;
    
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;	
    
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Set<String> updatedCountries = new LinkedHashSet<>();
    
	@Autowired
	ApplicationContext context;
    
	@Scheduled(fixedRate = 86400000)
//    @Scheduled(fixedRate = 60000)
	public void updateCountries() {
		logger.info("Running scheduled update countries task");
	
		CountryConfigurationsBean ccb = (CountryConfigurationsBean)context.getBean("country-configurations");
		
		for (CountryDB cc : countryConfigurations.values()) {
			if (cc.getCode().equals("BE") ||
				cc.getCode().equals("CY") || 
				cc.getCode().equals("EE") || 
				cc.getCode().equals("LV") || 
				cc.getCode().equals("FI") || 
				cc.getCode().equals("NO") || 
				cc.getCode().equals("CZ") || 
				cc.getCode().equals("RO") ||
				cc.getCode().equals("NL")) {
				Country country = new Country();
				country.setDcat(cc.getDcat());
				CountryDB updatedcc = countriesService.updateCountry(country);
				if (updatedcc != null) {
					Lock writeLock = lock.writeLock();
					writeLock.lock();
					updatedCountries.add(updatedcc.getCode());
					writeLock.unlock();
//					cc = updatedcc;
					ccb.put(cc.getCode(), updatedcc);
				}
			}	
		}
		
	}
    
//    @Scheduled(fixedRate = 40000)
	public void updateStatistics() {
		logger.info("Running scheduled update statistics task. Pending countries: " + updatedCountries);
	
		Lock writeLock = lock.writeLock();
		writeLock.lock();
		String cc = updatedCountries.iterator().next();
		updatedCountries.remove(cc);
		writeLock.unlock();

		Set<Dimension> dimensions = new LinkedHashSet<>();
		dimensions.add(Dimension.DATA); // should always be included
		dimensions.add(Dimension.NUTSLAU); 
		dimensions.add(Dimension.NACE);  
		dimensions.add(Dimension.FOUNDING); 
		dimensions.add(Dimension.DISSOLUTION); 
		dimensions.add(Dimension.NUTSLAU_NACE);
		dimensions.add(Dimension.NUTSLAU_FOUNDING);
		dimensions.add(Dimension.NUTSLAU_DISSOLUTION);
		dimensions.add(Dimension.NACE_FOUNDING);
		dimensions.add(Dimension.NACE_DISSOLUTION);
//		
//		statisticsService.computeStatistics(cc, dimensions); // ok
	}

}
