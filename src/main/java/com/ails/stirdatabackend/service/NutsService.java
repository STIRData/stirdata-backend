package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import com.ails.stirdatabackend.utils.URIMapper;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NutsService {

    @Autowired
    @Qualifier("endpoint-nuts-eu")
    private SparqlEndpoint nutsEndpointEU;
    
    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;

    @Autowired
    private URIMapper uriMapper;

    private final static Pattern nacePattern = Pattern.compile("^https://lod\\.stirdata\\.eu/nuts/code/([A-Z][A-Z])");
    
    
    public List<String> getNuts3Uris(CountryConfiguration cc, List<String> requestMap) {
    	
    	List<String> nuts3UrisList = null;
    	if (requestMap != null) {
            Set<String> nuts3Uris = new HashSet<>();
            for (String s : requestMap) {
            	nuts3Uris.addAll(getNuts3Descendents(s));
            }

            nuts3UrisList = new ArrayList<>(nuts3Uris);

            nuts3UrisList = cc.getCountry().equals("CZ") ? 
            		uriMapper.mapCzechNutsUri(nuts3UrisList) : uriMapper.mapEuropaNutsUri(nuts3UrisList);

    	}
    	
    	return nuts3UrisList;

    }
    
    public Map<CountryConfiguration, List<String>> getEndpointsByNuts(List<String> nutsUri) {
        Map<CountryConfiguration, List<String>> res = new HashMap<>();

        for (String uri : nutsUri) {
        	
        	Matcher matcher = nacePattern.matcher(uri);
        	if (matcher.find()) {
        		String country = matcher.group(1);
        		
        		CountryConfiguration cc = countryConfigurations.get(country);
        		
        		List<String> list = res.get(cc);
                if (list == null) {
                    list = new ArrayList<>();
                    res.put(cc, list);
                }
                
                list.add(uri);
        	}
        }
        
        return res;
    }
    
    public Map<CountryConfiguration, List<String>> getEndpointsByNuts() {
        Map<CountryConfiguration, List<String>> res = new HashMap<>();
        for (CountryConfiguration cc : countryConfigurations.values()) {
       		res.put(cc, null);
        }
        return res;
    }
    
    public String getNextNutsLevel(String parentNode) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"+
                        "SELECT ?code ?label WHERE { ";
        
        String countries = "";
        for (String c : countryConfigurations.keySet()) {
        	countries += "\"" + c + "\" ";
        }
        
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nuts/ont/level> 0 . "+
                      "?code <http://www.w3.org/2004/02/skos/core#notation> ?notation ."+
                      "VALUES ?notation { " + countries + " }";
        } else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ". ";
        }
        
        sparql += " ?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label } ORDER BY ?code";

        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = outStream.toString();
        }
        return json;
    }

    public String getTopLevelNuts(String uri) {
        String sparql = "SELECT ?nuts0 WHERE {" +
                "<" +  uri + ">" + "<http://www.w3.org/2004/02/skos/core#broader>* ?nuts0 . " +
                "?nuts0 <https://lod.stirdata.eu/nuts/ont/level> 0 ." +
                "} ";
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                return sol.get("nuts0").asResource().toString();
            }
        }
        return null;
    }
    
    public Set<String> getNuts3Descendents(String uri) {
    	Set<String> res = new HashSet<>();
    	
    	int pos = uri.lastIndexOf("/");
    	if (pos < 0) {
    		return res;
    	}
    	
    	String code = uri.substring(pos + 1);
    	int level = code.length() - 2;
    	
    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  "; 
    	if (level == 0) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader/skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 1) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 2) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader <" + uri + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 3) {
    		res.add(uri);
    		return res;
    	}
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("nuts3").asResource().toString());
            }
        }
    	
    	return res;
    }

    public String getNutsGeoJson(String nutsUri, String spatialResolution) {
//        final String sparql = "construct {\n"
//                + "<" + nutsUri+ "> <" + GeoSparqlVocabulary.hasGeometry + "> ?o .\n"
//                + "?o <" + GeoSparqlVocabulary.asGeoJSON + "> ?o2 .\n"
//                + "<" + nutsUri + "> <" + GeoSparqlVocabulary.contains + "> ?o1\n"
//                + "\n"
//                + "}  where {\n"
//                + "<" + nutsUri + "> <" + GeoSparqlVocabulary.hasGeometry + "> ?o.\n"
//                + "?o <" + GeoSparqlVocabulary.hasSpatialResolution + "> \"" + spatialResolution + "\" .\n"
//                + "?o <" + GeoSparqlVocabulary.asGeoJSON + "> ?o2 .\n"
//                + "OPTIONAL { <" + nutsUri + "> <" + GeoSparqlVocabulary.contains + "> ?o1} \n"
//                + "}";
        final String sparql =
            "construct {"
            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#hasGeometry> ?o . "
            + "?o <http://www.opengis.net/ont/geosparql#asGeoJSON> ?o2 . "
            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#contains> ?o1 . "
            + "} where {"
            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#hasGeometry> ?o . "
            + "?o <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"" + spatialResolution + "\" . "
            + "?o <http://www.opengis.net/ont/geosparql#asGeoJSON> ?o2 . "
            + "OPTIONAL { <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#contains> ?o1} . "
            + "}";
        String res;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU.getSparqlEndpoint(), sparql)) {
            final Model m = qe.execConstruct();
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            RDFDataMgr.write(outStream, m, RDFFormat.JSONLD);
            res = outStream.toString();
        }

        return res;
    }

}
