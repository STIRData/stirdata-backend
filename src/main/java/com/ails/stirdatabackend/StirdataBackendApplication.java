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
import com.ails.stirdatabackend.service.PlaceNode;
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

    @Autowired
    private NutsService nutsService;

    
    private static Country be = new Country("BE");
	private static Country cy = new Country("CY");
	private static Country cz = new Country("CZ"); 
	private static Country ee = new Country("EE");
	private static Country el = new Country("EL");
	private static Country fi = new Country("FI");
	private static Country lv = new Country("LV");
	private static Country nl = new Country("NL");
	private static Country no = new Country("NO");
	private static Country ro = new Country("RO");
	private static Country uk = new Country("UK");
	
    static {
    	be.setLabel("Belgium");
    	be.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/be/schema/dcat");
    	be.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-be/sparql");
    	be.setNaceScheme("https://lod.stirdata.eu/nace/scheme/NACEBEL2008", "nacebel2008");
    	be.setNacePath1("skos:broader+/skos:exactMatch/skos:broader/skos:broader/skos:broader");
    	be.setNacePath2("skos:broader*/skos:exactMatch");
    	be.setNacePath3("skos:broader*/skos:exactMatch");
    	be.setNacePath4("skos:broader*/skos:exactMatch");
    	be.setNaceFixedLevel(-1);
    	be.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	be.setLauPrefix("https://lod.stirdata.eu/lau/code/");
    	
    	cy.setLabel("Cyprus");
    	cy.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/cy/schema/dcat");
    	cy.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
    	cy.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	cy.setLauPrefix("https://lod.stirdata.eu/lau/code/");
    	
    	cz.setLabel("Czechia");
//    	cz.setDcat("https://raw.githubusercontent.com/STIRData/data-catalog/main/Czechia/br-ebg.jsonld");
    	cz.setDcat("https://data.europa.eu/api/hub/repo/datasets/https-lkod-mff-cuni-cz-zdroj-datove-sady-stirdata-obchodni-rejstr-i-k-stirdata.rdf?useNormalizedId=true");
    	cz.setNaceEnpoint("https://xn--obchodn-rejstk-6lbg94p.stirdata.opendata.cz/vsparql");
//    	cz.setNaceScheme("https://obchodn\\u00ed-rejst\\u0159\\u00edk.stirdata.opendata.cz/zdroj/\\u010d\\u00edseln\\u00edky/nace-cz", "nace-cz");
    	cz.setNaceScheme("https://obchodní-rejstřík.stirdata.opendata.cz/zdroj/číselníky/nace-cz", "nace-cz");
//    	cz.setNacePath1("skos:broader*/skos:exactMatch");
//    	cz.setNacePath2("skos:broader*/skos:exactMatch");
//    	cz.setNacePath3("skos:broader*/skos:exactMatch");
//    	cz.setNacePath4("skos:broader*/skos:exactMatch");
    	cz.setNaceFixedLevel(-5);
    	cz.setNacePathSparql("skos:broader*/skos:exactMatch");
    	cz.setNutsPrefix("http://nuts.geovocab.org/id/");
    	cz.setLauPrefix("https://lod.stirdata.eu/lau/code/");
    	
	    ee.setLabel("Estonia");
	    ee.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ee/schema/dcat");
	    ee.setLicense("CC BY-SA 3.0", "https://creativecommons.org/licenses/by-sa/3.0/");
	    
	    el.setLabel("Greece");
	    el.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/el/schema/dcat");
	    el.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-el/sparql");
	    el.setNaceScheme("https://lod.stirdata.eu/nace/scheme/KAD2008", "kad2008");
	    el.setNacePath1("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader");
	    el.setNacePath2("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader");
	    el.setNacePath3("skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader");
	    el.setNacePath4("skos:broader/skos:broader/skos:broader/skos:exactMatch");
	    el.setNaceFixedLevel(7);
    	el.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	el.setLauPrefix("https://lod.stirdata.eu/lau/code/");
	    
	    fi.setLabel("Finland");
	    fi.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/fi/schema/dcat");
	    fi.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-fi/sparql");
	    fi.setNaceScheme("http://fi.data.stirdata.eu/resource/nace/scheme/TOL2008", "tol2008");
	    fi.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
	    
	    lv.setLabel("Latvia");
	    lv.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/lv/schema/dcat");
	    lv.setLicense("CC0 1.0", "https://creativecommons.org/publicdomain/zero/1.0/");
	    
    	nl.setLabel("Netherlands");
    	nl.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nl/schema/dcat");
    	nl.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-nl/sparql");
    	nl.setNaceScheme("https://lod.stirdata.eu/nace/scheme/SBI2008", "sbi2008");
    	nl.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	nl.setLauPrefix("https://lod.stirdata.eu/lau/code/");

	    no.setLabel("Norway");
	    no.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/no/schema/dcat");
	    no.setLicense("NLOD", "https://data.norge.no/nlod/no/");
	    no.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-no/sparql");
	    no.setNaceScheme("https://lod.stirdata.eu/nace/scheme/SIC2007NO", "no-sic2007");
    	no.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	no.setLauPrefix("https://lod.stirdata.eu/lau/code/");
	    
	    ro.setLabel("Romania");
	    ro.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ro/schema/dcat");
	    ro.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
	    
	    uk.setLabel("United Kingdom");
	    uk.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/uk/schema/dcat");
	    uk.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-uk/sparql");
	    uk.setNaceScheme("https://lod.stirdata.eu/nace/scheme/SIC2007UK", "uk-sic2007");
    	uk.setNutsPrefix("https://lod.stirdata.eu/nuts/code/");
    	uk.setLauPrefix("https://lod.stirdata.eu/lau/code/");
    }
    
	@Autowired
	@Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;
    
    public static void main(String[] args) {
        SpringApplication.run(StirdataBackendApplication.class, args);
    }

	@Override
	public void run(String... args) throws Exception {

		
//		PlaceNode pn = nutsService.buildPlaceTree(countryConfigurations.get("FI"), false);
//		System.out.println(pn);
////
//		PlaceNode pn2 = nutsService.buildPlaceTree(countryConfigurations.get("NO"), false);
//		System.out.println(pn2);
		
//		System.out.println(dataService.getEntity("https://lod.stirdata.eu/data/organization/fi/3117737-8"));
//		System.out.println(dataService.getEntity("https://lod.stirdata.eu/data/organization/uk/09025276"));

		
//		countriesService.replace(ee); // ok // rubik-w3id
//		countriesService.replace(lv); // ok // rubik-w3id
//		countriesService.replace(fi); // ok // rubik-w3id
//		countriesService.replace(ro); // ok // rubik
//		countriesService.replace(no); // ok // rubik
//		countriesService.replace(cy);       // rubik
//		countriesService.replace(uk); // ok // rubik
//		countriesService.replace(nl);       // rubik
//		countriesService.replace(be);
//		countriesService.replace(cz);       // rubik
//		countriesService.replace(el);
//		countriesService.replace(fr);
		
		Set<Dimension> dimensions = new HashSet<>();
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
//		statisticsService.computeStatistics(countryConfigurations.get("CY"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("NL"), dimensions); // ok data, nace, found, diss
//		statisticsService.computeStatistics(countryConfigurations.get("UK"), dimensions); // ok data, nace, nutslau, found, diss, nutslau_nace 
//		statisticsService.computeStatistics(countryConfigurations.get("CZ"), dimensions); // ok			
		
//		dataService.precompute(countries);
		
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("EE")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("LV")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("FI")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("RO")); // ok // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("NO")); // ok // rubik 
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("CY"));       // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("CZ"));       // rubik
//		2022-04-11 14:07:15.803 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:46.5 2020-01-01 2020-01-15 null null ].
//		2022-04-11 14:11:38.480 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:41.10 2020-01-01 2020-01-17 null null ].
//		2022-04-11 14:11:53.505 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:47.41 2020-01-01 2020-01-01 null null ].
//		2022-04-11 14:12:56.744 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:65.20 2020-01-01 2020-01-16 null null ].
//		2022-04-11 14:13:50.240 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:78.20 2020-01-01 2020-01-17 null null ].
//		2022-04-11 14:14:32.766 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:95.21 2020-01-01 2020-01-15 null null ].
//		2022-04-11 14:14:33.172 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:95.12 2020-01-01 2020-01-14 null null ].
//		2022-04-11 14:14:37.743 ERROR 13896 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for CZ [ null nace-rev2:85.60 2020-01-01 2020-01-22 null null ].
		
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("NL"));       // rubik data, nace, found, diss
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("UK"));       // rubik data, nace, nutslau, found, diss, nutslau_nace      
//		ds.copyStatisticsFromMongoToRDBMS("EL");
//		ds.copyStatisticsFromMongoToRDBMS("CZ");
//		ds.copyStatisticsFromMongoToRDBMS("UK");
//		ds.copyStatisticsFromMongoToRDBMS("BE");

//		ds.copyNUTSFromVirtuosoToRDBMS();
//		System.out.println("NUTS FINISHED");
//
//		ds.copyLAUFromVirtuosoToRDBMS();
//		System.out.println("LAU FINISHED");
		
//		ds.updateNUTSDB();

//		ds.deleteNACEFromRDBMS(countryConfigurations.get("UK"));

//		ds.copyNACEFromVirtuosoToRDBMS(); // ok //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("FI")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("NO")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("UK")); //rubik Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:98.0
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("CZ")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("EL"));
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("BE"));
		
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("NL")); //rubik
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.21; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.21
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.22; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.22
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.23; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.23
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.24; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.24
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.29; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:33.29
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:35.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:35.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:46.68; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:46.68
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:86.91; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:86.91
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:86.92; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:86.92
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:93.14; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:93.14
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:93.15; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:93.15
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:3322
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:4668; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:4668
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:4668; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:4668
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8691
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:8692
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9314
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9315; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9315
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9315; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id sbi2008:9315
//		System.out.println("NACE FINISHED");

//		ds.copyStatisticsFromMongoToRDBMS();
//		System.out.println("FINISHED");
		

		System.out.println("READY");
		
	}

	
}
