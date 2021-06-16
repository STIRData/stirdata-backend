package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.model.EndpointManager;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparqlEndpointConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(SparqlEndpointConfiguration.class);

    @Value("${czechia.sparql.endpoint}")
    private String czechiaEndpoint;
    @Value("${czechia.nuts.0}")
    private String czechiaNuts0;
    
    @Value("${belgium.sparql.endpoint}")
    private String belgiumEndpoint;
    @Value("${belgium.nuts.0}")
    private String belgiumNuts0;
    
    @Value("${norway.sparql.endpoint}")
    private String norwayEndpoint;
    @Value("${norway.nuts.0}")
    private String norwayNuts0;

    @Value("${greece.sparql.endpoint}")
    private String greeceEndpoint;
    @Value("${greece.nuts.0}")
    private String greeceNuts0;

    @Value("${sparql.endpoint.nuts}")
    private String nutsEndpoint;
    @Value("${sparql.endpoint.nace}")
    private String naceEndpoint;

    @Bean(name = "czechia-sparql-endpoint")
    public SparqlEndpoint getCzechiaSparqlEndpoint() {
        logger.info("Creating czechia endpoint...");
        return new SparqlEndpoint("czechia-endpoint", czechiaEndpoint, czechiaNuts0);
    }

    @Bean(name = "belgium-sparql-endpoint")
    public SparqlEndpoint getBelgiumSparqlEndpoint() {
        logger.info("Creating belgium endpoint...");
        return new SparqlEndpoint("belgium-endpoint", belgiumEndpoint, belgiumNuts0);
    }

    @Bean(name = "norway-sparql-endpoint")
    public SparqlEndpoint getNorwaySparqlEndpoint() {
        logger.info("Creating norway endpoint...");
        return new SparqlEndpoint("norway-endpoint", norwayEndpoint, norwayNuts0);
    }

    @Bean(name = "greece-sparql-endpoint")
    public SparqlEndpoint getGreeceSparqlEndpoint() {
        logger.info("Creating greece endpoint...");
        return new SparqlEndpoint("greece-endpoint", greeceEndpoint, greeceNuts0);
    }
    
    @Bean(name = "nuts-sparql-endpoint")
    public SparqlEndpoint getNutsSparqlEndpoint() {
        logger.info("Creating NUTS endpoint...");
        return new SparqlEndpoint("nuts-endpoint", nutsEndpoint, null);
    }

    @Bean(name = "nace-sparql-endpoint")
    public SparqlEndpoint getNaceSparqlEndpoint() {
        logger.info("Creating NACE endpoint...");
        return new SparqlEndpoint("nace-endpoint", naceEndpoint, null);
    }

    @Bean
    public EndpointManager getEndpointManager() {
        return new EndpointManager();
    }
}
