package com.ails.stirdatabackend.service;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.repository.ActivitiesDBRepository;
import com.ails.stirdatabackend.vocs.SDVocabulary;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NaceService {

//	private final static Logger logger = LoggerFactory.getLogger(NaceService.class);
	
    @Autowired
    @Qualifier("endpoint-nace-eu")
    private String naceEndpointEU;

//    @Autowired
//    @Qualifier("namedgraph-nace-eu")
//    private String naceNamedgraphEU;
    
//    @Autowired
//    @Qualifier("country-configurations")
//    private Map<String, CountryConfiguration> countryConfigurations;

    @Autowired
    private ActivitiesDBRepository activitiesRepository;

    
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // NACE REV 2

    public ActivityDB getByCode(Code code) {
    	return activitiesRepository.findByCode(code);
    }

    public ActivityDB getNaceRev2Ancestor(ActivityDB activity) {
    	if (activity.getLevel() <= 4 && activity.getExactMatch() != null) {
    		return activity.getExactMatch();
    	} else if (activity.getLevel() == 5) {
    		return activitiesRepository.findLevel4NaceRev2AncestorFromLevel5(activity);
    	} else if (activity.getLevel() == 6) {
    		return activitiesRepository.findLevel4NaceRev2AncestorFromLevel6(activity);
    	} else if (activity.getLevel() == 7) {
    		return activitiesRepository.findLevel4NaceRev2AncestorFromLevel7(activity);
    	} else {
    		return null;
    	}
    }

    public List<ActivityDB> getParents(ActivityDB activity) {
    	List<ActivityDB> parents = new ArrayList<>();
    	
    	while (activity.getParent() != null) {
    		parents.add(0, activity.getParent());
    		activity = activity.getParent();
    	}
    	
    	return parents;
    }
    
    public List<ActivityDB> getNextNaceLevelListDb(Code parent) {

    	ActivityDB parentActivity = null;
    	if (parent != null) {
    		parentActivity = new ActivityDB(parent);
    	}
    	
    	return activitiesRepository.findBySchemeAndParent(Code.naceRev2Namespace, parentActivity);
    }

    public String getNextNaceLevelJsonTs(String parent, String lang) {
        String sparql = nextNaceLevelSparqlQuery(parent, lang);
        
        String json;
        try (QueryExecution qe = QueryExecutionFactory.sparqlService(naceEndpointEU, sparql)) {
            ResultSet rs = qe.execSelect();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(outStream, rs);
            json = outStream.toString();
        }
        return json;
    }
    
    private String nextNaceLevelSparqlQuery(String parent, String lang) {
    	String sparql = 
    			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?code " + (lang == null ? "" : "?label ");
    	
//    	sparql += naceNamedgraphEU != null ? "FROM <" + naceNamedgraphEU + "> " : "";
    	
    	sparql += " WHERE { ";

    	sparql += "?code <http://www.w3.org/2004/02/skos/core#inScheme> <https://w3id.org/stirdata/resource/nace/scheme/NACERev2> . ";
    	
		if (parent == null) {
		    sparql += "?code <" + SDVocabulary.level + "> 1 . ";
		} else {
		    sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parent + "> " +  ". ";
		}
		
		if (lang != null) {
			sparql += "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label . FILTER (lang(?label) = \"" + lang + "\") ";
		}
		
		sparql += "}" ;
		
		return sparql;
	}

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // LOCAL NACE 

    public List<String> getLocalNaceLeafUris(CountryDB cc, Code naceCode) {
    	return getLocalNaceLeafUris(cc, Arrays.asList(new Code[] { naceCode }));
   	}
    
    public List<String> getLocalNaceLeafUris(CountryDB cc, List<Code> naceCodes) {
    	List<String> naceLeafUris = null;
    	if (naceCodes != null) {
    		naceLeafUris = new ArrayList<>();

			for (Code code : naceCodes) {
//				System.out.println(code);
				if (cc.getNacePathSparql() != null) {
					naceLeafUris.add(Code.naceRev2Prefix + code.getCode()); // triples store contains local naces 	
				} else {
					naceLeafUris.addAll(getNaceLeafUrisTS(cc, code));
//					naceLeafUris.addAll(getNaceLeafUrisDB(cc, code)); // much slower
				}
            }
    	}
    	
    	return naceLeafUris;
    }    
    
    private Set<String> getNaceLeafUrisTS(CountryDB cc, Code code) {
    	Set<String> res = new HashSet<>();

    	int level = code.getNaceRev2Level();
    	if (level < 0) {
    		return res;
    	}

    	String sparql = 
    			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                "SELECT ?activity WHERE { ";
		
    	if (level == 1) {
			sparql += "?activity " + cc.getNacePath1() + " <" + code.toUri() + "> . "; 
		} else if (level == 2) {
			sparql += "?activity " + cc.getNacePath2() + " <" + code.toUri() + "> . "; 
		} else if (level == 3) {
			sparql += "?activity " + cc.getNacePath3() + " <" + code.toUri() + "> . "; 
		} else if (level == 4) {
			sparql += "?activity " + cc.getNacePath4() + " <" + code.toUri() + "> . "; 
    	}
    	
    	if (cc.getNaceFixedLevel() >= 0) {
    		sparql += " ?activity <" + SDVocabulary.level + "> " + cc.getNaceFixedLevel() + " . ";
    	}
    	
		sparql += " ?activity skos:inScheme <" + cc.getNaceScheme() + "> } ";
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getNaceEndpoint(), sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                res.add(sol.get("activity").asResource().toString());
            }
        }
    	
    	return res;

    }    
    
    private Set<String> getNaceLeafUrisDB(CountryDB cc, Code code) {
    	Set<String> res = new HashSet<>();

    	int level = code.getNaceRev2Level();
    	if (level < 0) {
    		return res;
    	}

    	List<ActivityDB> activities = new ArrayList<>();  
    	
    	if (cc.getNaceFixedLevel() == 5) {
    		if (level == 1) {
    			activities.addAll(activitiesRepository.findLevel4AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 2) {
    			activities.addAll(activitiesRepository.findLevel3AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 3) {
    			activities.addAll(activitiesRepository.findLevel2AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 4) {
    			activities.addAll(activitiesRepository.findLevel1AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} 
    	} else if (cc.getNaceFixedLevel() == -5) {
    		if (level == 1) {
    			activities.addAll(activitiesRepository.findLevel4AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel3AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel2AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel1AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel0AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 2) {
    			activities.addAll(activitiesRepository.findLevel3AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel2AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel1AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel0AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 3) {
    			activities.addAll(activitiesRepository.findLevel2AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel1AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel0AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} else if (level == 4) {
    			activities.addAll(activitiesRepository.findLevel1AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    			activities.addAll(activitiesRepository.findLevel0AfterDescendentFromNaceRev2(new ActivityDB(code), cc.getNaceNamespace()));
    		} 
    	}

    	
		for (ActivityDB ac : activities) {
			res.add(cc.getNacePrefix() + "" + ac.getCode().getCode());
		}
    	
    	return res;

    }        

}
