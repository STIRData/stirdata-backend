package com.ails.stirdatabackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.ails.stirdatabackend.StirdataBackendApplication;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.LogActionType;
import com.ails.stirdatabackend.model.LogState;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.UpdateLog;
import com.ails.stirdatabackend.model.UpdateLogAction;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.StatisticsRepository;
import com.ails.stirdatabackend.repository.UpdateLogRepository;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;
import com.ails.stirdatabackend.vocs.SDVocabulary;
import com.ails.stirdatabackend.vocs.SKOSVocabulary;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
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
	
//	@Value("${nuts.prefix.00}")
//	String defaultNutsPrefix;
	
	@Value("${lau.prefix.00}")
	String defaultLauPrefix;
	
	@Autowired
	@Qualifier("model-configurations")
    private Map<String, ModelConfiguration> modelConfigurations;
	
    @Autowired
    @Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;	
	
    @Autowired
    private CountriesDBRepository countriesRepository;

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private UpdateLogRepository updateLogRepository;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private DataStoring ds;
    
    
	@Autowired
	private Environment env;
    
	public static String fixDcat(String dcat) {
		if (dcat == null) {
			return null;
		}
		
		if (dcat.startsWith("https://data.europa.eu/88u/dataset/")) {
			return "https://data.europa.eu/api/hub/repo/datasets/" + dcat.substring("https://data.europa.eu/88u/dataset/".length()) + "?useNormalizedId=true";
		} else if (dcat.startsWith("http://data.europa.eu/88u/dataset/")) {
				return "http://data.europa.eu/api/hub/repo/datasets/" + dcat.substring("http://data.europa.eu/88u/dataset/".length()) + "?useNormalizedId=true";
		} else {
			return dcat;
		}
	}
	
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
	
	public CountryDB updateCountry(Country country) {
		UpdateLog log = new UpdateLog();
		
		log.setType(LogActionType.RELOAD_COUNTRY);
		log.setDcat(country.getDcat());

		updateLogRepository.save(log);
		
		CountryDB cc = replace(country, log);
		
		if (log.getState() == LogState.RUNNING) {
			log.completed();
			updateLogRepository.save(log);
		}
		
		if (cc != null) {
			countriesRepository.save(cc);
			return cc;
		} else {
			return null;
		}
	}
	
//	public CountryDB replace(Country country)  {
//		return replace(country, null);
//	}
	
    public CountryDB replace(Country country, UpdateLog log)  {
		logger.info("Checking country for " + country.getDcat() + ".");
		
		// needs lock
//		CountryDB cc = countriesRepository.findByCode(country.getCode());
//    	if (cc == null) {
//    		cc = new CountryDB();
//    	}
    	
    	return makeCountry(country, log);
    }
	
    private CountryDB makeCountry(Country country, UpdateLog log)  {    	
    	
    	CountryDB cc;
    	
    	if (country.getDcat() == null) {
    		return null;
    	}

    	UpdateLogAction action = null;
    	
    	if (log != null) {
    		action = new UpdateLogAction(LogActionType.READ_REGISTRY_DCAT);
    		log.addAction(action);
    		updateLogRepository.save(log);
    	}

		String conformsTo = null;
        String dataEndpoint = null;
        String countryLabel = null;
        String countryCode = null;
        Date lastUpdatedDate = null;
        String sourceUri = null;
        String licenseUri = null;
        String naceDataset = null;

        ModelConfiguration mc = null;

    	try {
	    	Model dcatModel = RDFDataMgr.loadModel(fixDcat(country.getDcat()));
	    	
	    	String sparql =
	    		"SELECT ?conformsTo WHERE { " + 
			        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
			        "  ?dcat <" + DCTVocabulary.conformsTo + "> ?conformsTo . " +
			        "} ";
			
	        try (QueryExecution qe = QueryExecutionFactory.create(sparql, dcatModel)) {
	        	ResultSet rs = qe.execSelect();
	        	
	        	while (rs.hasNext()) {
	        		QuerySolution sol = rs.next(); 
	        		conformsTo = sol.get("conformsTo").toString();
	        		
	        		mc = modelConfigurations.get(conformsTo);
	        		if (mc != null) {
	                	break;
	                }
	        	}
	        }
	        
	    	if (mc == null) {
	        	throw new Exception("Invalid conformsTo value: " + conformsTo);
	        }        
	        
	        dataEndpoint = readEndpointFromDCat(dcatModel);

	    	if (dataEndpoint == null) {
	        	throw new Exception("Could not detect data endpoint");
	        }        
	    	
	        if (log != null) {
	        	action.completed();
	        	updateLogRepository.save(log);
	        }
	        
    	} catch (Exception ex) {
    		if (log != null) {
	    		action.failed(ex.getMessage());
	    		log.failed();
	    		updateLogRepository.save(log);
    		}
    		
    		return null;
    	}
    
        
    	try {
    		
        	if (log != null) {
        		action = new UpdateLogAction(LogActionType.READ_DATASET_METADATA);
        		log.addAction(action);
        		updateLogRepository.save(log);
        	}
    	
    		String sparql = 
	            	"SELECT ?spatial ?modified ?license ?source ?naceDataset WHERE { " +
	            	"  ?dataset <" + DCTVocabulary.spatial + "> ?spatial . " +
	            	"  OPTIONAL { ?dataset <" + DCTVocabulary.modified + "> ?modified . } " +
	            	"  OPTIONAL { ?dataset <" + DCTVocabulary.source + "> ?source . } " +
	            	"  OPTIONAL { ?source  <" + DCTVocabulary.license + "> ?license . } " +
	            	"  OPTIONAL { ?dataset <" + SDVocabulary.naceDataset + "> ?naceDataset . } " +
	    	        "} " ;
	            		
	
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(dataEndpoint, sparql)) {
	        	ResultSet rs = qe.execSelect();
	        	
	        	while (rs.hasNext()) {
	        		QuerySolution sol = rs.next();
	        		
	        		if (sol.get("spatial") != null) {
	        			String spatial = sol.get("spatial").asResource().toString();
	        			
	        			countryCode = StirdataBackendApplication.isoCountryMap.get(spatial.substring(spatial.lastIndexOf("/") + 1));
	        			
	        			if (countryCode == null) {
	        				throw new Exception("Could not detect country of dataset");
	        			}
	        			
	        			Model imodel = ModelFactory.createDefaultModel();
	        			RDFDataMgr.read(imodel, spatial, Lang.RDFXML); // content negotiation not supported 
	        			
	        			String isparql = "SELECT ?label WHERE { <" + spatial + "> <" + SKOSVocabulary.prefLabel + "> ?label . FILTER(lang(?label) = \"en\") } " ;
	        			
	        			try (QueryExecution iqe = QueryExecutionFactory.create(isparql, imodel)) {
	        	        	ResultSet irs = iqe.execSelect();
	        	        	
	        	        	while (irs.hasNext()) {
	        	        		QuerySolution isol = irs.next();
	        	        		
	        	        		if (isol.get("label") != null) {
	        	        			countryLabel = isol.get("label").asLiteral().getLexicalForm(); 
	        	        		}
	        	        	}
	        			}
	        		}
	        		
	        		if (sol.get("modified") != null) {
	        			String lastUpdated = sol.get("modified").asLiteral().getLexicalForm();
	
	        			try {
	        				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	        				lastUpdatedDate = format.parse(lastUpdated);
	        			} catch (ParseException ex) {
	        				ex.printStackTrace();
	        			}
	        		}
	        		
	        		if (sol.get("source") != null && sol.get("source").isResource()) {
	        			sourceUri = sol.get("source").asResource().toString();
	        		}
	        		
	        		if (sol.get("license") != null && sol.get("license").isResource()) {
	        			licenseUri = sol.get("license").asResource().toString();
	        		}
	
	        		if (sol.get("naceDataset") != null && sol.get("naceDataset").isResource()) {
	        			naceDataset = sol.get("naceDataset").asResource().toString();
	        		}
	
	       			break;
	        	}
	        }
	        
//	        System.out.println(">> New dataset data ");
//	        System.out.println("CT: " + conformsTo);
//	        System.out.println("DE: " + dataEndpoint);
//	        System.out.println("CL: " + countryLabel);
//	        System.out.println("CC: " + countryCode);
//	        System.out.println("MD: " + lastUpdatedDate);
//	        System.out.println("SU: " + sourceUri);
//	        System.out.println("LU: " + licenseUri);
//	        System.out.println("ND: " + naceDataset);
//	        System.out.println("<<");
	            
			cc = countriesRepository.findByCode(countryCode);
	
	    	boolean changed = false;
	    	
			if (cc == null) {
	    		cc = new CountryDB();
	    		cc.setCode(countryCode);
	    		cc.setLabel(countryLabel);
	    		cc.setDcat(country.getDcat());
	    		
	    		if (log != null) {
	    			action.setMessage("Created new country " + cc.getCode());
	    		}
	    	}

	    	cc.setLastAccessed(new Date());

//			System.out.println(lastUpdatedDate  + " " + cc.getLastUpdated() + " " + changed(lastUpdatedDate, cc.getLastUpdated()));
			
			if (changed(lastUpdatedDate, cc.getLastUpdated())) {
				changed = true;
				cc.setLastUpdated(lastUpdatedDate);
			}
			
			if (!changed) {
		   		if (log != null) {
		    		action.completed("Country " + cc.getCode() + " has not been modified");
		    		log.completed();
		    		updateLogRepository.save(log);
	    		}
		   		
		   		logger.info(countryLabel  + " has not changed.");
		   		
		   		return cc;
			}
			
			logger.info(countryLabel  + " has changed. Reloading.");
			
			cc.modified = true;
			
//			System.out.println(countryLabel  + " " + cc.getLabel() + " " + changed(countryLabel, cc.getLabel()));
			
			if (changed(countryLabel, cc.getLabel())) {
	    		cc.setLabel(countryLabel);
			}
	
//			System.out.println(country.getDcat()  + " " + cc.getDcat() + " " + changed(country.getDcat(), cc.getDcat()));
			
			if (changed(country.getDcat(), cc.getDcat())) {
	    		cc.setDcat(country.getDcat());
			}
			
//			System.out.println(conformsTo  + " " + cc.getConformsTo() + " " + changed(conformsTo, cc.getConformsTo()));
			
			if (changed(conformsTo, cc.getConformsTo())) {
	    		cc.setConformsTo(conformsTo);
			}
			
			cc.setModelConfiguration(mc);
	
//			System.out.println(dataEndpoint  + " " + cc.getDataEndpoint() + " " + changed(dataEndpoint, cc.getDataEndpoint()));
			
			if (changed(dataEndpoint, cc.getDataEndpoint())) {
				cc.setDataEndpoint(dataEndpoint);
			}
	  	
//			System.out.println(sourceUri  + " " + cc.getSourceUri() + " " + changed(sourceUri, cc.getSourceUri()));
	
			if (changed(sourceUri, cc.getSourceUri())) {
				cc.setSourceUri(sourceUri);
	
				String sourceLabel = null;
				if (sourceUri != null) {
					sourceLabel = getPageTitle(sourceUri);
				}
				
				cc.setSourceLabel(sourceLabel);
			}
			
//			System.out.println(licenseUri  + " " + cc.getLicenseUri() + " " + changed(licenseUri, cc.getLicenseUri()));
	
			String licenseLabel = null;
			
			if (changed(licenseUri, cc.getLicenseUri())) {
	    		cc.setLicenseUri(licenseUri);
	    		
	    		if (licenseUri != null) {
		    		if (licenseUri.startsWith("https://creativecommons.org/licenses/")) {
		    			Model licenseModel = RDFDataMgr.loadModel(licenseUri + "/rdf"); // content negotiation not supported
		    			
		    			String licenseSparql = "SELECT ?identifier ?version { ?p <http://purl.org/dc/elements/1.1/identifier> ?identifier . ?p <http://purl.org/dc/terms/hasVersion> ?version } ";
		    			try (QueryExecution qe = QueryExecutionFactory.create(licenseSparql, licenseModel)) {
		    				ResultSet rs = qe.execSelect();
		    				if (rs.hasNext()) {
		    					QuerySolution qs = rs.next();
		    					
		    				    licenseLabel = qs.get("identifier").toString().toUpperCase() + " " + qs.get("version");
		    				}
		    			}
		    		} else {
		    			licenseLabel = getPageTitle(licenseUri);
		    		}
	    		}
	    		
	    		cc.setSourceLabel(licenseLabel);
	    		
//	    		System.out.println("LL: " + licenseLabel);

	 		}
			
			if (log != null) {
	    		action.completed();
	    		updateLogRepository.save(log);
	    	}
			
    	} catch (Exception ex) {

    		if (log != null) {
	    		action.failed(ex.getMessage());
	    		log.failed();
	    		updateLogRepository.save(log);
			}
    		
    		return null;
    	}

    	
		String naceEndpoint = null;
		
		if (naceDataset != null) {
	    	if (log != null) {
	    		action = new UpdateLogAction(LogActionType.READ_NACE_DCAT);
	    		log.addAction(action);
	    		updateLogRepository.save(log);
	    	}

	    	try {
	    		System.out.println("LOADING " + naceDataset);
		    	Model naceDcatModel = RDFDataMgr.loadModel(fixDcat(naceDataset));
		    	
		    	naceEndpoint = readEndpointFromDCat(naceDcatModel);
		    	
				if (naceEndpoint != null) {
					cc.setNaceEndpoint(naceEndpoint);
				}			
				
	    	} catch (Exception ex) {
	    		ex.printStackTrace();
	    		naceEndpoint = null;
	    		
	    		if (log != null) {
	    			action.failed(ex.getMessage());
	    			updateLogRepository.save(log);
	    		}
	    	}
		}
		

//		System.out.println("NE: " + naceEndpoint);
//    	
//    	cc.setNutsEndpoint(country.getNutsEndpoint());
//    	cc.setNutsNamedGraph(country.getNutsNamedGraph());
//    	
//    	cc.setDataNamedGraph(country.getDataNamedGraph());
//    	cc.setEntitySparql(country.getEntitySparql());
//    	cc.setLegalNameSparql(country.getLegalNameSparql());
//    	cc.setActiveSparql(country.getActiveSparql());
//    	cc.setAddressSparql(country.getAddressSparql());
//    	
    	String nuts3Sparql = env.getProperty("sparql.nuts3." + cc.getCode());
    	cc.setNuts3Sparql(nuts3Sparql != null ? nuts3Sparql : null);
//    	
    	String lauSparql = env.getProperty("sparql.lau." + cc.getCode());
    	cc.setLauSparql(lauSparql != null ? lauSparql : null);
//    	
//    	cc.setFoundingDateSparql(country.getFoundingDateSparql());
//    	cc.setDissolutionDateSparql(country.getDissolutionDateSparql());
//
//    	cc.setCompanyTypeEndpoint(country.getCompanyTypeEndpoint());
//    	cc.setCompanyTypeScheme(country.getCompanyTypeScheme());
//    	cc.setCompanyTypeNamespace(country.getCompanyTypeNamespace());
//    	cc.setCompanyTypeSparql(country.getCompanyTypeSparql());
//		
//		cc.setNutsEndpoint(country.getNutsEndpoint() != null ? country.getNutsEndpoint() : defaultNutsEndpoint); 
//
//	    cc.setNutsPrefix(country.getNutsPrefix() != null ? country.getNutsPrefix() : defaultNutsPrefix);	
//	    cc.setLauPrefix(country.getLauPrefix() != null ? country.getLauPrefix() : defaultLauPrefix);	
//
//    	cc.setNacePathSparql(country.getNacePathSparql());
//    	
//    	cc.setLicenseLabel(country.getLicenceLabel());
//    	cc.setLicenseUri(country.getLicenceUri());
//    	
    	loadCountry(cc, log);
    	
//		if (cc.getNaceEndpoint() != null) {
//		    if (country.getNaceFixedLevels() != null) {
//		    	String s = "";
//		    	for (int i = 0; i < country.getNaceFixedLevels().length; i++) {
//		    		if (s.length() > 0) {
//		    			s += ",";
//		    		}
//		    		s += country.getNaceFixedLevels()[i];
//		    	}
//		    	
//		    	cc.setNaceFixedLevels(s);
//		    } else {
//		    	cc.setNaceFixedLevels(defaultNaceFixedLevel);
//		    }
//
//		}
//    	
//    	countriesRepository.save(cc);
//    	
//    	countryConfigurations.put(cc.getCode(), cc);
//    	
//    	logger.info("Country " + country.getCode() + " added/replaced.");
//    	
        return cc;
    }
    
    private boolean computeCountry(CountryDB cc) {
//		Collection<Dimension> dimensions = new ArrayList<>();
//		dimensions.add(Dimension.DATA); // should always be included
//		dimensions.add(Dimension.NUTSLAU);
//		dimensions.add(Dimension.NACE);
//		dimensions.add(Dimension.FOUNDING);
//		dimensions.add(Dimension.DISSOLUTION);
//		dimensions.add(Dimension.NUTSLAU_NACE);
//		dimensions.add(Dimension.NUTSLAU_FOUNDING);
//		dimensions.add(Dimension.NUTSLAU_DISSOLUTION);
//		dimensions.add(Dimension.NACE_FOUNDING);
//		dimensions.add(Dimension.NACE_DISSOLUTION);
//		
//		statisticsService.computeStatistics(cc, dimensions); 
//		
//		ds.copyStatisticsFromMongoToRDBMS(cc); 
		return true;
    }
	


	boolean changed(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return false;
		}
		
		if (o1 == null && o2 != null || o1 != null && o2 == null || !o1.equals(o2)) {
			return true; 
		}
		
		return false;
	}
	
	
	public boolean loadCountry(CountryDB cc, UpdateLog log) {
//		System.out.println("Loading " + cc.getCode());

		UpdateLogAction action = null;
    	if (log != null) {
    		action = new UpdateLogAction(LogActionType.QUERY_DATASET_PROPERTIES);
    		log.addAction(action);
    		updateLogRepository.save(log);
    	}

		try {
			
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getNaceSparql() + " }")) {
				cc.setNace(qe.execAsk());
	        }
	        
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getCompanyTypeSparql() + " }")) {
				cc.setCompanyType(qe.execAsk());
	        }
	
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getLegalNameSparql() + " }")) {
				cc.setLegalName(qe.execAsk());
	        }
	
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getTradingNameSparql() + " }")) {
				cc.setTradingName(qe.execAsk());
	        }
	
//	        System.out.println("ASK WHERE { " +  cc.getNuts3Sparql() + " }");
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "ASK WHERE { " +  cc.getNuts3Sparql() + " }")) {
				cc.setNuts(qe.execAsk());
	        }
	        
//	        System.out.println(cc.getDataEndpoint());
//	        System.out.println("ASK WHERE { " +  cc.getLauSparql() + " }");
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
	   		
			if (log != null) {
				action.completed();
				updateLogRepository.save(log);
			}

		} catch (Exception ex) {
			if (log != null) {
				action.failed(ex.getMessage());
				log.failed();
				updateLogRepository.save(log);
				
				return false;
			}
		}
		
		if (cc.isNuts()) {
		
        	if (log != null) {
        		action = new UpdateLogAction(LogActionType.QUERY_NUTS_PROPERTIES);
        		log.addAction(action);
        		updateLogRepository.save(log);
        	}

        	try {

		   		List<String> nutsUris = new ArrayList<>();       		
		
		   		int totalNuts = 0;
		        //try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (count(?nace) AS ?count) WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> . }")) {
	   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT (count(?nuts) AS ?count) WHERE { ?nuts a <http://data.europa.eu/m8g/AdminUnit> ; <http://data.europa.eu/m8g/level> <https://w3id.org/stirdata/resource/adminUnitLevel/NUTS-3> }")) {
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			totalNuts = qs.get("count").asLiteral().getInt();
		            }
		        }
	   			
	//	       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50")) {
		   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT ?nuts WHERE { ?nuts a <http://data.europa.eu/m8g/AdminUnit> ; <http://data.europa.eu/m8g/level> <https://w3id.org/stirdata/resource/adminUnitLevel/NUTS-3> } LIMIT 50")) {
		       		
		   				  
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			nutsUris.add(qs.get("nuts").toString());
		            }
		       		
		        }
		
		       	if (totalNuts > 50) {
			       	//try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50 OFFSET " + (totalNace - 50))) {
		       		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), "SELECT ?nuts WHERE { ?nuts a <http://data.europa.eu/m8g/AdminUnit> ; <http://data.europa.eu/m8g/level> <https://w3id.org/stirdata/resource/adminUnitLevel/NUTS-3> } LIMIT 50 OFFSET " + (totalNuts - 50))) {
			       		
			       		ResultSet rs = qe.execSelect();
			       		while (rs.hasNext()) {
			       			QuerySolution qs = rs.next();
			       			nutsUris.add(qs.get("nace").toString());
			            }
			       		
			        }
		       	}
		       	
		   		String nutsPrefix = StringUtils.getCommonPrefix(nutsUris.toArray(new String[] {}));
		   		
		   		while (nutsPrefix.charAt(nutsPrefix.length() - 1) != '/' && nutsPrefix.charAt(nutsPrefix.length() - 1) != '#') {
		   			nutsPrefix = nutsPrefix.substring(0, nutsPrefix.length() - 1);
		   		}
		   		
		   		cc.setNutsPrefix(nutsPrefix);
				
		   		if (log != null) {
					action.completed();
					updateLogRepository.save(log);
				}

			} catch (Exception ex) {
				if (log != null) {
					action.failed(ex.getMessage());
					log.failed();
					updateLogRepository.save(log);
					
					return false;
				}
			}			
		}
		
		
        if (cc.isNace() && cc.getNaceEndpoint() != null) {
        	
        	if (log != null) {
        		action = new UpdateLogAction(LogActionType.QUERY_NACE_PROPERTIES);
        		log.addAction(action);
        		updateLogRepository.save(log);
        	}

        	try {

        		// detect max nace level
//	        	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (MAX(?level) AS ?maxLevel) WHERE { ?nace a <https://w3id.org/stirdata/vocabulary/BusinessActivity> . ?nace <" + SDVocabulary.level + "> ?level }")) {
//	        		
//	           		ResultSet rs = qe.execSelect();
//	           		while (rs.hasNext()) {
//	           			QuerySolution qs = rs.next();
//	           			cc.setNaceLevels((qs.get("maxLevel").asLiteral().getInt()));
//	           		}
//	            }	
	        	
        		int maxLevel = -1;
	   			for (int i = 0; i <= 8; i++) {
	   				String p = "<http://www.w3.org/2004/02/skos/core#topConceptOf>";
	   				for (int k = 0; k < i; k++) {
	   					p = "<http://www.w3.org/2004/02/skos/core#broader>/" + p;
	   				}
	   				String lSparql = "ASK { ?nace " + p + " ?scheme  }";

//	   				System.out.println(lSparql);
		   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), lSparql)) {
		   				if (qe.execAsk()) {
		   					maxLevel = i + 1;
		   				}
		   			}
		   			
//		   			System.out.println(cc.getNaceEndpoint());
//		   			System.out.println(lSparql);

		        }
	   			
	   			
	   			cc.setNaceLevels(maxLevel);
	   			
	   			int totalNace = 0;
	   		
	//   			String nSparql = "SELECT distinct(?level) WHERE { " + cc.getNaceSparql() + " SERVICE <" + cc.getNaceEndpoint() + "> { ?nace <https://w3id.org/stirdata/vocabulary/level> ?level } }";
	   			String nSparql = "SELECT distinct(?nace) WHERE { " + cc.getNaceSparql() + " }";
//	   			System.out.println(nSparql);
	
	   			List<String> uris = new ArrayList<>();
	   			
	   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), nSparql)) {
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			uris.add(qs.get("nace").toString());
		            }
		        }
	   			
//	   			System.out.println(uris.size());
	   			
	   			// detect effective nace levels
	   			Set<Integer> levels = new TreeSet<>();
	   			for (int j = 0; j < uris.size(); j += 1000) {
		   				
		   			StringBuffer sb = new StringBuffer();
		   			for (int i = j; i < Math.min(j + 1000, uris.size()); i++) {
		   				sb.append(" <" + uris.get(i) + ">");
		   			}

		   			for (int i = 0; i <= 8; i++) {
		   				String p = "<http://www.w3.org/2004/02/skos/core#topConceptOf>";
		   				for (int k = 0; k < i; k++) {
		   					p = "<http://www.w3.org/2004/02/skos/core#broader>/" + p;
		   				}
		   				String lSparql = "ASK { ?nace " + p + " ?scheme . VALUES ?nace {" + sb.toString() + " } }";

//		   				System.out.println(lSparql);
			   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), lSparql)) {
			   				if (qe.execAsk()) {
			   					levels.add(i + 1);
			   				}
			   			}
			        }

//		   			String lSparql = "SELECT DISTINCT(?level) WHERE { ?nace <https://w3id.org/stirdata/vocabulary/level> ?level . VALUES ?nace {" + sb.toString() + " } }";
//		   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), lSparql)) {
//			       		ResultSet rs = qe.execSelect();
//			       		while (rs.hasNext()) {
//			       			QuerySolution qs = rs.next();
//			       			levels.add(qs.get("level").asLiteral().getInt());
//			            }
//			        }
	   			}   			
	   			
//	   			System.out.println("NL: " + levels);
	
//	   			int [] el = new int[levels.size()];
//	   			int i = 0;
//	   			for (Integer level : levels) {
//	   				el[i++] = level;
//	   			}
//	
//	   			cc.setNaceEffectiveLevels(el);
	   			
	   			String s = "";
	   			for (Iterator<Integer> iter = levels.iterator(); iter.hasNext();) {
		    		if (s.length() > 0) {
		    			s += ",";
		    		}
		    		s += iter.next();
		    	}
		    	
	   			cc.setNaceFixedLevels(s);
	   			
	   			
	   			
		        //try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (count(?nace) AS ?count) WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> . }")) {
	   			try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT (count(?nace) AS ?count) WHERE { ?nace a <https://w3id.org/stirdata/vocabulary/BusinessActivity> }")) {
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			totalNace = qs.get("count").asLiteral().getInt();
		            }
		        }
		
		   		List<String> naceUris = new ArrayList<>();       		
		
	//	       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50")) {
		   		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace a <https://w3id.org/stirdata/vocabulary/BusinessActivity> } LIMIT 50")) {
		       		
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			naceUris.add(qs.get("nace").toString());
		            }
		       		
		        }
		
		       	if (totalNace > 50) {
			       	//try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getNaceScheme() + "> } LIMIT 50 OFFSET " + (totalNace - 50))) {
		       		try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), "SELECT ?nace WHERE { ?nace a <https://w3id.org/stirdata/vocabulary/BusinessActivity> } LIMIT 50 OFFSET " + (totalNace - 50))) {
			       		
			       		ResultSet rs = qe.execSelect();
			       		while (rs.hasNext()) {
			       			QuerySolution qs = rs.next();
			       			naceUris.add(qs.get("nace").toString());
			            }
			       		
			        }
		       	}
		       	
		   		String nacePrefix = StringUtils.getCommonPrefix(naceUris.toArray(new String[] {}));
		   		
		   		while (nacePrefix.charAt(nacePrefix.length() - 1) != '/' && nacePrefix.charAt(nacePrefix.length() - 1) != '#') {
		   			nacePrefix = nacePrefix.substring(0, nacePrefix.length() - 1);
		   		}
		   		
		   		cc.setNacePrefix(nacePrefix);
				
		   		if (log != null) {
					action.completed();
					updateLogRepository.save(log);
				}

			} catch (Exception ex) {
				if (log != null) {
					action.failed(ex.getMessage());
					log.failed();
					updateLogRepository.save(log);
					
					return false;
				}
			}
   		}   		
   		
   		if (cc.getCompanyType() && cc.getCompanyTypeEndpoint() != null) {
   			
        	if (log != null) {
        		action = new UpdateLogAction(LogActionType.QUERY_COMPANY_TYPE_PROPERTIES);
        		log.addAction(action);
        		updateLogRepository.save(log);
        	}
        	
        	try {
	        	
	   			int totalCompanyType = 0;
	   		
	//   			System.out.println(cc.getCompanyTypeEndpoint());
	//   			System.out.println("SELECT (count(?ct) AS ?count) WHERE { ?ct <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getCompanyTypeScheme() + "> . }");
	   			
		        try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getCompanyTypeEndpoint(), "SELECT (count(?ct) AS ?count) WHERE { ?ct <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getCompanyTypeScheme() + "> . }")) {
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			totalCompanyType = qs.get("count").asLiteral().getInt();
		            }
		        }
		
		   		List<String> companyTypeUris = new ArrayList<>();       		
		
		       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getCompanyTypeEndpoint(), "SELECT ?ct WHERE { ?ct <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getCompanyTypeScheme() + "> } LIMIT 50")) {
		       		
		       		ResultSet rs = qe.execSelect();
		       		while (rs.hasNext()) {
		       			QuerySolution qs = rs.next();
		       			companyTypeUris.add(qs.get("ct").toString());
		            }
		       		
		        }
		
		       	if (totalCompanyType > 50) {
			       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getCompanyTypeEndpoint(), "SELECT ?ct WHERE { ?ct <http://www.w3.org/2004/02/skos/core#inScheme> <" +  cc.getCompanyTypeScheme() + "> } LIMIT 50 OFFSET " + (totalCompanyType - 50))) {
			       		
			       		ResultSet rs = qe.execSelect();
			       		while (rs.hasNext()) {
			       			QuerySolution qs = rs.next();
			       			companyTypeUris.add(qs.get("ct").toString());
			            }
			       		
			        }
		       	}
		
	//	       	System.out.println(companyTypeUris);
		       	
		   		String companyTypePrefix = StringUtils.getCommonPrefix(companyTypeUris.toArray(new String[] {}));
		   		
		   		while (companyTypePrefix.charAt(companyTypePrefix.length() - 1) != '/' && companyTypePrefix.charAt(companyTypePrefix.length() - 1) != '#') {
		   			companyTypePrefix = companyTypePrefix.substring(0, companyTypePrefix.length() - 1);
		   		}
		   		
		   		cc.setCompanyTypePrefix(companyTypePrefix);
		   		
		   		if (log != null) {
					action.completed();
					updateLogRepository.save(log);
				}
	
			} catch (Exception ex) {
				if (log != null) {
					action.failed(ex.getMessage());
					log.failed();
					updateLogRepository.save(log);
					
					return false;
				}
			}
		}  
   		
   		return true;
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
	
    private String readEndpointFromDCat(Model dcatModel) {
        String sparql = 
				"SELECT ?endpoint WHERE { " + 
		        "  ?dcat a <" + DCATVocabulary.Dataset + "> . " +
		        "  ?dcat <" + DCATVocabulary.distribution + "> ?distribution . " +
		        "  ?distribution <" + DCATVocabulary.accessService + "> ?service . " +
		        "  ?service a <" + DCATVocabulary.DataService + "> . " +
		        "  ?service <" + DCTVocabulary.conformsTo + "> <https://www.w3.org/TR/sparql11-protocol/> . " +
		        "  ?service <" + DCATVocabulary.endpointURL + "> ?endpoint . " +
		        "} ";

        try (QueryExecution qe = QueryExecutionFactory.create(sparql, dcatModel)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next(); 
        		String endpoint = sol.get("endpoint").toString();
        		String[] parts = endpoint.split("://");
        	

        		return parts[0] + "://" + IDN.toASCII(parts[1]);
        	}
        }
        
        return null;
    }


    
//  public boolean reloadDCAT(CountryDB cc)  {
//		logger.info("DCAT reloading for " + cc.getCode() + ".");
//		
//		
//		boolean changed = processDcat(cc, modelConfigurations);
//  	
//  	ModelConfiguration mc = cc.getModelConfiguration();
//  	if (mc == null) {
//  		return false;
//  	}
//  	
//  	if (changed) {
//	//    	cd.setLastAccessedStart(new Date());
//	    	
//	    	loadCountry(cc);
//	    	
//	//    	System.out.println(cc.getNaceNamespace());
//	    	
//	    	countriesRepository.save(cc);
//	    	
//	    	countryConfigurations.put(cc.getCode(), cc);
//	    	
//	    	logger.info("Country " + cc.getCode() + " reloaded.");
//  	} else {
//  		logger.info("Country " + cc.getCode() + " has not changed.");
//  	}
//  	
//      return changed;
//  }

}

