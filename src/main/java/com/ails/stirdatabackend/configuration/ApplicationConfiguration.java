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
	
	@Bean(name = "endpoint-lau-eu")
	public SparqlEndpoint getLauEndpointEU() {
		return new SparqlEndpoint("eu-endpoint", Dimension.LAU, env.getProperty("endpoint.lau.eu"));
	}
	
	@Bean(name = "country-configurations")
	public Map<String, CountryConfiguration> getSupportedCountriesConfigurations() {

		Map<String, CountryConfiguration> map = new HashMap<>();
		
		String defaultNaceEndpoint = env.getProperty("endpoint.nace.00");
		String defaultNutsEndpoint = env.getProperty("endpoint.nuts.00");

	    String defaultEntitySparql = env.getProperty("sparql.entity.00");
	    String defaultEntityNameSparql = env.getProperty("sparql.entityName.00");
	    String defaultNuts3Sparql = env.getProperty("sparql.nuts3.00");
	    String defaultNaceSparql = env.getProperty("sparql.nace.00");
	    String defaultFoundingDateSparql = env.getProperty("sparql.foundingDate.00"); 

		String defaultNacePath1 = env.getProperty("nace.path-1.00");
		String defaultNacePath2 = env.getProperty("nace.path-2.00");
		String defaultNacePath3 = env.getProperty("nace.path-3.00");
		String defaultNacePath4 = env.getProperty("nace.path-4.00");
		String defaultNaceFixedLevel = env.getProperty("nace.fixed-level.00");
		
		String defaultNutsPrefix = env.getProperty("nuts.prefix.00");

		String[] cntr = countries.split(",");
		
		for (String c : cntr) {
			CountryConfiguration cc = new CountryConfiguration(c);
			
			cc.setLabel(env.getProperty("country.label." + c));
			
			String dataEndpoint = env.getProperty("endpoint.data." + c);
			
			cc.setDataEndpoint(new SparqlEndpoint(c, Dimension.DATA, dataEndpoint));

			String naceEndpoint = env.getProperty("endpoint.nace." + c);
			cc.setNaceEndpoint(new SparqlEndpoint(c, Dimension.NACE, naceEndpoint != null ? naceEndpoint : defaultNaceEndpoint)); 

			String nutsEndpoint = env.getProperty("endpoint.nuts." + c);
			cc.setNutsEndpoint(new SparqlEndpoint(c, Dimension.NUTS, nutsEndpoint != null ? nutsEndpoint : defaultNutsEndpoint)); 

		    String entitySparql = env.getProperty("sparql.entity." + c);
		    cc.setEntitySparql(entitySparql != null ? entitySparql : defaultEntitySparql);

		    String naceScheme = env.getProperty("nace.scheme." + c);
		    cc.setNaceScheme(naceScheme);

		    String nacePath1 = env.getProperty("nace.path-1." + c);
		    cc.setNacePath1(nacePath1 != null ? nacePath1 : defaultNacePath1);

		    String nacePath2 = env.getProperty("nace.path-2." + c);
		    cc.setNacePath2(nacePath2 != null ? nacePath2 : defaultNacePath2);
		    
		    String nacePath3 = env.getProperty("nace.path-3." + c);
		    cc.setNacePath3(nacePath3 != null ? nacePath3 : defaultNacePath3);

		    String nacePath4 = env.getProperty("nace.path-4." + c);
		    cc.setNacePath4(nacePath4 != null ? nacePath4 : defaultNacePath4);

		    String naceFixedLevel = env.getProperty("nace.fixed-level." + c);
		    cc.setNaceFixedLevel(naceFixedLevel != null ? Integer.parseInt(naceFixedLevel) : Integer.parseInt(defaultNaceFixedLevel));
		    
		    String entityNameSparql = env.getProperty("sparql.entityName." + c);
	    	cc.setEntityNameSparql(entityNameSparql != null ? entityNameSparql : defaultEntityNameSparql);	

		    String nuts3Sparql = env.getProperty("sparql.nuts3." + c);
	    	cc.setNuts3Sparql(nuts3Sparql != null ? nuts3Sparql : defaultNuts3Sparql);	

		    String naceSparql = env.getProperty("sparql.nace." + c);
	    	cc.setNaceSparql(naceSparql != null ? naceSparql : defaultNaceSparql);	

		    String foundingDateSparql = env.getProperty("sparql.foundingDate." + c);
	    	cc.setFoundingDateSparql(foundingDateSparql != null ? foundingDateSparql : defaultFoundingDateSparql);

		    String nutsPrefix = env.getProperty("nuts.prefix." + c);
		    cc.setNutsPrefix(nutsPrefix != null ? nutsPrefix : defaultNutsPrefix);	

			map.put(c, cc);
		}
		
		return map;
	}
	
}
