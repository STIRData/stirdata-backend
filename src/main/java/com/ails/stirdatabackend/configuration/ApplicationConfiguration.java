package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.controller.URIDescriptor;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.HashSet;
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
	public SparqlEndpoint getNaceEndpointEU() {
		return new SparqlEndpoint("eu-endpoint", Dimension.NACE, env.getProperty("endpoint.nace.eu"));
	}

	@Bean(name = "endpoint-nuts-eu")
	public SparqlEndpoint getNutsEndpointEU() {
		return new SparqlEndpoint("eu-endpoint", Dimension.NUTS, env.getProperty("endpoint.nuts.eu"));
	}
	
	@Bean(name = "country-configurations")
	public Map<String, CountryConfiguration> getSupportedCountriesConfigurations() {

		Map<String, CountryConfiguration> map = new HashMap<>();
		
		String defaultNaceEndpoint = env.getProperty("endpoint.nace.default");
		String defaultNutsEndpoint = env.getProperty("endpoint.nuts.default");

		String[] cntr = countries.split(",");
		
		for (String c : cntr) {
			CountryConfiguration cc = new CountryConfiguration(c);
			
			cc.setLabel(env.getProperty("country.label." + c));
			
			String dataEndpoint = env.getProperty("endpoint.data." + c);
			
			String naceEndpoint = env.getProperty("endpoint.nace." + c);
			String nutsEndpoint = env.getProperty("endpoint.nuts." + c);
			
			cc.setDataEndpoint(new SparqlEndpoint(c, Dimension.DATA, dataEndpoint));
			
			if (naceEndpoint != null) {
				cc.setNaceEndpoint(new SparqlEndpoint(c, Dimension.NACE, naceEndpoint)); 
			} else {
				cc.setNaceEndpoint(new SparqlEndpoint(c, Dimension.NACE, defaultNaceEndpoint));
			}

			if (nutsEndpoint != null) {
				cc.setNutsEndpoint(new SparqlEndpoint(c, Dimension.NUTS, nutsEndpoint)); 
			} else {
				cc.setNutsEndpoint(new SparqlEndpoint(c, Dimension.NUTS, defaultNutsEndpoint));
			}

			map.put(c, cc);
		}
		
		return map;
	}
	
}
