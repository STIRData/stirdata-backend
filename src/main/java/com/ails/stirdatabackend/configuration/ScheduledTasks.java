package com.ails.stirdatabackend.configuration;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.service.CountriesService;

@Component
public class ScheduledTasks {

    @Autowired
    private CountriesService countriesService;
    
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;	
    
	@Scheduled(fixedRate = 86400000)
	public void updateCountries() {
		for (CountryDB cc : countryConfigurations.values()) {
			if (!cc.getCode().equals("CZ")) { // due to sparql entpoint
				countriesService.reloadDCAT(cc);
			}
		}
	}

}
