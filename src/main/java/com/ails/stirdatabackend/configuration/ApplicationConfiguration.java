package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.controller.URIDescriptor;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.repository.CountriesRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class ApplicationConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);
	
	@Value("${cache.labels.size}")
	private int cacheSize;

	@Value("${cache.labels.live-time}")
	private int liveTime;
	
	@Value("${app.countries}")
	private String countries;

	@Value("${app.data-models}")
	private String models;
	
	@Value("${statistics.default.from-date}")
	private String fromDate;

	@Autowired
	private CountriesRepository countriesRepository;

	@Autowired
	private StatisticsDBRepository statisticsRepository;
	
	@Bean(name = "labels-cache")
	public Cache getLabelsCache() {
	    CacheManager singletonManager = CacheManager.create();
	    if (!singletonManager.cacheExists("labels")) {
		    singletonManager.addCache(new Cache("labels", cacheSize, false, false, liveTime, liveTime));
		    
			logger.info("Created labels cache.");
	    }
	    
	    return singletonManager.getCache("labels");
	}

	@Bean(name = "nuts-geojson-cache")
	public Cache getNutsGeojsonCache() {
		CacheManager singletonManager = CacheManager.create();
		if (!singletonManager.cacheExists("nuts-geojson-cache")) {
			singletonManager.addCache(new Cache("nuts-geojson-cache", cacheSize, false, false, liveTime, liveTime));

			logger.info("Created nuts-geojson cache.");
		}

		return singletonManager.getCache("nuts-geojson-cache");
	}

	@Bean(name = "prefixes")
	@DependsOn({ "endpoint-nace-eu", "endpoint-nuts-eu" })
	public Set<URIDescriptor> getURIDesriptors() {
	    Set<URIDescriptor> set = new HashSet<>();
	    
	    set.add(new URIDescriptor("https://lod.stirdata.eu/nace/", "http://www.w3.org/2004/02/skos/core#prefLabel", getNaceEndpointEU()));
	    set.add(new URIDescriptor("https://lod.stirdata.eu/nuts/", "http://www.w3.org/2004/02/skos/core#prefLabel", getNutsEndpointEU()));

	    return set;
	}
	
	@Autowired
	private Environment env;
	
	@Bean(name = "endpoint-nace-eu")
	public String getNaceEndpointEU() {
		return env.getProperty("endpoint.nace.eu");
	}

	@Bean(name = "endpoint-nuts-eu")
	public String getNutsEndpointEU() {
		return env.getProperty("endpoint.nuts.eu");
	}
	
	@Bean(name = "endpoint-lau-eu")
	public String getLauEndpointEU() {
		return env.getProperty("endpoint.lau.eu");
	}
	
	@Bean(name = "namedgraph-nace-eu")
	public String getNaceNamedgraphEU() {
		return env.getProperty("namedgraph.nace.eu");
	}

	@Bean(name = "namedgraph-nuts-eu")
	public String getNutsNamedgraphEU() {
		return env.getProperty("namedgraph.nuts.eu");
	}
	
	@Bean(name = "namedgraph-lau-eu")
	public String getLauNamedgraphEU() {
		return env.getProperty("namedgraph.lau.eu");
	}
	
	@Bean(name = "default-from-date")
	public Date getDefaultFromDate() {
		return Date.valueOf(fromDate);
	}
	
	@Bean(name = "model-configurations")
	public Map<String, ModelConfiguration> getSupportedModels() {

		Map<String, ModelConfiguration> map = new HashMap<>();
		
		for (String m : models.split(",")) {
			ModelConfiguration mc = new ModelConfiguration(m);
			
			mc.setUrl(env.getProperty("data-model.url." + m));
			
		    String entitySparql = env.getProperty("sparql.entity." + m);
		    String legalNameSparql = env.getProperty("sparql.legalName." + m);
		    String activeSparql = env.getProperty("sparql.active." + m);
		    String nuts3Sparql = env.getProperty("sparql.nuts3." + m);
		    String lauSparql = env.getProperty("sparql.lau." + m);
		    String naceSparql = env.getProperty("sparql.nace." + m);
		    String foundingDateSparql = env.getProperty("sparql.foundingDate." + m); 
		    String dissolutionDateSparql = env.getProperty("sparql.dissolutionDate." + m);

		    mc.setEntitySparql(entitySparql);
	    	mc.setLegalNameSparql(legalNameSparql);	
	    	mc.setActiveSparql(activeSparql);
	    	mc.setNuts3Sparql(nuts3Sparql);	
	    	mc.setLauSparql(lauSparql);	
	    	mc.setNaceSparql(naceSparql);	
	    	mc.setFoundingDateSparql(foundingDateSparql);
	    	mc.setDissolutionDateSparql(dissolutionDateSparql);

			map.put(mc.getUrl(), mc);
		}
		
		return map;
	}
	
	@Bean(name = "country-configurations")
	@DependsOn("model-configurations")
	public Map<String, CountryConfiguration> getSupportedCountriesConfigurations(@Qualifier("model-configurations") Map<String,ModelConfiguration> mcMap) {
		Map<String, CountryConfiguration> map = new HashMap<>();
		
//		System.out.println("LOADING COUNTRIES: ");
		String s = "";
		for (CountryConfiguration cc  : countriesRepository.findAll()) {
			cc.setModelConfiguration(mcMap.get(cc.getConformsTo()));
			cc.setStatistics(new HashSet<>(statisticsRepository.findDimensionsByCountry(cc.getCountryCode())));
			
			s += cc.getCountryCode() + " ";
			map.put(cc.getCountryCode(), cc);
		}
		logger.info("Loaded countries: " + s);
		
		return map;
	}
	
//	public Map<String, CountryConfiguration> getSupportedCountriesConfigurations(@Qualifier("model-configurations") Map<String,ModelConfiguration> mcMap) {
//
//		Set<String> supportedModelUrls = new HashSet<>() ;
//		Map<String, ModelConfiguration> mcUriMap = new HashMap<>() ;
//		for (ModelConfiguration mc : mcMap.values()) {
//			supportedModelUrls.add(mc.getUrl());
//			mcUriMap.put(mc.getUrl(), mc);
//		}
//		
////		System.out.println(supportedModelUrls);
//		Map<String, CountryConfiguration> map = new HashMap<>();
//		
//		String defaultNaceEndpoint = env.getProperty("endpoint.nace.00");
//		String defaultNutsEndpoint = env.getProperty("endpoint.nuts.00");
//
//		String defaultNacePath1 = env.getProperty("nace.path-1.00");
//		String defaultNacePath2 = env.getProperty("nace.path-2.00");
//		String defaultNacePath3 = env.getProperty("nace.path-3.00");
//		String defaultNacePath4 = env.getProperty("nace.path-4.00");
//		String defaultNaceFixedLevel = env.getProperty("nace.fixed-level.00");
//		
//		String defaultNutsPrefix = env.getProperty("nuts.prefix.00");
//		String defaultLauPrefix = env.getProperty("lau.prefix.00");
//
//		String[] cntr = countries.split(",");
//		
//		for (String c : cntr) {
////			System.out.println(c);
//			CountryConfiguration cc = new CountryConfiguration(c);
//			
//			cc.setCountryLabel(env.getProperty("country.label." + c));
//			
////			String dcat = env.getProperty("country.dcat." + c);
////			
////			if (dcat != null) {
////				processDcat(cc, dcat, supportedModelUrls, mcUriMap);
////			}
//			
//			for (String d : env.getProperty("country.dimensions." + c).split(",")) {
//				if (d.equalsIgnoreCase("nace")) {
//					cc.setNace(true);
//				} else if (d.equalsIgnoreCase("nuts")) {
//					cc.setNuts(true);
//				} else if (d.equalsIgnoreCase("lau")) {
//					cc.setLau(true);
//				} else if (d.equalsIgnoreCase("founding")) {
//					cc.setFoundingDate(true);
//				} else if (d.equalsIgnoreCase("dissolution")) {
//					cc.setDissolutionDate(true);
//				}
//			}
//			
//
//			if (cc.getDataEndpoint() == null) {
//				String dataEndpoint = env.getProperty("endpoint.data." + c);
//				cc.setDataEndpoint(dataEndpoint);
//			}
//			
////			System.out.println(cc.getCountry() + " : " + cc.getDataEndpoint().getSparqlEndpoint());
//			
//			String naceEndpoint = env.getProperty("endpoint.nace." + c);
//			cc.setNaceEndpoint(naceEndpoint != null ? naceEndpoint : defaultNaceEndpoint); 
//
//			String nutsEndpoint = env.getProperty("endpoint.nuts." + c);
//			cc.setNutsEndpoint(nutsEndpoint != null ? nutsEndpoint : defaultNutsEndpoint); 
//
//		    String naceScheme = env.getProperty("nace.scheme." + c);
//		    cc.setNaceScheme(naceScheme);
//
//		    String nacePath1 = env.getProperty("nace.path-1." + c);
//		    cc.setNacePath1(nacePath1 != null ? nacePath1 : defaultNacePath1);
//
//		    String nacePath2 = env.getProperty("nace.path-2." + c);
//		    cc.setNacePath2(nacePath2 != null ? nacePath2 : defaultNacePath2);
//		    
//		    String nacePath3 = env.getProperty("nace.path-3." + c);
//		    cc.setNacePath3(nacePath3 != null ? nacePath3 : defaultNacePath3);
//
//		    String nacePath4 = env.getProperty("nace.path-4." + c);
//		    cc.setNacePath4(nacePath4 != null ? nacePath4 : defaultNacePath4);
//
//		    String naceFixedLevel = env.getProperty("nace.fixed-level." + c);
//		    cc.setNaceFixedLevel(naceFixedLevel != null ? Integer.parseInt(naceFixedLevel) : Integer.parseInt(defaultNaceFixedLevel));
//		    
//		    String nutsPrefix = env.getProperty("nuts.prefix." + c);
//		    cc.setNutsPrefix(nutsPrefix != null ? nutsPrefix : defaultNutsPrefix);	
//
//		    String lauPrefix = env.getProperty("lau.prefix." + c);
//		    cc.setLauPrefix(lauPrefix != null ? lauPrefix : defaultLauPrefix);	
//
//		    cc.setEntitySparql(env.getProperty("sparql.entity." + c));
//	    	cc.setLegalNameSparql(env.getProperty("sparql.legalName." + c));	
//	    	cc.setActiveSparql(env.getProperty("sparql.active." + c));
//	    	cc.setNuts3Sparql(env.getProperty("sparql.nuts3." + c));	
//	    	cc.setLauSparql(env.getProperty("sparql.lau." + c));	
//	    	cc.setNaceSparql(env.getProperty("sparql.nace." + c));	
//	    	cc.setFoundingDateSparql(env.getProperty("sparql.foundingDate." + c));
//	    	cc.setDissolutionDateSparql(env.getProperty("sparql.dissolutionDate." + c));
//	    	
//			map.put(c, cc);
//		}
//		
//		return map;
//	}
	
}
