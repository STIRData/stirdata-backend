package com.ails.stirdatabackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.payload.Resource;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;
import com.ails.stirdatabackend.vocs.SDVocabulary;

import antlr.CSharpCodeGenerator;

@Transactional
@Service
public class CountriesService {
	
	Logger logger = LoggerFactory.getLogger(CountriesService.class);

//	@Value("${endpoint.nace.00}")
//	String defaultNaceEndpoint;
	
	@Value("${endpoint.nuts.00}")
	String defaultNutsEndpoint;

	@Value("${nace.path-1.00}")
	String defaultNacePath1;
	
	@Value("${nace.path-2.00}")
	String defaultNacePath2;
	
	@Value("${nace.path-3.00}")
	String defaultNacePath3;
	
	@Value("${nace.path-4.00}")
	String defaultNacePath4;
	
	@Value("${nace.fixed-level.00}")
	String defaultNaceFixedLevel;
	
	@Value("${nuts.prefix.00}")
	String defaultNutsPrefix;
	
	@Value("${lau.prefix.00}")
	String defaultLauPrefix;
	
	@Autowired
	@Qualifier("model-configurations")
    private Map<String, ModelConfiguration> modelConfigurations;
	
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;	
	
    @Autowired
    private CountriesDBRepository countriesRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;
    
    @Autowired
    private StatisticsService statisticsService;
    
    
	public void reload() {
		Map<String, CountryDB> countryConfigurations = new HashMap<>();
		
		System.out.println("LOADING COUNTRIES: ");
		String s = "";
		for (CountryDB cc  : countriesRepository.findAll()) {
			cc.setModelConfiguration(modelConfigurations.get(cc.getConformsTo()));
//			cc.setStatistics(new HashSet<>(statisticsRepository.findDimensionsByCountry(cc.getCode())));
			
			s += cc.getCode() + " ";
			countryConfigurations.put(cc.getCode(), cc);
		}
		logger.info("Loaded countries: " + s);
	}
	
    public boolean replace(Country country)  {
		logger.info("Replacing country " + country.getCode() + ".");
		
		// needs lock
		CountryDB cc = countriesRepository.findByCode(country.getCode());
    	if (cc == null) {
    		cc = new CountryDB();
    	}
    	
    	return makeCountry(cc, country);
    }
    
	public boolean add(Country country)  {
		logger.info("Adding country " + country.getCode() + ".");
		
		// needs lock
		CountryDB cc = countriesRepository.findByCode(country.getCode());
    	if (cc != null) {
    		return false;
    	}
    	
    	cc = new CountryDB();
    	
    	return makeCountry(cc, country);
	}
	
    private boolean makeCountry(CountryDB cc, Country country)  {    	
    	
    	cc.setCode(country.getCode());
    	cc.setLabel(country.getLabel());
    	cc.setDcat(country.getDcat());
    	cc.setNaceScheme(country.getNaceScheme());
    	cc.setNaceNamespace(country.getNaceNamespace());
    	cc.setNaceNamedGraph(country.getNaceNamedGraph());
    	cc.setNutsEndpoint(country.getNutsEndpoint());
    	cc.setNutsNamedGraph(country.getNutsNamedGraph());
    	cc.setDataNamedGraph(country.getDataNamedGraph());
    	cc.setEntitySparql(country.getEntitySparql());
    	cc.setLegalNameSparql(country.getLegalNameSparql());
    	cc.setActiveSparql(country.getActiveSparql());
    	cc.setAddressSparql(country.getAddressSparql());
    	cc.setNuts3Sparql(country.getNuts3Sparql());
    	cc.setLauSparql(country.getLauSparql());
    	cc.setNaceSparql(country.getNaceSparql());
    	cc.setFoundingDateSparql(country.getFoundingDateSparql());
    	cc.setDissolutionDateSparql(country.getDissolutionDateSparql());

//		cc.setNaceEndpoint(country.getNaceEnpoint() != null ? country.getNaceEnpoint() : defaultNaceEndpoint); 
    	cc.setNaceEndpoint(country.getNaceEnpoint());
		
		if (cc.getNaceEndpoint() != null) {
		    cc.setNacePath1(country.getNacePath1() != null || country.getNacePathSparql() != null ? country.getNacePath1() : defaultNacePath1);
		    cc.setNacePath2(country.getNacePath2() != null || country.getNacePathSparql() != null  ? country.getNacePath2() : defaultNacePath2);
		    cc.setNacePath3(country.getNacePath3() != null || country.getNacePathSparql() != null ? country.getNacePath3() : defaultNacePath3);
		    cc.setNacePath4(country.getNacePath4() != null || country.getNacePathSparql() != null ? country.getNacePath4() : defaultNacePath4);
	
	//	    cc.setNaceFixedLevel(country.getNaceFixedLevel() != null || country.getNacePathSparql() != null  ? country.getNaceFixedLevel() : Integer.parseInt(defaultNaceFixedLevel));
	//		why the above gives exception????
		    if (country.getNaceFixedLevel() != null || country.getNacePathSparql() != null) { 
		    	if ( country.getNaceFixedLevel() != null) {
		    		cc.setNaceFixedLevel(country.getNaceFixedLevel());
		    	}
		    } else {
		    	cc.setNaceFixedLevel(Integer.parseInt(defaultNaceFixedLevel));
		    }
		} else {
			cc.setNacePath1(null);
			cc.setNacePath2(null);
			cc.setNacePath3(null);
			cc.setNacePath4(null);
			cc.setNaceFixedLevel(null);
		}
		
		cc.setNutsEndpoint(country.getNutsEndpoint() != null ? country.getNutsEndpoint() : defaultNutsEndpoint); 

	    cc.setNutsPrefix(country.getNutsPrefix() != null ? country.getNutsPrefix() : defaultNutsPrefix);	
	    cc.setLauPrefix(country.getLauPrefix() != null ? country.getLauPrefix() : defaultLauPrefix);	

	    // if null > get from model 
	    cc.setEntitySparql(country.getEntitySparql());
    	cc.setLegalNameSparql(country.getLegalNameSparql());	
    	cc.setActiveSparql(country.getActiveSparql());
    	cc.setNuts3Sparql(country.getNuts3Sparql());	
    	cc.setLauSparql(country.getLauSparql());	
    	cc.setNaceSparql(country.getNaceSparql());	
    	cc.setNacePathSparql(country.getNacePathSparql());
    	cc.setFoundingDateSparql(country.getFoundingDateSparql());
    	cc.setDissolutionDateSparql(country.getDissolutionDateSparql());

//    	cc.setLegalEntityPrefix(country.getLegalEntityPrefix());
    	
    	cc.setLicenseLabel(country.getLicenceLabel());
    	cc.setLicenseUri(country.getLicenceUri());
    	
    	if (cc.getDcat() == null) {
    		return false;
    	}
    	
    	processDcat(cc, modelConfigurations);
    	
    	ModelConfiguration mc = cc.getModelConfiguration();
    	if (mc == null) {
    		return false;
    	}
    	
    	cc.setLastAccessedStart(new Date());
    	
    	loadCountry(cc);
    	
//    	System.out.println(cc.getNaceNamespace());
    	
    	countriesRepository.save(cc);
    	
    	countryConfigurations.put(cc.getCode(), cc);
    	
    	logger.info("Country " + country.getCode() + " added.");
    	
        return true;
    }
	
	private void processDcat(CountryDB cc, Map<String, ModelConfiguration> modelConfigurations) {

		Model model = RDFDataMgr.loadModel(cc.getDcat());

		String sparql;

		sparql = 
				"SELECT ?conformsTo ?issued ?modified ?source ?accrualPeriodicity WHERE { " + 
		        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
		        "  ?dcat <" + DCTVocabulary.conformsTo + "> ?conformsTo . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.source + "> ?source } . " +						
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.issued + "> ?issued } . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.modified + "> ?modified } . " +
		        "  OPTIONAL { ?dcat <" + DCTVocabulary.accrualPeriodicity + "> ?accrualPeriodicity } . " +
		        "} ";


		String conformsTo;
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, model)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next(); 
        		conformsTo = sol.get("conformsTo").toString();
        		
        		ModelConfiguration mc = modelConfigurations.get(conformsTo);
        		cc.setConformsTo(conformsTo);
       			cc.setModelConfiguration(mc);

        		String lastUpdated = null;
        		
        		if (sol.get("source") != null && sol.get("source").isResource()) {
        			String uri = sol.get("source").asResource().toString();
        			String label = getPageTitle(uri);
        			
        			cc.setSourceUri(uri);
        			cc.setSourceLabel(label);
        		}
        		
        		if (sol.get("issued") != null) {
        			lastUpdated = sol.get("issued").asLiteral().getLexicalForm();
        		}
        		
        		if (sol.get("modified") != null) {
        			lastUpdated = sol.get("modified").asLiteral().getLexicalForm();
        		}
        		
        		if (lastUpdated != null) {
        			try {
        				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
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

	
	public void loadCountry(CountryDB cc) {
//		System.out.println("Loading " + c);
		
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getNaceSparql() + " }")) {
			cc.setNace(qe.execAsk());
        }
        
        if (cc.isNace() && cc.getNaceEndpoint() != null) {
        	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (MAX(?level) AS ?maxLevel) WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> . ?nace <" + SDVocabulary.level + "> ?level }")) {
        		
           		ResultSet rs = qe.execSelect();
           		while (rs.hasNext()) {
           			QuerySolution qs = rs.next();
           			cc.setNaceLevels((qs.get("maxLevel").asLiteral().getInt()));
           		}
            }	
        }

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getLegalNameSparql() + " }")) {
			cc.setLegalName(qe.execAsk());
        }

        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getTradingNameSparql() + " }")) {
			cc.setTradingName(qe.execAsk());
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
        
        
        // guess entity prefix
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT (COUNT(?entity) AS ?count) WHERE { " +  cc.getEntitySparql()  + " }")) {
       		ResultSet rs = qe.execSelect();
       		while (rs.hasNext()) {
       			QuerySolution qs = rs.next();
       			cc.setTotalLegalEntityCount(qs.get("count").asLiteral().getInt());
            }
        }

   		List<String> entityUris = new ArrayList<>();       		

       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT ?entity WHERE { " +  cc.getEntitySparql()  + " } LIMIT 50")) {
       		
       		ResultSet rs = qe.execSelect();
       		while (rs.hasNext()) {
       			QuerySolution qs = rs.next();
       			entityUris.add(qs.get("entity").toString());
            }
        }

       	
//       	System.out.println(cc.getDataEndpoint());
//       	System.out.println("SELECT ?entity WHERE { " +  cc.getEntitySparql()  + " } LIMIT 50");

       	if (cc.getTotalLegalEntityCount() - 50 > 0 ) {
	       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT ?entity WHERE { " +  cc.getEntitySparql()  + " } LIMIT 50 OFFSET " + (cc.getTotalLegalEntityCount() - 50) )) {
	       		
	       		ResultSet rs = qe.execSelect();
	       		while (rs.hasNext()) {
	       			QuerySolution qs = rs.next();
	       			entityUris.add(qs.get("entity").toString());
	            }
	       		
	        }
       	}
       	
//       	System.out.println(entityUris);
       	
   		String entityPrefix = StringUtils.getCommonPrefix(entityUris.toArray(new String[] {}));
   		
   		while (entityPrefix.charAt(entityPrefix.length() - 1) != '/' && entityPrefix.charAt(entityPrefix.length() - 1) != '#') {
   			entityPrefix = entityPrefix.substring(0, entityPrefix.length() - 1);
   		}
   		
   		cc.setLegalEntityPrefix(entityPrefix);

   		// guess nace prefix
   		if (cc.isNace() && cc.getNaceEndpoint() != null) {
   			int totalNace = 0;
   		
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (count(?nace) AS ?count) WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> . }")) {
	       		ResultSet rs = qe.execSelect();
	       		while (rs.hasNext()) {
	       			QuerySolution qs = rs.next();
	       			totalNace = qs.get("count").asLiteral().getInt();
	            }
	        }
	
	   		List<String> naceUris = new ArrayList<>();       		
	
	       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50")) {
	       		
	       		ResultSet rs = qe.execSelect();
	       		while (rs.hasNext()) {
	       			QuerySolution qs = rs.next();
	       			naceUris.add(qs.get("nace").toString());
	            }
	       		
	        }
	
	       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50 OFFSET " + (totalNace - 50))) {
	       		
	       		ResultSet rs = qe.execSelect();
	       		while (rs.hasNext()) {
	       			QuerySolution qs = rs.next();
	       			naceUris.add(qs.get("nace").toString());
	            }
	       		
	        }
	
	   		String nacePrefix = StringUtils.getCommonPrefix(naceUris.toArray(new String[] {}));
	   		
	   		while (nacePrefix.charAt(nacePrefix.length() - 1) != '/' && nacePrefix.charAt(nacePrefix.length() - 1) != '#') {
	   			nacePrefix = nacePrefix.substring(0, nacePrefix.length() - 1);
	   		}
	   		
	   		cc.setNacePrefix(nacePrefix);
   		}   		
	}
	
	
	private static String getPageTitle(String uri) {
	    Document document;
	    try {
			document = Jsoup.connect(uri).get();
		 
			//Get title from document object.
			return document.title();
		 
	    } catch (Exception e) {
	    	System.out.println("Access failed: " + uri);
	    	e.printStackTrace();
	    }
	    
	    return null;
	}	
}
