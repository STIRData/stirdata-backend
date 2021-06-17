package com.ails.stirdatabackend.configuration;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.ails.stirdatabackend.controller.URIDescriptor;
import com.ails.stirdatabackend.model.SparqlEndpoint;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

@Configuration
public class ApplicationConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);
	
	@Value("${cache.labels.size}")
	private int cacheSize;

	@Value("${cache.labels.live-time}")
	private int liveTime;
	
    @Autowired
    @Qualifier("nace-sparql-endpoint")
    private SparqlEndpoint naceSparqlEndpoint;

    @Autowired
    @Qualifier("nuts-sparql-endpoint")
    private SparqlEndpoint nutsSparqlEndpoint;
	
	@Bean(name = "labels-cache")
	public Cache getLabelsCache() {
	    CacheManager singletonManager = CacheManager.create();
	    if (!singletonManager.cacheExists("labels")) {
		    singletonManager.addCache(new Cache("labels", cacheSize, false, false, liveTime, liveTime));
		    
			logger.info("Created labels cache.");
	    }
	    
	    return singletonManager.getCache("labels");
	}
	
	@Bean(name = "prefixes")
	@DependsOn({ "nuts-sparql-endpoint", "nace-sparql-endpoint" })
	public Set<URIDescriptor> getURIDesriptors() {
	    Set<URIDescriptor> set = new HashSet<>();
	    
	    set.add(new URIDescriptor("https://lod.stirdata.eu/nace/", "http://www.w3.org/2004/02/skos/core#prefLabel", naceSparqlEndpoint));
	    set.add(new URIDescriptor("https://lod.stirdata.eu/nuts/", "http://www.w3.org/2004/02/skos/core#prefLabel", nutsSparqlEndpoint));

	    return set;
	}
}
