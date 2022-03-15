package com.ails.stirdatabackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.service.CountriesService;
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
@OpenAPIDefinition( 
    servers = {
       @Server(url = "/", description = "Default Server URL")
    }
) 
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
    private CountriesService countriesService;
    
    @Autowired
    private StatisticsService statisticsService;

    private static Country be = new Country("BE");
	private static Country cy = new Country("CY");
	private static Country cz = new Country("CZ"); 
	private static Country ee = new Country("EE");
	private static Country el = new Country("EL");
	private static Country fi = new Country("FI");
	private static Country lv = new Country("LV");
	private static Country no = new Country("NO");
	private static Country ro = new Country("RO");
	private static Country uk = new Country("UK");
	
    static {
    	be.setLabel("Belgium");
    	be.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/be/schema/dcat");
    	be.setNaceScheme("https://lod.stirdata.eu/nace/scheme/NACEBEL2008", "nacebel2008");
    	be.setNacePath1("skos:broader+/skos:exactMatch/skos:broader/skos:broader/skos:broader");
    	be.setNacePath2("skos:broader*/skos:exactMatch");
    	be.setNacePath3("skos:broader*/skos:exactMatch");
    	be.setNacePath4("skos:broader*/skos:exactMatch");
    	be.setNaceFixedLevel(-1);
    	
    	cy.setLabel("Cyprus");
    	cy.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/cy/schema/dcat");
    	
    	cz.setLabel("Czechia");
    	cz.setDcat("https://raw.githubusercontent.com/STIRData/data-catalog/main/Czechia/br-ebg.jsonld");
    	cz.setNaceEnpoint("https://xn--obchodn-rejstk-6lbg94p.stirdata.opendata.cz/sparql");
    	cz.setNaceScheme("https://obchodn\\u00ed-rejst\\u0159\\u00edk.stirdata.opendata.cz/zdroj/\\u010d\\u00edseln\\u00edky/nace-cz");
//    	cz.setNacePath1("skos:broader*/skos:exactMatch");
//    	cz.setNacePath2("skos:broader*/skos:exactMatch");
//    	cz.setNacePath3("skos:broader*/skos:exactMatch");
//    	cz.setNacePath4("skos:broader*/skos:exactMatch");
//    	cz.setNaceFixedLevel(-1);
    	cz.setNacePathSparql("skos:broader*/skos:exactMatch");
    	cz.setNutsPrefix("http://nuts.geovocab.org/id/");
    	
	    ee.setLabel("Estonia");
	    ee.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ee/schema/dcat");
	    
	    el.setLabel("Greece");
	    el.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/el/schema/dcat");
	    el.setNaceScheme("https://lod.stirdata.eu/nace/scheme/KAD2008", "kad2008");
	    el.setNacePath1("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader");
	    el.setNacePath2("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader");
	    el.setNacePath3("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader");
	    el.setNacePath4("skos:broader/skos:broader/skos:broader/skos:exactMatch");
	    el.setNaceFixedLevel(7);
	    
	    fi.setLabel("Finland");
	    fi.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/fi/schema/dcat");
	    fi.setNaceScheme("https://lod.stirdata.eu/nace/scheme/TOL2008", "tol2008");
	    
	    lv.setLabel("Latvia");
	    lv.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/lv/schema/dcat");
	    
	    no.setLabel("Norway");
	    no.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/no/schema/dcat");
	    no.setNaceScheme("https://lod.stirdata.eu/nace/scheme/SIC2007NO", "no-sic2007");
	    
	    ro.setLabel("Romania");
	    ro.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ro/schema/dcat");
	    
	    uk.setLabel("United Kingdom");
	    uk.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/uk/schema/dcat");
	    uk.setNaceScheme("https://lod.stirdata.eu/nace/scheme/SIC2007UK", "uk-sic2007");
    }
    
	@Autowired
	@Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;
    
    public static void main(String[] args) {
        SpringApplication.run(StirdataBackendApplication.class, args);
    }

	@Override
	public void run(String... args) throws Exception {

//		System.out.println(dataService.getEntity("https://lod.stirdata.eu/data/organization/fi/3117737-8"));
//		System.out.println(dataService.getEntity("https://lod.stirdata.eu/data/organization/uk/09025276"));

		
//		countriesService.replace(ee); // ok // rubik
//		countriesService.replace(lv); // ok // rubik
//		countriesService.replace(fi); // ok // rubik
//		countriesService.replace(ro); // ok // rubik
//		countriesService.replace(no); // ok // rubik
//		countriesService.replace(uk); // ok // rubik
//		countriesService.replace(be);
//		countriesService.replace(cy);
//		countriesService.replace(cz);
//		countriesService.replace(el);
//		countriesService.replace(uk);
		
//		Set<Dimension> dimensions = new HashSet<>();
//		dimensions.add(Dimension.DATA); // should always be included
//		dimensions.add(Dimension.NACE);
//		dimensions.add(Dimension.NUTSLAU);
//		dimensions.add(Dimension.FOUNDING);
//		dimensions.add(Dimension.DISSOLUTION);
//		dimensions.add(Dimension.NUTSLAU_NACE);
//		dimensions.add(Dimension.NUTSLAU_FOUNDING);
//		dimensions.add(Dimension.NUTSLAU_DISSOLUTION);
//		dimensions.add(Dimension.NACE_FOUNDING);
//		dimensions.add(Dimension.NACE_DISSOLUTION);
		
//		System.out.println("A " + countryConfigurations);
//		String[] countries = new String[] { "BE", "CY", "CZ", "EE", "EL", "FI", "LV", "NO", "RO", "UK" };
//		statisticsService.computeStatistics(countryConfigurations.get("EE"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("LV"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("FI"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("RO"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("NO"), dimensions); // ok
		
//		dataService.precompute(countries);
		
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("EE")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("LV")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("FI")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("RO")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("NO")); // ok // rubik 
//		ds.copyStatisticsFromMongoToRDBMS("CY");
//		ds.copyStatisticsFromMongoToRDBMS("NO");
//		ds.copyStatisticsFromMongoToRDBMS("EL");
//		ds.copyStatisticsFromMongoToRDBMS("CZ");
//		ds.copyStatisticsFromMongoToRDBMS("UK");
//		ds.copyStatisticsFromMongoToRDBMS("BE");

//		ds.copyNUTSFromVirtuosoToRDBMS();
//		System.out.println("NUTS FINISHED");
//
//		ds.copyLAUFromVirtuosoToRDBMS();
//		System.out.println("LAU FINISHED");

//		ds.copyNACEFromVirtuosoToRDBMS(); // ok //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("FI")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("NO")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("UK")); //rubik Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:98.0
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("EL"));
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("BE"));
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("CZ"));
//		System.out.println("NACE FINISHED");

//		ds.copyStatisticsFromMongoToRDBMS();
//		System.out.println("FINISHED");

//		System.out.println("READY");
		
	}

	
}
