package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    private final static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Value("${sparql.endpoint.czech}")
    private String czechEndpoint;

    @Value("${sparql.endpoint.belgium}")
    private String belgiumEndpoint;

    @Bean(name = "czech-sparql-endpoint")
    public SparqlEndpoint getCzechSparqlEndpoint() {
        logger.info("creating czech endpoint...");
        return new SparqlEndpoint("czech-endpoint", czechEndpoint);
    }

    @Bean(name = "belgium-sparql-endpoint")
    public SparqlEndpoint getBelgiumSparqlEndpoint() {
        logger.info("creating belgium endpoint...");
        return new SparqlEndpoint("belgium-endpoint", belgiumEndpoint);
    }
}
