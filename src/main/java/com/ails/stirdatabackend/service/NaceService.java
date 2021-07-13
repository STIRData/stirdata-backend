package com.ails.stirdatabackend.service;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import com.ails.stirdatabackend.configuration.ApplicationConfiguration;
import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.model.SparqlEndpoint;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

@Service
public class NaceService {

	private final static Logger logger = LoggerFactory.getLogger(NaceService.class);
	
    @Autowired
    @Qualifier("endpoint-nace-eu")
    private SparqlEndpoint naceEndpointEU;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
    public List<String> getLocalNaceLeafUris(String country, List<String> naceList, HttpClient httpClient) {
    	
    	List<String> naceLeafUris = null;
    	if (naceList != null) {
    		naceLeafUris = new ArrayList<>();
    		
    		try {
	    		Method md = NaceService.class.getDeclaredMethod("get" + country + "NaceLeaves", String.class, HttpClient.class);
	    		
	    		for (String s : naceList) {
	       			naceLeafUris.addAll((Set<String>)md.invoke(this, s, httpClient));
	            }
    		} catch (Exception ex) {
    			ex.printStackTrace();
    			logger.error("Could not invoke NACE service for " + country);
    		}
    	}
    	
    	return naceLeafUris;
    }
    
    public String getNextNaceLevel(String parentNode, String language) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "SELECT ?code ?label WHERE { ";
        
        if (parentNode == null) {
            sparql += "?code <https://lod.stirdata.eu/nace/ont/level> 1 . ";
        } else {
            sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parentNode + "> " +  ". ";
        }
        sparql += "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label " +
                  "FILTER (lang(?label) = \"" + language + "\") }";

        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceEndpointEU.getSparqlEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = outStream.toString();
        }
        return json;
    }
    
    public int getNaceLevel(String uri) {
    	int pos = uri.lastIndexOf("/");
    	if (pos < 0) {
    		return -1;
    	}
    	
    	String code = uri.substring(pos + 1);
    	int len = code.length();
    	
    	if (len == 1 && Character.isLetter(code.charAt(0))) {
    		return 1;
    	} else if (len == 2) {
    		return 2;
    	} else if (len == 4 && code.charAt(2) == '.') {
    	    return 3;
    	} else if (len == 5 && code.charAt(2) == '.') {
    		return 4;
    	}
    	
    	return -1;
    }

    
    public Set<String> getNONaceLeaves(String uri, HttpClient httpClient) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "SELECT ?activity WHERE { ";
		if (level == 1) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
		} else if (level == 2) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
		} else if (level == 3) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
		} else if (level == 4) {
			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
    	}
    	
		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/SIC2007NO> } ";

    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("NO").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
    
    public Set<String> getBENaceLeaves(String uri, HttpClient httpClient) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		                "SELECT ?activity WHERE { ";
    	if (level == 1) {
    		sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 2) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 3) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
    	} else if (level == 4) {
			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
    	}

		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/NACEBEL2008> } ";

    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("BE").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
    
    public Set<String> getELNaceLeaves(String uri, HttpClient httpClient) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		                "SELECT ?activity WHERE { ";
    	if (level == 1) {
    		sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 2) {
			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 3) {
			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
    	} else if (level == 4) {
			sparql += "?activity skos:broader/skos:broader/skos:broader/skos:exactMatch <" + uri + "> . "; 
    	}

		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 7 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/KAD2008> } ";

    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("EL").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
    
    public Set<String> getCZNaceLeaves(String uri, HttpClient httpClient) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		                "SELECT ?activity WHERE { " +
    		            "   ?activity skos:broader*/skos:exactMatch  <" + uri + "> . " + 
		                "   ?activity skos:inScheme <https://obchodní-rejstřík.stirdata.opendata.cz/zdroj/číselníky/nace-cz> " +
    		            " } ";

    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("CZ").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }
    
    public Set<String> getFINaceLeaves(String uri, HttpClient httpClient) {
    	Set<String> res = new HashSet<>();

    	int level = getNaceLevel(uri);
    	if (level < 0) {
    		return res;
    	}

    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		                "SELECT ?activity WHERE { ";
    	if (level == 1) {
    		sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 2) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader/skos:broader <" + uri + "> . "; 
    	} else if (level == 3) {
			sparql += "?activity skos:broader/skos:exactMatch/skos:broader <" + uri + "> . "; 
    	} else if (level == 4) {
			sparql += "?activity skos:broader/skos:exactMatch <" + uri + "> . "; 
    	}

		sparql += " ?activity <https://lod.stirdata.eu/nace/ont/level> 5 . " +
		          " ?activity skos:inScheme <https://lod.stirdata.eu/nace/scheme/TOL2008> } ";

    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(countryConfigurations.get("FI").getNaceEndpoint().getSparqlEndpoint(), sparql, httpClient)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;
    }
}
