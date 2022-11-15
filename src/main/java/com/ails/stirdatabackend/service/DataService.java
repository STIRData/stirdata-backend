package com.ails.stirdatabackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.IDN;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import org.apache.jena.riot.RDFFormat.JSONLDVariant;

import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.payload.Resource;
import com.ails.stirdatabackend.repository.CountriesRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdError;

@Service
public class DataService {

	@Autowired
	ResourceLoader resourceLoader;
	
	@Autowired
	@Qualifier("model-configurations")
    private Map<String, ModelConfiguration> modelConfigurations;

	@Autowired
	@Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;

	@Autowired
	@Qualifier("model-jsonld-context")
    private JsonLDWriteContext context;
	
	public CountryDB findCountry (String uri) {
		for (CountryDB cc : countryConfigurations.values()) {
			if (cc.getLegalEntityPrefix() != null && uri.startsWith(cc.getLegalEntityPrefix())) {
				return cc;
			}
		}
		
		return null;
	}

//	public getEntity(String uri, String country) {
//		
//	}
	
	public String getEntity(String uri) {
		return getEntity(uri, findCountry(uri));
	}
	
	public String getEntity(String uri, String country) {
		return getEntity(uri, countryConfigurations.get(country));
	}
	
	public String getEntity(String uri, CountryDB cc) {

        String sparqlConstruct = 
        		"CONSTRUCT { " + 
                "  ?entity a <http://www.w3.org/ns/legal#LegalEntity> . " + 
                "  ?entity <http://www.w3.org/ns/legal#legalName> ?entityName . " +
                "  ?entity <http://www.w3.org/ns/legal#companyActivity> ?nace . " +
        		"  ?entity <http://www.w3.org/ns/legal#registeredAddress> ?address . ?address ?ap ?ao . " + 
                "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	            		
        		" WHERE { " +
                cc.getEntitySparql() + " " +
                cc.getLegalNameSparql() + " " + 
                (cc.isDissolutionDate() ? cc.getActiveSparql() : "") + " " +
                "OPTIONAL { " + cc.getAddressSparql() + " ?address ?ap ?ao . } " + 
	            "OPTIONAL { " + cc.getNaceSparql() + " } " +
                "OPTIONAL { " + cc.getFoundingDateSparql() + " } " +
                "VALUES ?entity { <" + uri + "> } } ";

        
        ObjectMapper objectMapper = new ObjectMapper();

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
            Model model = qe.execConstruct();
//          StringWriter sw = new StringWriter();
//          JsonLDWriter writer = new JsonLDWriter(RDFFormat.JSONLD_COMPACT_PRETTY);
//          writer.write(sw, DatasetFactory.wrap(model).asDatasetGraph(), null, null, context);
            
            Map<String,Object> jn = (HashMap)JsonLDWriter.toJsonLDJavaAPI((RDFFormat.JSONLDVariant)RDFFormat.JSONLD_COMPACT_PRETTY.getVariant(), DatasetFactory.wrap(model).asDatasetGraph(), null, null, context);
            jn.put("@context", "https://dev.stirdata.eu/api/data/context/stirdata.jsonld");
            
            StringWriter sw2 = new StringWriter();
            objectMapper.writeValue(sw2, jn);
            
            return sw2.toString();
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return null;
	}
	
	public void precompute(String[] countries) {
		
		for (String s : countries) {
			
			CountryDB cc = countryConfigurations.get(s);
			System.out.println(s);
			
			if (cc.isNace()) {
				String sparql = "SELECT (COUNT(DISTINCT ?entity) AS ?count) ?nace WHERE { " + cc.getNaceSparql() + " } GROUP BY ?nace ";
				
		        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), sparql)) {
		        	ResultSet rs = qe.execSelect();
		        	
		        	while (rs.hasNext()) {
		        		QuerySolution sol = rs.next();
		        		System.out.println(sol.get("nace") + " " + sol.get("count"));
		        	}
		        }
	
			} 
			
			if (cc.isNuts()) {
				String sparql = "SELECT (COUNT(DISTINCT ?entity) AS ?count) ?nuts3 WHERE { " + cc.getNuts3Sparql() + " } GROUP BY ?nuts3 ";
				
				try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), sparql)) {
		        	ResultSet rs = qe.execSelect();
		        	
		        	while (rs.hasNext()) {
		        		QuerySolution sol = rs.next();
		        		System.out.println(sol.get("nuts3") + " " + sol.get("count"));
		        	}
		        }
			}
			
			if (cc.isNuts()) {
				String sparql = "SELECT (COUNT(DISTINCT ?entity) AS ?count) ?lau WHERE { " + cc.getLauSparql() + " } GROUP BY ?lau ";
				
				try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), sparql)) {
		        	ResultSet rs = qe.execSelect();
		        	
		        	while (rs.hasNext()) {
		        		QuerySolution sol = rs.next();
		        		System.out.println(sol.get("lau") + " " + sol.get("count"));
		        	}
		        }
			}
		}
	}
	
}
