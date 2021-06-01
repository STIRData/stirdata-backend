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

    @Value("${czech.sparql.endpoint}")
    private String czechEndpoint;
    @Value("${czech.nuts.0}")
    private String czechNuts0;
    @Value("${belgium.sparql.endpoint}")
    private String belgiumEndpoint;
    @Value("${belgium.nuts.0}")
    private String belgiumNuts0;
    @Value("${norway.sparql.endpoint}")
    private String norwayEndpoint;
    @Value("${norway.nuts.0}")
    private String norwayNuts0;
    @Value("${sparql.endpoint.nuts}")
    private String nutsEndpoint;
    @Value("${sparql.endpoint.nace}")
    private String naceEndpoint;

    @Bean(name = "czech-sparql-endpoint")
    public SparqlEndpoint getCzechSparqlEndpoint() {

        logger.info("Creating czech endpoint...");
        return new SparqlEndpoint("czech-endpoint", czechEndpoint, czechNuts0);
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
