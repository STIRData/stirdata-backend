package com.ails.stirdatabackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.repository.CountriesRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;


@Service
public class DataService {

	@Autowired
	private Environment env;
	
	@Autowired
	ResourceLoader resourceLoader;
	
	@Autowired
	@Qualifier("model-configurations")
    private Map<String, ModelConfiguration> modelConfigurations;

	@Autowired
	@Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;

	@Autowired
	private CountriesRepository countriesRepository;

	@Autowired
	private StatisticsRepository statisticsRepository;

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	ApplicationContext context;
	
	public void loadCountry(String c, boolean stats, Code date) {

		Set<String> supportedModelUrls = new HashSet<>() ;
		Map<String, ModelConfiguration> mcUriMap = new HashMap<>() ;
		for (ModelConfiguration mc : modelConfigurations.values()) {
			supportedModelUrls.add(mc.getUrl());
			mcUriMap.put(mc.getUrl(), mc);
		}
		
		String defaultNaceEndpoint = env.getProperty("endpoint.nace.00");
		String defaultNutsEndpoint = env.getProperty("endpoint.nuts.00");

		String defaultNacePath1 = env.getProperty("nace.path-1.00");
		String defaultNacePath2 = env.getProperty("nace.path-2.00");
		String defaultNacePath3 = env.getProperty("nace.path-3.00");
		String defaultNacePath4 = env.getProperty("nace.path-4.00");
		String defaultNaceFixedLevel = env.getProperty("nace.fixed-level.00");
		
		String defaultNutsPrefix = env.getProperty("nuts.prefix.00");
		String defaultLauPrefix = env.getProperty("lau.prefix.00");
		
		// get entry from mongo to update if it exists
		CountryConfiguration cc;
		Optional<CountryConfiguration> ccOpt = countriesRepository.findOneByCountryCode(c);
		if (ccOpt.isPresent()) {
			cc = ccOpt.get();
		} else {
			cc = new CountryConfiguration(c);
			cc.setCountryLabel(env.getProperty("country.label." + c));
		}
		
		// read dcat
		String dcat = env.getProperty("country.dcat." + c);
		
		if (dcat != null) {
			processDcat(cc, dcat, supportedModelUrls, mcUriMap);
		}
		
//		for (String d : env.getProperty("country.dimensions." + c).split(",")) {
//			if (d.equalsIgnoreCase("nace")) {
//				cc.setNace(true);
//			} else if (d.equalsIgnoreCase("nuts")) {
//				cc.setNuts(true);
//			} else if (d.equalsIgnoreCase("lau")) {
//				cc.setLau(true);
//			} else if (d.equalsIgnoreCase("founding")) {
//				cc.setFoundingDate(true);
//			} else if (d.equalsIgnoreCase("dissolution")) {
//				cc.setDissolutionDate(true);
//			}
//		}
		
//		if (cc.getDataEndpoint() == null) {
//			String dataEndpoint = env.getProperty("endpoint.data." + c);
//			cc.setDataEndpoint(dataEndpoint);
//		}
		
		String naceEndpoint = env.getProperty("endpoint.nace." + c);
		cc.setNaceEndpoint(naceEndpoint != null ? naceEndpoint : defaultNaceEndpoint); 

		String nutsEndpoint = env.getProperty("endpoint.nuts." + c);
		cc.setNutsEndpoint(nutsEndpoint != null ? nutsEndpoint : defaultNutsEndpoint); 

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
	    
	    String nutsPrefix = env.getProperty("nuts.prefix." + c);
	    cc.setNutsPrefix(nutsPrefix != null ? nutsPrefix : defaultNutsPrefix);	

	    String lauPrefix = env.getProperty("lau.prefix." + c);
	    cc.setLauPrefix(lauPrefix != null ? lauPrefix : defaultLauPrefix);	

	    cc.setEntitySparql(env.getProperty("sparql.entity." + c));
    	cc.setLegalNameSparql(env.getProperty("sparql.legalName." + c));	
    	cc.setActiveSparql(env.getProperty("sparql.active." + c));
    	cc.setNuts3Sparql(env.getProperty("sparql.nuts3." + c));	
    	cc.setLauSparql(env.getProperty("sparql.lau." + c));	
    	cc.setNaceSparql(env.getProperty("sparql.nace." + c));	
    	cc.setFoundingDateSparql(env.getProperty("sparql.foundingDate." + c));
    	cc.setDissolutionDateSparql(env.getProperty("sparql.dissolutionDate." + c));
    	
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getNaceSparql() + " }")) {
			cc.setNace(qe.execAsk());
        }
        
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getNuts3Sparql() + " }")) {
			cc.setNuts(qe.execAsk());
        }
        
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getLauSparql() + " }")) {
			cc.setLau(qe.execAsk());
        }

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getFoundingDateSparql() + " }")) {
       		cc.setFoundingDate(qe.execAsk());
        }
        
        if (cc.isFoundingDate()) {
        	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT (MIN(?foundingDate) AS ?min) (MAX(?foundingDate) AS ?max) WHERE { " +  cc.getFoundingDateSparql() + " }")) {
           		ResultSet rs = qe.execSelect();
           		while (rs.hasNext()) {
           			QuerySolution qs = rs.next();
           			cc.setFoundingDateFrom(qs.get("min").asLiteral().getLexicalForm());
           			cc.setFoundingDateTo(qs.get("max").asLiteral().getLexicalForm());
           		}
            }	
        }

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getDissolutionDateSparql() + " }")) {
			cc.setDissolutionDate(qe.execAsk());
        }
        
        if (cc.isDissolutionDate()) {
        	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT (MIN(?dissolutionDate) AS ?min) (MAX(?dissolutionDate) AS ?max) WHERE { " +  cc.getDissolutionDateSparql() + " }")) {
           		ResultSet rs = qe.execSelect();
           		while (rs.hasNext()) {
           			QuerySolution qs = rs.next();
           			cc.setDissolutionDateFrom(qs.get("min").asLiteral().getLexicalForm());
           			cc.setDissolutionDateTo(qs.get("max").asLiteral().getLexicalForm());
           		}
            }	
        }
    	
        countryConfigurations.put(cc.getCountryCode(), cc);
    	
        if (stats) {
	    	statisticsService.computeAndSaveAllStatistics(cc, date, true, cc.isNuts(), cc.isNace(), cc.isFoundingDate(), cc.isDissolutionDate(), cc.isNace() && cc.isNuts());
	    	
			List<Statistic> list = statisticsRepository.findByCountryAndDimension(cc.getCountryCode(), Dimension.DATA);
			if (!list.isEmpty()) {
				cc.setLegalEntityCount(list.get(0).getCount());
			}
        }
		
    	countriesRepository.save(cc);
	}
	
	private void processDcat(CountryConfiguration cc, String dcat, Set<String> supportedModelUrls, Map<String, ModelConfiguration> mcUriMap) {

		Model model = null;
		if (dcat.startsWith("http")) {
			model = RDFDataMgr.loadModel(dcat);
		} else {
			try (InputStream in = resourceLoader.getResource("classpath:" + dcat).getInputStream()) {
				model = ModelFactory.createDefaultModel();
				RDFDataMgr.read(model, in, Lang.JSONLD);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String sparql;

		sparql = 
				"SELECT ?conformsTo ?issued ?modified WHERE { " + 
		        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
		        "  ?dcat <" + DCTVocabulary.conformsTo + "> ?conformsTo . " +
		        "} ";


		String conformsTo;
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next(); 
        		conformsTo = sol.get("conformsTo").toString();
        		if (supportedModelUrls.contains(conformsTo)) {
        			cc.setConformsTo(conformsTo);
        			cc.setModelConfiguration(mcUriMap.get(conformsTo));
        			break;
        		}
        	}
        }

        if (cc.getModelConfiguration() == null) {
        	return;
        }
        
		sparql = 
				"SELECT ?issued ?modified ?accrualPeriodicity WHERE { " + 
		        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.issued + "> ?issued } . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.modified + "> ?modified } . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.accrualPeriodicity + "> ?accrualPeriodicity } . " +
		        "} ";

        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		String lastUpdated = null;
        		
        		QuerySolution sol = rs.next(); 
        		if (sol.get("issued") != null) {
        			lastUpdated = sol.get("issued").asLiteral().getLexicalForm();
        		}
        		
        		if (sol.get("modified") != null) {
        			lastUpdated = sol.get("modified").asLiteral().getLexicalForm();
        		}
        		
        		if (lastUpdated != null) {
        			try {
        				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        				cc.setLastUpdated(format.parse(lastUpdated));
        			} catch (ParseException ex) {
        				ex.printStackTrace();
        			}
        			
        			
        		}
        		
        		if (sol.get("accrualPeriodicity") != null) {
        			cc.setAccrualPeriodicity(sol.get("accrualPeriodicity").asResource().toString());
        		}
        		
       			break;
        	}
        }

        
		sparql = 
				"SELECT ?endpoint WHERE { " + 
		        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
		        "  ?dcat <" + DCATVocabulary.distribution + "> ?distribution . " +
		        "  ?distribution <" + DCATVocabulary.accessService + "> ?service . " +
		        "  ?service a <" + DCATVocabulary.DataService + "> . " +
		        "  ?service <" + DCTVocabulary.conformsTo + "> <https://www.w3.org/TR/sparql11-protocol/> . " +
		        "  ?service <" + DCATVocabulary.endpointURL + "> ?endpoint . " +
		        "} ";
		
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next(); 
        		String endpoint = sol.get("endpoint").toString();
        		String[] parts = endpoint.split("://");
        		cc.setDataEndpoint(parts[0] + "://" + IDN.toASCII(parts[1]));
        		break;
        	}
        }
	}
	
	
}
