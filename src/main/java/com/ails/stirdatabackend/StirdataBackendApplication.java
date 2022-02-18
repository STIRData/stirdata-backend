package com.ails.stirdatabackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.service.DataService;
import com.ails.stirdatabackend.service.DataStoring;
import com.ails.stirdatabackend.service.NaceService;
import com.ails.stirdatabackend.service.NutsService;
import com.ails.stirdatabackend.service.StatisticsService;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@SpringBootApplication
@EnableMongoRepositories
public class StirdataBackendApplication implements CommandLineRunner {

    @Autowired
    private DataService dataService;

    @Autowired
    private DataStoring ds;
    
    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private StatisticsDBRepository statisticsDBRepository;

    @Autowired
    private PlacesDBRepository placesDBRepository;

	@Autowired
	@Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
    public static void main(String[] args) {
        SpringApplication.run(StirdataBackendApplication.class, args);
    }

	@Override
	public void run(String... args) throws Exception {
//		System.out.println("A " + countryConfigurations);
//		String[] countries = new String[] { "BE", "CY", "CZ", "EE", "EL", "FI", "LV", "NO", "RO", "UK" };
//		String[] countries = new String[] { "EE" }; 
//		for (String cc : countries) {
//			dataService.loadCountry(cc, true, Code.createDateCode("1900-12-01", "2021-12-31", "10Y"));
//		}
//		System.out.println("FINISHED");
		
//		ds.copyStatisticsFromMongoToRDBMS("EE");
//		System.out.println("FINISHED EE");
//		ds.copyStatisticsFromMongoToRDBMS("LV");
//		System.out.println("FINISHED LV");
//		ds.copyStatisticsFromMongoToRDBMS("FI");
//		System.out.println("FINISHED FI");
//		ds.copyStatisticsFromMongoToRDBMS("CY");
//		System.out.println("FINISHED CY");
//		ds.copyStatisticsFromMongoToRDBMS("RO");
//		System.out.println("FINISHED RO");
//		ds.copyStatisticsFromMongoToRDBMS("NO");
//		System.out.println("FINISHED NO");
//		ds.copyStatisticsFromMongoToRDBMS("EL");
//		System.out.println("FINISHED EL");
//		ds.copyStatisticsFromMongoToRDBMS("CZ");
//		System.out.println("FINISHED CZ");
//		ds.copyStatisticsFromMongoToRDBMS("UK", Dimension.DATA);
//		ds.copyStatisticsFromMongoToRDBMS("UK", Dimension.NACE);
//		ds.copyStatisticsFromMongoToRDBMS("UK", Dimension.NUTSLAU);
//		ds.copyStatisticsFromMongoToRDBMS("UK", Dimension.FOUNDING);
//		ds.copyStatisticsFromMongoToRDBMS("UK", Dimension.DISSOLUTION);
//		System.out.println("FINISHED UK");
//		ds.copyStatisticsFromMongoToRDBMS("BE", Dimension.DATA);
//		ds.copyStatisticsFromMongoToRDBMS("BE", Dimension.NACE);
//		ds.copyStatisticsFromMongoToRDBMS("BE", Dimension.NUTSLAU);
//		ds.copyStatisticsFromMongoToRDBMS("BE", Dimension.FOUNDING);
//		ds.copyStatisticsFromMongoToRDBMS("BE", Dimension.DISSOLUTION);
//		System.out.println("FINISHED BE");

//		ds.copyNUTSFromVirtuosoToRDBMS();
//		System.out.println("FINISHED");

//		ds.copyLAUFromVirtuosoToRDBMS();
//		System.out.println("FINISHED");

//		ds.copyNACEFromVirtuosoToRDBMS();
//		System.out.println("FINISHED");

//		ds.copyStatisticsFromMongoToRDBMS();
//		System.out.println("FINISHED");

		
	}

	
}
