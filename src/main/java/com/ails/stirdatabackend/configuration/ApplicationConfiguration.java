package com.ails.stirdatabackend.configuration;

import com.ails.stirdatabackend.controller.URIDescriptor;
import com.ails.stirdatabackend.model.AddOn;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.ModelConfiguration;
import com.ails.stirdatabackend.model.StatisticDB;
import com.ails.stirdatabackend.payload.CodeLabel;
import com.ails.stirdatabackend.payload.CubeResponse;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.repository.CountriesRepository;
import com.ails.stirdatabackend.repository.StatisticsDBRepository;
import com.ails.stirdatabackend.service.CountriesService;
import com.ails.stirdatabackend.vocs.DCATVocabulary;
import com.ails.stirdatabackend.vocs.DCTVocabulary;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.riot.JsonLDWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
//@EnableScheduling
public class ApplicationConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);

	@Value("${cache.labels.size}")
	private int cacheSize;

	@Value("${cache.labels.live-time}")
	private int liveTime;
	
	@Value("${app.data-models}")
	private String models;
	
	@Value("${statistics.default.from-date}")
	private String fromDate;

	@Autowired
	private CountriesDBRepository countriesRepository;

	@Autowired
	ResourceLoader resourceLoader;

	@Bean(name = "labels-cache")
	public Cache getLabelsCache() {
	    CacheManager singletonManager = CacheManager.create();
	    if (!singletonManager.cacheExists("labels")) {
		    singletonManager.addCache(new Cache("labels", cacheSize, false, false, liveTime, liveTime));
		    
			logger.info("Created labels cache.");
	    }
	    
	    return singletonManager.getCache("labels");
	}

//	@Bean(name = "nuts-geojson-cache")
//	public Cache getNutsGeojsonCache() {
//		CacheManager singletonManager = CacheManager.create();
//		if (!singletonManager.cacheExists("nuts-geojson-cache")) {
//			singletonManager.addCache(new Cache("nuts-geojson-cache", cacheSize, false, false, liveTime, liveTime));
//
//			logger.info("Created nuts-geojson cache.");
//		}
//
//		return singletonManager.getCache("nuts-geojson-cache");
//	}

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
	public String getNaceEndpointEU() {
		return env.getProperty("endpoint.nace.eu");
	}

	@Bean(name = "endpoint-nuts-eu")
	public String getNutsEndpointEU() {
		return env.getProperty("endpoint.nuts.eu");
	}
	
	@Bean(name = "endpoint-nuts-stats-eu")
	public String getNutsStatsEndpointEU() {
		return env.getProperty("endpoint.nuts-stats.eu");
	}
	
	@Bean(name = "endpoint-lau-eu")
	public String getLauEndpointEU() {
		return env.getProperty("endpoint.lau.eu");
	}
	
	@Bean(name = "namedgraph-nace-eu")
	public String getNaceNamedgraphEU() {
		return env.getProperty("namedgraph.nace.eu");
	}

	@Bean(name = "namedgraph-nuts-eu")
	public String getNutsNamedgraphEU() {
		return env.getProperty("namedgraph.nuts.eu");
	}
	
	@Bean(name = "namedgraph-lau-eu")
	public String getLauNamedgraphEU() {
		return env.getProperty("namedgraph.lau.eu");
	}
	
	@Bean(name = "default-from-date")
	public Date getDefaultFromDate() {
		return Date.valueOf(fromDate);
	}
	
	@Bean(name = "model-configurations")
	public Map<String, ModelConfiguration> getSupportedModels() {

		Map<String, ModelConfiguration> map = new HashMap<>();
		
		for (String m : models.split(",")) {
			ModelConfiguration mc = new ModelConfiguration(m);
			
			mc.setUrl(env.getProperty("data-model.url." + m));
			
		    String entitySparql = env.getProperty("sparql.entity." + m);
		    String legalNameSparql = env.getProperty("sparql.legalName." + m);
		    String tradingNameSparql = env.getProperty("sparql.tradingName." + m);
		    String activeSparql = env.getProperty("sparql.active." + m);
		    String addressSparql = env.getProperty("sparql.address." + m);
		    String nuts3Sparql = env.getProperty("sparql.nuts3." + m);
		    String lauSparql = env.getProperty("sparql.lau." + m);
		    String naceSparql = env.getProperty("sparql.nace." + m);
		    String companyTypeSparql = env.getProperty("sparql.companyType." + m);
		    String foundingDateSparql = env.getProperty("sparql.foundingDate." + m); 
		    String dissolutionDateSparql = env.getProperty("sparql.dissolutionDate." + m);
		    String leiCodeSparql = env.getProperty("sparql.leiCode." + m);

		    mc.setEntitySparql(entitySparql);
	    	mc.setLegalNameSparql(legalNameSparql);	
	    	mc.setTradingNameSparql(tradingNameSparql);
	    	mc.setActiveSparql(activeSparql);
	    	mc.setAddressSparql(addressSparql);
	    	mc.setNuts3Sparql(nuts3Sparql);	
	    	mc.setLauSparql(lauSparql);	
	    	mc.setNaceSparql(naceSparql);	
	    	mc.setCompanyTypeSparql(companyTypeSparql);
	    	mc.setFoundingDateSparql(foundingDateSparql);
	    	mc.setDissolutionDateSparql(dissolutionDateSparql);
	    	mc.setLeiCodeSparql(leiCodeSparql);
	    	
			map.put(mc.getUrl(), mc);
		}
		
		return map;
	}
	
	@Bean(name = "country-configurations")
	@DependsOn("model-configurations")
	public CountryConfigurationsBean getSupportedCountriesConfigurations(@Qualifier("model-configurations") Map<String, ModelConfiguration> mcMap) {
//		Map<String, CountryDB> map = new LinkedHashMap<>();
		
		CountryConfigurationsBean ccb = new CountryConfigurationsBean();
		
		System.out.println("LOADING COUNTRIES: ");
		String s = "";
		for (CountryDB cc  : countriesRepository.findAll()) {
			if (cc.getActiveLegalEntityCount() != null) {
				cc.setModelConfiguration(mcMap.get(cc.getConformsTo()));
	//			cc.setStatistics(new HashSet<>(statisticsRepository.findDimensionsByCountry(cc.getCode())));
				
				s += cc.getCode() + " ";
				ccb.put(cc.getCode(), cc);
			}
		}
		logger.info("Loaded countries: " + s);
		
		return ccb;
	}
	
	@Bean(name = "model-jsonld-context")
	public JsonLDWriteContext getContext() {
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		
		try (InputStream in = resourceLoader.getResource("classpath:stirdata.jsonld").getInputStream()) {
	        ctx.setJsonLDContext(new String(FileCopyUtils.copyToByteArray(in), StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ctx;
	}
	
	@Bean(name = "country-addons")
	@DependsOn("country-configurations")
	public Map<String, AddOn> getAddons(@Qualifier("country-configurations") CountryConfigurationsBean countryConfigurations) {
		Map<String, AddOn> map = new HashMap<>();
		
		String addons = env.getProperty("app.add-ons");
		System.out.println(addons);
		for (String addon : addons.split(",")) {
			
			String properties = env.getProperty("properties." + addon);
			
			for (String country : countryConfigurations.keySet()) {
				
				AddOn ao = new AddOn();
				ao.setCountry(country);
				
				if (env.getProperty("endpoint." + addon + "." + country) != null) {
					ao.setEndpoint(env.getProperty("endpoint." + addon + "." + country));
					ao.setNamedGraph(env.getProperty("named-graph." + addon + "." + country));
					
					for (String prop : properties.split(",")) {
						String sparql = env.getProperty("sparql." + addon + "." + prop + "." + country);
						
						if (prop.equals("entity")) {
							ao.setEntitySparql(sparql);
						} else {
							ao.addProperty(prop, sparql);
						}
					}
					
					map.put(addon + "-" + country, ao);
				}
			}
		}
		
		return map;
		
	}

	@Bean(name = "filters")
	@DependsOn("endpoint-nuts-stats-eu")
    public List<CubeResponse> getFilters(@Qualifier("endpoint-nuts-stats-eu") String nutsStatsEndpointEU) {

		logger.info("Loading filters");
    	String sparql = 
    			"PREFIX cube: <http://purl.org/linked-data/cube#> " +
    			"SELECT ?dataset ?prop ?propLabel ?value ?valueLabel " +  
    			"WHERE { " +
    				"?dataset a cube:DataSet ; " +
    	         		"cube:structure " +
    	                	"[ a cube:DataStructureDefinition ; " +
    	                  		"cube:component " +
    								"[ cube:dimension <https://w3id.org/stirdata/vocabulary/stat/refArea> ] ] ; " +
    	         		"cube:structure " +
    	                	"[ a cube:DataStructureDefinition ; " +
    	                  		"cube:component " +
    								"[ cube:measure ?prop ] ] . " +
    				"?prop a cube:DimensionProperty ; " +
      					"<http://www.w3.org/2000/01/rdf-schema#label> ?propLabel ; " +
    	        		"cube:codeList ?scheme . " +
    				"?value a <http://www.w3.org/2004/02/skos/core#Concept> ; " +
    					"<http://www.w3.org/2004/02/skos/core#prefLabel> ?valueLabel ; " +    				
    	        		"<http://www.w3.org/2004/02/skos/core#inScheme> ?scheme . " + 
    	        "} ORDER BY ?propLabel ?valueLabel";
    	
    		
    	Map<String, CubeResponse> res = new LinkedHashMap<>();
    	
//    	System.out.println(sparql);
//    	System.out.println(QueryFactory.create(sparql));
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsStatsEndpointEU, sparql)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next();

        		String dataset = sol.get("dataset").asResource().toString();
        		
     			String prop = sol.get("prop").asResource().toString();
     			String propLabel = sol.get("propLabel").asLiteral().getLexicalForm();
     			String value = sol.get("value").asResource().toString();
     			String valueLabel = sol.get("valueLabel").asLiteral().getLexicalForm();

     			Code datasetCode = Code.fromStatDatasetUri(dataset);
     			CodeLabel datasetCodeLabel = new CodeLabel(datasetCode.toString(), null, dataset);

     			Code propCode = Code.fromStatPropetyUri(prop);
     			CodeLabel propCodeLabel = new CodeLabel(propCode.toString(), propLabel, prop);

     			Code valueCode = Code.fromStatValueUri(value);
     			CodeLabel valueCodeLabel = new CodeLabel(valueCode.toString(), valueLabel, value);
     			
     			CubeResponse cube = res.get(dataset + "##" + prop);
     			if (cube == null) {
     				cube = new CubeResponse();
     				cube.setDataset(datasetCodeLabel);
     				cube.setProperty(propCodeLabel);
     				res.put(dataset + "##" + prop, cube);
     			}
     			
     			cube.addValue(valueCodeLabel);
       		}
    	}
    	
    	return new ArrayList<>(res.values());
    	
    }

	@Bean(name = "eurostat-filters")
	@DependsOn("endpoint-nuts-stats-eu")
    public List<CubeResponse> getEurostatFilters(@Qualifier("endpoint-nuts-stats-eu") String nutsStatsEndpointEU) {
    	
		logger.info("Loading EUROSTAT filters");
    	Map<String, CubeResponse> res = new LinkedHashMap<>();

    	{

    	String sparql = 
    			"PREFIX qb: <http://purl.org/linked-data/cube#> " + 
    			"SELECT ?dataset ?datasetLabel ?prop WHERE { " + 
    			"   ?dataset a qb:DataSet ; " + 
    			"      <http://www.w3.org/2000/01/rdf-schema#label>  ?datasetLabel . FILTER (lang(?datasetLabel) = 'en') . " +
    			"   ?dataset " +
    			"      qb:structure [ " + 
    			"         a qb:DataStructureDefinition ; " + 
    			"         qb:component [ " + 
    			"            qb:dimension <https://w3id.org/stirdata/vocabulary/stat/geo> " + 
    			"         ] ; " + 
    			"         qb:component [ " + 
    			"            qb:dimension <https://w3id.org/stirdata/vocabulary/stat/time> " + 
    			"         ]  " +
    			"      ] ; " + 
    			"      qb:structure [ " + 
    			"         a qb:DataStructureDefinition ; " + 
    			"         qb:component [ " + 
    			"            qb:measure ?prop " + 
    			"         ] " + 
    			"      ] . " + 
    			"  FILTER EXISTS { ?obs a       <http://purl.org/linked-data/cube#Observation> ; " +
    			"      qb:dataSet ?dataset . } " +

    			" } " ;
    	
    	
//    	System.out.println(sparql);
//    	System.out.println(QueryFactory.create(sparql));
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsStatsEndpointEU, sparql)) {
        	ResultSet rs = qe.execSelect();
        	
        	while (rs.hasNext()) {
        		QuerySolution sol = rs.next();

        		String dataset = sol.get("dataset").asResource().toString();
        		
     			String prop = sol.get("prop").asResource().toString();
     			String datasetLabel = sol.get("datasetLabel").asLiteral().getLexicalForm();

     			Code datasetCode = Code.fromStatDatasetUri(dataset);
     			CodeLabel datasetCodeLabel = new CodeLabel(datasetCode.toString(), null, dataset);

     			Code propCode = Code.fromStatPropetyUri(prop);
     			CodeLabel propCodeLabel = new CodeLabel(propCode.toString(), datasetLabel, prop);

     			CubeResponse cube = res.get(dataset + "##" + prop);
     			if (cube == null) {
     				cube = new CubeResponse();
     				cube.setDataset(datasetCodeLabel);
     				cube.setProperty(propCodeLabel);
     				res.put(dataset + "##" + prop, cube);
     			}
       		}
        	
    	}}
    	
    	for (Map.Entry<String, CubeResponse> entry : res.entrySet()) {
    		String[] s = entry.getKey().split("##");
    		String dataset = s[0];
    		String prop = s[1];
    		
    		CubeResponse cube = entry.getValue();
    		
    	   	String sparql = 
        			"PREFIX qb: <http://purl.org/linked-data/cube#> " + 
        			"SELECT ?dimension ?dimensionLabel (min(?value) AS ?min) (max(?value) AS ?max) ?dimensionValue ?dimensionValueLabel WHERE { " + 
        			"   <" + dataset + "> a qb:DataSet ; " + 
        			"      qb:structure [ " + 
        			"         a qb:DataStructureDefinition ; " + 
        			"         qb:component [ " + 
        			"            qb:dimension ?dimension " + 
        			"         ]  " + 
        			"      ] . " +
        			"  ?dimension <http://www.w3.org/2000/01/rdf-schema#label> ?dimensionLabel . FILTER (lang(?dimensionLabel) = 'en') . " +
        			"  FILTER ( ?dimension != <https://w3id.org/stirdata/vocabulary/stat/time> && ?dimension != <https://w3id.org/stirdata/vocabulary/stat/geo> ) " +
        			"  ?obs a       <http://purl.org/linked-data/cube#Observation> ; " +
        			"      qb:dataSet ?dataset ; " +
        			"      <" + prop + "> ?value ; " +        			
        			"      ?dimension ?dimensionValue ." +
        			"  ?dimensionValue  <http://www.w3.org/2004/02/skos/core#prefLabel>  ?dimensionValueLabel . FILTER ( lang(?dimensionValueLabel) = \"en\" ) " +
        			" } " +
        			" GROUP BY ?dimension ?dimensionLabel  ?dimensionValue ?dimensionValueLabel " +
        			" ORDER BY ?dimension ?dimensionValue " ;
        	
        	
//        	System.out.println(sparql);
//        	System.out.println(QueryFactory.create(sparql));
        	
        	CodeLabel prevDimensionCodeLabel = null;
        	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsStatsEndpointEU, sparql)) {
            	ResultSet rs = qe.execSelect();
            	
            	while (rs.hasNext()) {
            		QuerySolution sol = rs.next();

            		String dimension = sol.get("dimension").toString();
         			String dimensionLabel = sol.get("dimensionLabel").asLiteral().getLexicalForm();
            		String dimensionValue = sol.get("dimensionValue").toString();
         			String dimensionValueLabel = sol.get("dimensionValueLabel").asLiteral().getLexicalForm();

         			Literal minValue = sol.get("min").asLiteral();
         			Literal maxValue = sol.get("max").asLiteral();
         			
         			if (prevDimensionCodeLabel == null || !prevDimensionCodeLabel.uri.equals(dimension)) {
	         			Code dimensionCode = Code.fromStatPropetyUri(dimension);
	         			CodeLabel dimensionCodeLabel = new CodeLabel(dimensionCode.toString(), dimensionLabel, dimension);

	         			cube.addValue(dimensionCodeLabel);
	         			
	         			prevDimensionCodeLabel = dimensionCodeLabel;
         			}
         			
         			Code dimensionValueCode = Code.fromStatValueUri(dimensionValue);
         			CodeLabel dimensionValueCodeLabel = new CodeLabel(dimensionValueCode.toString(), dimensionValueLabel, dimensionValue);
         			if (minValue.getDatatype().toString().equals("http://www.w3.org/2001/XMLSchema#integer")) {
         				dimensionValueCodeLabel.minIntValue = minValue.getInt();
         				dimensionValueCodeLabel.maxIntValue = maxValue.getInt();
         			} else {
         				dimensionValueCodeLabel.minDoubleValue = minValue.getDouble();
         				dimensionValueCodeLabel.maxDoubleValue = maxValue.getDouble();
         			}
         			
         			prevDimensionCodeLabel.addValue(dimensionValueCodeLabel);
         			
           		}
            	
//            	System.out.println(res);
        	}
    		
    	}
    	
    	return new ArrayList<>(res.values());
    	
    }
	
}
