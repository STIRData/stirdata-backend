package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.repository.PlacesDBRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NutsService {

    @Autowired
    @Qualifier("endpoint-nuts-eu")
    private String nutsEndpointEU;

    @Autowired
    @Qualifier("endpoint-lau-eu")
    private String lauEndpointEU;
    
//    @Autowired
//    @Qualifier("namedgraph-nuts-eu")
//    private String nutsNamedgraphEU;
//
//    @Autowired
//    @Qualifier("namedgraph-lau-eu")
//    private String lauNamedgraphEU;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;
    
    @Autowired
    private PlacesDBRepository placesRepository;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public class PlaceSelection {
    	private List<Code> nuts3; // null means no selection
    	private List<Code> lau;   // null means no selection
    	
    	public void addNuts3(Code nuts3) {
    		if (this.nuts3 == null) {
        		this.nuts3 = new ArrayList<>();
    		}
    		this.nuts3.add(nuts3);
    	}

    	public void addLau(Code lau) {
    		if (this.lau == null) {
        		this.lau = new ArrayList<>();
    		}
    		this.lau.add(lau);
    	}
    }
    
    public List<String> getLocalNutsLeafUris(CountryDB cc, List<Code> nutsCodes) {
    	
    	if (nutsCodes != null && nutsCodes.contains(Code.createNutsCode(cc.getCode()))) { // entire country, ignore nuts
    		return null;
    	}
    	
    	List<String> res = null;
    	if (nutsCodes != null) {
            Set<String> nuts3Uris = new HashSet<>();
            for (Code s : nutsCodes) {
           		nuts3Uris.addAll(getLocalNutsLeafUris(cc, s));
            }

            res = new ArrayList<>(nuts3Uris);
    	}
    	
    	return res;

    }
    
    public List<String> getLocalNutsLeafUrisDB(CountryDB cc, List<Code> nutsCodes) {
    	
    	if (nutsCodes != null && nutsCodes.contains(Code.createNutsCode(cc.getCode()))) { // entire country, ignore nuts
    		return null;
    	}
    	
    	List<String> res = null;
    	if (nutsCodes != null) {
            Set<String> nuts3Uris = new HashSet<>();
            for (Code s : nutsCodes) {
           		nuts3Uris.addAll(getLocalNutsLeafUrisDB(cc, s));
            }

            res = new ArrayList<>(nuts3Uris);
    	}
    	
    	return res;

    }
    
    public List<String> getLocalNutsLeafUrisDB(CountryDB cc, Code nutsCode) {
    	List<String> res = new ArrayList<>();
    	
    	int level = nutsCode.getNutsLevel();
    	
    	PlaceDB p = new PlaceDB(nutsCode);
    	
    	List<PlaceDB> places = null;
    	if (level == 0) {
    		places = placesRepository.findNUTS0Leaves(p);
    	} else if (level == 1) {
    		places = placesRepository.findNUTS1Leaves(p);
    	} else if (level == 2) {
    		places = placesRepository.findNUTS2Leaves(p);    	
    	} else if (level == 3) {
    		places = new ArrayList<>();
    		places.add(placesRepository.findByCode(nutsCode));
    	}
    	
//    	System.out.println(">>> " + places);

    	for (PlaceDB pp : places) {
            res.add(cc.getNutsPrefix() + pp.getCode().getCode());
        }
    	
//    	System.out.println(">>> " + res);
    	
    	return res;
    }
    
    public Set<String> getLocalNutsLeafUris(CountryDB cc, Code nutsCode) {
    	Set<String> res = new HashSet<>();
    	
    	int level = nutsCode.getNutsLevel();
    	
    	String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  "; 
//    	sparql += nutsNamedgraphEU != null ? "FROM <" + nutsNamedgraphEU + "> " : "";
    	if (level == 0) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader/skos:broader <" + nutsCode.toUri() + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 1) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader/skos:broader <" + nutsCode.toUri() + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 2) {
			sparql += "SELECT ?nuts3 WHERE { " +
	                   "?nuts3 skos:broader <" + nutsCode.toUri() + "> . " +
	                   "?nuts3 <https://lod.stirdata.eu/nuts/ont/level> 3 } ";
    	} else if (level == 3) {
    		//adjust prefix
            res.add(cc.getNutsPrefix() + nutsCode.getCode());
    		return res;
    	}
    	
//    	System.out.println(">>> " + sparql);
    	
    	try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU, sparql)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String nuts3 = sol.get("nuts3").asResource().toString();
                
                //adjust prefix
                res.add(cc.getNutsPrefix() + Code.fromNutsUri(nuts3).getCode());
            }
        }
    	
    	return res;
    }
    
    public List<String> getLocalLauUris(CountryDB cc, List<Code> lauCodes) {
    	
    	List<String> res = null;
    	if (lauCodes != null) {
            Set<String> lauUris = new HashSet<>();
            for (Code s : lauCodes) {
           		lauUris.add(getLocalLauUri(cc, s));
            }

            res = new ArrayList<>(lauUris);
    	}
    	
    	return res;

    }
    
    public String getLocalLauUri(CountryDB cc, Code lauCode) {
  		//adjust prefix
   		return cc.getLauPrefix() + lauCode.getCode();
    }
    
    public Code getLauCodeFromLocalUri(CountryDB cc, String uri) {
  		//adjust prefix
    	return Code.createLauCode(uri.substring(cc.getLauPrefix().length()));
    }
    
    public Code getNutsCodeFromLocalUri(CountryDB cc, String uri) {
  		//adjust prefix
    	return Code.createNutsCode(uri.substring(cc.getNutsPrefix().length()));
    }
    
    public Map<CountryDB, PlaceSelection> getEndpointsByNuts() {
        Map<CountryDB, PlaceSelection> res = new HashMap<>();
        for (CountryDB cc : countryConfigurations.values()) {
       		res.put(cc, null);
        }
        return res;
    }
    
    public Map<CountryDB, PlaceSelection> getEndpointsByNuts(List<Code> nutsLauCodes) {
        Map<CountryDB, PlaceSelection> res = new HashMap<>();

        for (Code code : nutsLauCodes) {
        	
        	if (code.isNuts()) {
        		CountryDB cc = countryConfigurations.get(code.getNutsCountry());
        		
        		PlaceSelection rc = res.get(cc);
        		if (rc == null) {
        			rc = new PlaceSelection();
        			res.put(cc, rc);
        		}
        		
                rc.addNuts3(code);
                
        	} else if (code.isLau()) {
        		CountryDB cc = countryConfigurations.get(code.getLauCountry());
        		
        		PlaceSelection rc = res.get(cc);
        		if (rc == null) {
        			rc = new PlaceSelection();
        			res.put(cc, rc);
        		}
        		
                rc.addLau(code);
        	} 

        }
        
        return res;
    }
    
    public PlaceDB getByCode(Code code) {
    	return placesRepository.findByCode(code);
    }
    
    public List<PlaceDB> getParents(PlaceDB place) {
    	List<PlaceDB> parents = new ArrayList<>();
    	
    	while (place.getParent() != null) {
    		parents.add(0, place.getParent());
    		place = place.getParent();
    	}
    	
    	return parents;
    }
    
    
    public PlaceNode buildPlaceTree(CountryDB cc, boolean lau) {
    	PlaceNode res = new PlaceNode();
    	
    	List<PlaceDB> next = this.getNextDeepestListDb(Code.createNutsCode(cc.getCode()), lau);
    	
    	for (PlaceDB r : next) {
    		res.addChild(r);
    	}
    	
    	for (PlaceNode r : res.getNext()) {
    		buildPlaceTreeIter(r, lau);
    	}
    	
    	return res;
    }
    
    public void buildPlaceTreeIter(PlaceNode place, boolean lau) {
    	List<PlaceDB> next = getNextDeepestListDb(place.getNode().getCode(), lau);
    	
    	for (PlaceDB r : next) {
    		place.addChild(r);
    	}
    	
    	if (place.getNext() != null) {
	    	for (PlaceNode r : place.getNext()) {
	    		buildPlaceTreeIter(r, lau);
	    	}
    	}    	
    }
    
    private PlaceDB getNextDeepestSingle(PlaceDB parent, boolean lau) {
    	PlaceDB res = parent;
    
    	while (true) {
    		
    		if (res.getNumberOfChildren() > 1 || res.isLau() || (!lau && res.getCode().getNutsLevel() == 3)) {
    			break;
    		}
    	
    		res = placesRepository.findByParent(res).get(0);
    	}
    	
    	return res;
    	
    	
    }
    
    //returns deepest non single children nodes (next level may contain nuts and lau if parent nuts has single child).
    public List<PlaceDB> getNextDeepestListDb(Code parent, boolean lau) {
    	List<PlaceDB> places = new ArrayList<>();
    	
    	PlaceDB parentPlace = null;
    	if (parent != null) {
    		parentPlace = (new PlaceDB(parent));
    	} else {
    		return places;
    	}
    	
    	if (!lau && parent.getNutsLevel() == 3) {
    		return places;
    	}

		places = placesRepository.findByParent(parentPlace);
		
		if (places.size() == 1) {
			PlaceDB next = getNextDeepestSingle(places.get(0), lau);
			if (next.getNumberOfChildren() > 0 && (lau || !lau && next.getCode().getNutsLevel() !=3 )) {
				places = placesRepository.findByParent(next);
			} else {
				places.set(0, next);
			}
		}
		
		if (places.size() > 1) {
	    	for (int i = 0; i < places.size(); i++) {
	    		places.set(i, getNextDeepestSingle(places.get(i), lau));
	    	}
		}
		
        return places;
    }
    
    public List<PlaceDB> getNextNutsLauLevelListDb(Code parent) {
    	return getNextDeepestListDb(parent, true);
    }
    
    public List<PlaceDB> getNextNutsLauLevelListDbSameLevel(Code parent) {
    	List<PlaceDB> places = new ArrayList<>();
    	
    	PlaceDB parentPlace = null;
    	if (parent != null) {
    		parentPlace = (new PlaceDB(parent));
    	} else {
    		return places;
    	}

    	while (true) {
    		places = placesRepository.findByParent(parentPlace);

    		// inefficient!
//        	for (int i = 0; i < places.size();) {
//        		if (!places.get(i).isNuts()) { // we are in lau
//        			break;
//        		}
//        		
//       			i++;
//        	}
        	
            if (places.size() == 1) {
            	PlaceDB place = places.get(0); 
            
            	if (place.isNuts()) {
            		parentPlace = place;
            	} else if (place.isLau()) {
            		break;
            	}
            } else {
            	break;
            }
    	}
    	
        return places;
    }
    
    public List<PlaceDB> getNutsLauLevelListDb(List<Code> codes) {
        return placesRepository.findByCodeIn(codes);
    }

    public String getNextNutsLevelJsonTs(String parent, String spatialResolution) {
        String json = "";

		boolean lauChildren = false;
    	while (true) {
    		
    		String sparql = nextNutsLevelSparqlQuery(parent, lauChildren, spatialResolution);
	        
    		String endpoint = !lauChildren ? nutsEndpointEU : lauEndpointEU;
	        try (QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, sparql)) {
	            ResultSet rs = qe.execSelect();
	            
	            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	            ResultSetFormatter.outputAsJSON(outStream, rs);
	            json = outStream.toString();
	            
	            ObjectMapper mapper = new ObjectMapper();  //inefficient way . alternative do the same query twice if needed
	            JsonNode root = mapper.readTree(json);
	            
	            ArrayNode array = (ArrayNode)root.get("results").get("bindings");
	            
	            if (array.size() == 1) {
	            	ObjectNode node = (ObjectNode)array.get(0); 
	            
	            	if (!lauChildren && node.get("level").get("value").asInt() < 3) {
	            		parent = node.get("code").get("value").asText();
	            		lauChildren = false;
	            	} else if (!lauChildren && node.get("level").get("value").asInt() == 3) {
	            		parent = node.get("code").get("value").asText();
	            		lauChildren = true;
	            	} else if (lauChildren) {
	            		break;
	            	}
	            } else if (array.size() == 0) {
	            	if (!lauChildren) {
	            		lauChildren = true;
	            	} else {
	            		break;
	            	}
	            } else {
	            	break;
	            }
	            
	        } catch (Exception e) {
				e.printStackTrace();
				break;
			}
    	}
    	
        return json;
    }
    
    private String nextNutsLevelSparqlQuery(String parent, boolean lauChildren, String spatialResolution) {
        String sparql = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "SELECT ?code ?label ?level " + (spatialResolution != null ? "?geoJson " : "");
        
//        if (!lauChildren) {
//        	sparql += nutsNamedgraphEU != null ? "FROM <" + nutsNamedgraphEU + "> " : "";
//        } else {
//        	sparql += lauNamedgraphEU != null ? "FROM <" + lauNamedgraphEU + "> " : "";
//        }
        
        sparql += " WHERE { ";
	        
        if (parent == null) {
        	String countries = "";
            for (String c : countryConfigurations.keySet()) {
            	countries += "\"" + c + "\" ";
            }
	            
            sparql += "?code <https://lod.stirdata.eu/nuts/ont/level> 0 . "+
                      "?code <http://www.w3.org/2004/02/skos/core#notation> ?notation ."+
                      "VALUES ?notation { " + countries + " }";
        } else {
        	if (lauChildren) {
        		sparql += "?code <https://lod.stirdata.eu/nuts/ont/partOf>" + " <" + parent + "> . ";
        	} else {
        		sparql += "?code <http://www.w3.org/2004/02/skos/core#broader>" + " <" + parent + "> . ";
        	}
        }

        if (!lauChildren) {
	       	sparql += "?code <https://lod.stirdata.eu/nuts/ont/level> ?level . " ;
        } else {
        	sparql += "BIND (4 AS ?level) . " ;
        }
        
	    sparql += "?code <http://www.w3.org/2004/02/skos/core#prefLabel> ?label . ";

	    if (spatialResolution != null) {
	        sparql += "OPTIONAL { ?code <http://www.opengis.net/ont/geosparql#hasGeometry> [ " +
	                  "  <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"" + spatialResolution + "\" ; " +
	                  "  <http://www.opengis.net/ont/geosparql#asGeoJSON> ?geoJson ] . } ";
	    }
	    
	    sparql += "} " ;
	    
	    if (lauChildren) {
	    	sparql += "ORDER BY ?label";
	    } else {
	    	sparql += "ORDER BY ?label";
//        	sparql += " ORDER BY ?code";
	    }
	    
//	    System.out.println(sparql);
	    return sparql;
	}
    


//    public String getNutsGeoJson(String nutsUri, String spatialResolution) {
////        final String sparql = "construct {\n"
////                + "<" + nutsUri+ "> <" + GeoSparqlVocabulary.hasGeometry + "> ?o .\n"
////                + "?o <" + GeoSparqlVocabulary.asGeoJSON + "> ?o2 .\n"
////                + "<" + nutsUri + "> <" + GeoSparqlVocabulary.contains + "> ?o1\n"
////                + "\n"
////                + "}  where {\n"
////                + "<" + nutsUri + "> <" + GeoSparqlVocabulary.hasGeometry + "> ?o.\n"
////                + "?o <" + GeoSparqlVocabulary.hasSpatialResolution + "> \"" + spatialResolution + "\" .\n"
////                + "?o <" + GeoSparqlVocabulary.asGeoJSON + "> ?o2 .\n"
////                + "OPTIONAL { <" + nutsUri + "> <" + GeoSparqlVocabulary.contains + "> ?o1} \n"
////                + "}";
//        final String sparql =
//            "construct {"
//            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#hasGeometry> ?o . "
//            + "?o <http://www.opengis.net/ont/geosparql#asGeoJSON> ?o2 . "
//            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#contains> ?o1 . "
//            + "} where {"
//            + " <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#hasGeometry> ?o . "
//            + "?o <http://www.opengis.net/ont/geosparql#hasSpatialResolution> \"" + spatialResolution + "\" . "
//            + "?o <http://www.opengis.net/ont/geosparql#asGeoJSON> ?o2 . "
//            + "OPTIONAL { <" + nutsUri + "> <http://www.opengis.net/ont/geosparql#contains> ?o1} . "
//            + "}";
//        String res;
//        try (QueryExecution qe = QueryExecutionFactory.sparqlService(nutsEndpointEU, sparql)) {
//            final Model m = qe.execConstruct();
//            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
//            RDFDataMgr.write(outStream, m, RDFFormat.JSONLD);
//            res = outStream.toString();
//        }
//
//        return res;
//    }

}
