package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.controller.URIDescriptor;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.CountriesRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.service.CountriesService;
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
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.Symbol;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
//@EnableScheduling
public class ApplicationConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);
	
	@Value("${cache.labels.size}")
	private int cacheSize;

	@Value("${cache.labels.live-time}")
	private int liveTime;
	
	@Value("${app.data-models}")
	private String models;
	
	@Value("${statistics.default.from-date}")
	private String fromDate;

	@Autowired
	private CountriesDBRepository countriesRepository;

	@Autowired
	ResourceLoader resourceLoader;

	@Bean(name = "labels-cache")
	public Cache getLabelsCache() {
	    CacheManager singletonManager = CacheManager.create();
	    if (!singletonManager.cacheExists("labels")) {
		    singletonManager.addCache(new Cache("labels", cacheSize, false, false, liveTime, liveTime));
		    
			logger.info("Created labels cache.");
	    }
	    
	    return singletonManager.getCache("labels");
	}

//	@Bean(name = "nuts-geojson-cache")
//	public Cache getNutsGeojsonCache() {
//		CacheManager singletonManager = CacheManager.create();
//		if (!singletonManager.cacheExists("nuts-geojson-cache")) {
//			singletonManager.addCache(new Cache("nuts-geojson-cache", cacheSize, false, false, liveTime, liveTime));
//
//			logger.info("Created nuts-geojson cache.");
//		}
//
//		return singletonManager.getCache("nuts-geojson-cache");
//	}

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
	
	@Bean(name = "endpoint-nuts-stats-eu")
	public String getNutsStatsEndpointEU() {
		return env.getProperty("endpoint.nuts-stats.eu");
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
		    String tradingNameSparql = env.getProperty("sparql.tradingName." + m);
		    String activeSparql = env.getProperty("sparql.active." + m);
		    String addressSparql = env.getProperty("sparql.address." + m);
		    String nuts3Sparql = env.getProperty("sparql.nuts3." + m);
		    String lauSparql = env.getProperty("sparql.lau." + m);
		    String naceSparql = env.getProperty("sparql.nace." + m);
		    String companyTypeSparql = env.getProperty("sparql.companyType." + m);
		    String foundingDateSparql = env.getProperty("sparql.foundingDate." + m); 
		    String dissolutionDateSparql = env.getProperty("sparql.dissolutionDate." + m);

		    mc.setEntitySparql(entitySparql);
	    	mc.setLegalNameSparql(legalNameSparql);	
	    	mc.setTradingNameSparql(tradingNameSparql);
	    	mc.setActiveSparql(activeSparql);
	    	mc.setAddressSparql(addressSparql);
	    	mc.setNuts3Sparql(nuts3Sparql);	
	    	mc.setLauSparql(lauSparql);	
	    	mc.setNaceSparql(naceSparql);	
	    	mc.setCompanyTypeSparql(companyTypeSparql);
	    	mc.setFoundingDateSparql(foundingDateSparql);
	    	mc.setDissolutionDateSparql(dissolutionDateSparql);

			map.put(mc.getUrl(), mc);
		}
		
		return map;
	}
	
	@Bean(name = "country-configurations")
	@DependsOn("model-configurations")
	public Map<String, CountryDB> getSupportedCountriesConfigurations(@Qualifier("model-configurations") Map<String, ModelConfiguration> mcMap) {
		Map<String, CountryDB> map = new HashMap<>();
		
		System.out.println("LOADING COUNTRIES: ");
		String s = "";
		for (CountryDB cc  : countriesRepository.findAll()) {
			cc.setModelConfiguration(mcMap.get(cc.getConformsTo()));
//			cc.setStatistics(new HashSet<>(statisticsRepository.findDimensionsByCountry(cc.getCode())));
			
			s += cc.getCode() + " ";
			map.put(cc.getCode(), cc);
		}
		logger.info("Loaded countries: " + s);
		
		return map;
	}
	
	@Bean(name = "model-jsonld-context")
	public JsonLDWriteContext getContext() {
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		
		try (InputStream in = resourceLoader.getResource("classpath:stirdata.jsonld").getInputStream()) {
	        ctx.setJsonLDContext(new String(FileCopyUtils.copyToByteArray(in), StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ctx;
	}
	
}
