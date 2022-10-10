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
	private static Country fr = new Country("FR");
	private static Country lv = new Country("LV");
	private static Country md = new Country("MD");
	private static Country nl = new Country("NL");
	private static Country no = new Country("NO");
	private static Country ro = new Country("RO");
	private static Country uk = new Country("UK");
	
    static {
    	be.setLabel("Belgium");
    	be.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/be/schema/dcat");
    	be.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-be/sparql");
    	be.setNaceScheme("http://be.data.stirdata.eu/resource/nace/scheme/NACEBEL2008", "nacebel2008");
	    be.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-be/sparql");
	    be.setCompanyTypeScheme("http://be.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-BE", "ct-be");
    	
//    	be.setNacePath1("skos:broader+/skos:exactMatch/skos:broader/skos:broader/skos:broader");
//    	be.setNacePath2("skos:broader*/skos:exactMatch");
//    	be.setNacePath3("skos:broader*/skos:exactMatch");
//    	be.setNacePath4("skos:broader*/skos:exactMatch");
//    	be.setNaceFixedLevel(-1);
    	
    	cy.setLabel("Cyprus");
    	cy.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/cy/schema/dcat");
    	cy.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
	    cy.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-cy/sparql");
	    cy.setCompanyTypeScheme("http://cy.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-CY", "ct-cy");

	    
    	cz.setLabel("Czechia");
//    	cz.setDcat("https://raw.githubusercontent.com/STIRData/data-catalog/main/Czechia/br-ebg.jsonld");
    	cz.setDcat("https://data.europa.eu/api/hub/repo/datasets/https-lkod-mff-cuni-cz-zdroj-datove-sady-stirdata-obchodni-rejstr-i-k-stirdata.rdf?useNormalizedId=true");
    	cz.setLicense("Otevřené data ISVR", "https://dataor.justice.cz/files/ISVR_OpenData_Podminky_uziti.pdf");
    	cz.setNaceEnpoint("https://xn--obchodn-rejstk-6lbg94p.stirdata.opendata.cz/vsparql");
//    	cz.setNaceScheme("https://obchodn\\u00ed-rejst\\u0159\\u00edk.stirdata.opendata.cz/zdroj/\\u010d\\u00edseln\\u00edky/nace-cz", "nace-cz");
    	cz.setNaceScheme("https://obchodní-rejstřík.stirdata.opendata.cz/zdroj/číselníky/nace-cz", "nace-cz");
//    	cz.setNacePath1("skos:broader*/skos:exactMatch");
//    	cz.setNacePath2("skos:broader*/skos:exactMatch");
//    	cz.setNacePath3("skos:broader*/skos:exactMatch");
//    	cz.setNacePath4("skos:broader*/skos:exactMatch");
//    	cz.setNaceFixedLevel(-5);
    	cz.setNaceFixedLevels(1,2,3,4,5);
    	cz.setNacePathSparql("skos:broader*/skos:exactMatch");
    	cz.setCompanyTypeEndpoint("https://xn--obchodn-rejstk-6lbg94p.stirdata.opendata.cz/vsparql");
    	
	    ee.setLabel("Estonia");
	    ee.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ee/schema/dcat");
	    ee.setLicense("CC BY-SA 3.0", "https://creativecommons.org/licenses/by-sa/3.0/");
	    ee.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-ee/sparql");
	    ee.setCompanyTypeScheme("http://ee.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-EE", "ct-ee");
	    
	    el.setLabel("Greece (Athens)");
	    el.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/el/schema/dcat");
	    el.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-el/sparql");
	    el.setNaceScheme("http://el.data.stirdata.eu/resource/nace/scheme/KAD2008", "kad2008");
	    el.setNacePath1("skos:broader/skos:broader/skos:broader/skos:broader/skos:broader/skos:broader/skos:exactMatch");
	    el.setNacePath2("skos:broader/skos:broader/skos:broader/skos:broader/skos:broader/skos:exactMatch");
	    el.setNacePath3("skos:broader/skos:broader/skos:broader/skos:broader/skos:exactMatch");
	    el.setNacePath4("skos:broader/skos:broader/skos:broader/skos:exactMatch");
//	    el.setNaceFixedLevel(7);
	    el.setNaceFixedLevels(7);

	    
	    fi.setLabel("Finland");
	    fi.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/fi/schema/dcat");
	    fi.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-fi/sparql");
	    fi.setNaceScheme("http://fi.data.stirdata.eu/resource/nace/scheme/TOL2008", "tol2008");
	    fi.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
	    fi.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-fi/sparql");
	    fi.setCompanyTypeScheme("http://fi.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-FI", "ct-fi");
	    
	    fr.setLabel("France");
	    fr.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/fr/schema/dcat");
	    fr.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-fr/sparql");
	    fr.setNaceScheme("http://fr.data.stirdata.eu/resource/nace/scheme/NAFRev2", "naf-rev2");
	    fr.setLicense("Licence Ouverte / Open Licence version 2.0", "https://www.etalab.gouv.fr/licence-ouverte-open-licence/");
	    fr.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-fr/sparql");
	    fr.setCompanyTypeScheme("http://fr.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-FR", "ct-fr");
	    
	    lv.setLabel("Latvia");
	    lv.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/lv/schema/dcat");
	    lv.setLicense("CC0 1.0", "https://creativecommons.org/publicdomain/zero/1.0/");
	    lv.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-lv/sparql");
	    lv.setCompanyTypeScheme("http://lv.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-LV", "ct-lv");

    	md.setLabel("Moldova");
    	md.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/md/schema/dcat");
    	md.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-md/sparql");
    	md.setNaceScheme("http://md.data.stirdata.eu/resource/nace/scheme/CAEM2", "caem2");
    	md.setNaceFixedLevels(2,3,4);
//	    md.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-be/sparql");
//	    md.setCompanyTypeScheme("http://be.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-BE", "ct-be");
	    
    	nl.setLabel("Netherlands");
    	nl.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nl/schema/dcat");
    	nl.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-nl/sparql");
    	nl.setNaceScheme("http://nl.data.stirdata.eu/resource/nace/scheme/SBI2008", "sbi2008");
    	nl.setNaceFixedLevels(4, 5);

	    no.setLabel("Norway");
	    no.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/no/schema/dcat");
	    no.setLicense("NLOD", "https://data.norge.no/nlod/no/");
	    no.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-no/sparql");
	    no.setNaceScheme("http://no.data.stirdata.eu/resource/nace/scheme/SIC2007NO", "no-sic2007");
	    no.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-no/sparql");
	    no.setCompanyTypeScheme("http://no.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-NO", "ct-no");
	    
	    
	    ro.setLabel("Romania");
	    ro.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/ro/schema/dcat");
	    ro.setLicense("CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/");
	    
	    uk.setLabel("United Kingdom");
	    uk.setDcat("https://stirdata-semantic.ails.ece.ntua.gr/api/content/uk/schema/dcat");
	    uk.setNaceEnpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/nace-uk/sparql");
	    uk.setNaceScheme("http://uk.data.stirdata.eu/resource/nace/scheme/SIC2007UK", "uk-sic2007");
	    uk.setCompanyTypeEndpoint("https://stirdata-semantic.ails.ece.ntua.gr/api/content/companytype-uk/sparql");
	    uk.setCompanyTypeScheme("http://uk.data.stirdata.eu/resource/companyType/scheme/COMPANYTYPES-UK", "ct-uk");
	    
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
//		countriesService.replace(ro); // ok // rubik-w3id
//		countriesService.replace(no); // ok // rubik-w3id
//		countriesService.replace(cy);       // rubik-w3id
//		countriesService.replace(uk); // ok // rubik-w3id
//		countriesService.replace(nl);       // rubik-w3id
//		countriesService.replace(be);       // rubik-w3id
//		countriesService.replace(cz);       // rubik
//		countriesService.replace(el);	    // rubik-w3id
//		countriesService.replace(fr);       // rubik-w3id
//		countriesService.replace(md);       // rubik-w3id
		
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
		
//		statisticsService.computeStatistics(countryConfigurations.get("EE"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("LV"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("FI"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("RO"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("NO"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("CY"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("NL"), dimensions); // ok data, nace, found, diss
//		statisticsService.computeStatistics(countryConfigurations.get("UK"), dimensions); // ok  
//		statisticsService.computeStatistics(countryConfigurations.get("CZ"), dimensions); // ok			
//		statisticsService.computeStatistics(countryConfigurations.get("BE"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("FR"), dimensions); // ok 
//		statisticsService.computeStatistics(countryConfigurations.get("MD"), dimensions); // ok
//		statisticsService.computeStatistics(countryConfigurations.get("EL"), dimensions); // ok
		
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
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("UK"));       // rubik       
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("BE"));       // rubik 
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("MD"));       // rubik
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("FR"));       // rubik 
//		ds.copyStatisticsFromMongoToRDBMS(countryConfigurations.get("EL"));       // rubik
//		2022-05-17 00:09:41.246 ERROR 20484 --- [           main] c.a.stirdatabackend.service.DataStoring  : Not unique NACE_FOUNDING statistics for FR [ null nace-rev2:23.65 2020-01-01 2020-01-01 null null ].
		
//		ds.copyStatisticsFromMongoToRDBMS("EL");

//		ds.copyNUTSFromVirtuosoToRDBMS();
//		ds.copyNUTSFromVirtuosoToRDBMS(countryConfigurations.get("MD"));
//		System.out.println("NUTS FINISHED");
//
//		ds.copyLAUFromVirtuosoToRDBMS();
//		ds.copyLAUFromVirtuosoToRDBMS(countryConfigurations.get("MD"));
//		System.out.println("LAU FINISHED");
		
//		ds.updateNUTSDB();

//		ds.deleteNACEFromRDBMS(countryConfigurations.get("UK"));

//		ds.copyNACEFromVirtuosoToRDBMS(); // ok //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("FI")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("NO")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("UK")); //rubik Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:98.0
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("CZ")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("BE")); //rubik
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("MD")); //rubik
		
//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("FR")); //rubik
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.1
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.12; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.12
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.13; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.13
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.14; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.14
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.15; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.15
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.16; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.16
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.19; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id naf-rev2:01.19

//		ds.copyNACEFromVirtuosoToRDBMS(countryConfigurations.get("EL"));
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.0; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.0
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.01; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.01
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.02; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.02
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.03; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id nace-rev2:00.03
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01.0; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01.0
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02.0; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02.0
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03.0; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03.0
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01.00; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.01.00
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02.00; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.02.00
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03.00; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:00.03.00
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:01.00.00; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:01.00.00
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20
//		Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20; nested exception is javax.persistence.EntityNotFoundException: Unable to find com.ails.stirdatabackend.model.ActivityDB with id kad2008:46.69.20		
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
		
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("EE")); //rubik
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("LV")); //rubik
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("FI")); //rubik
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("CY")); //rubik
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("NO")); //rubik
//		ds.copyCompanyTypesFromVirtuosoToRDBMS(countryConfigurations.get("UK")); //not added ! needs to add company data first with legal forms. 
		
		System.out.println("READY");
		
	}

	
}
