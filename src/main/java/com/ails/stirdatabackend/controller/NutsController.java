package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.payload.ComplexResponse;
import com.ails.stirdatabackend.payload.GenericResponse;
import com.ails.stirdatabackend.service.NutsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nuts")
public class NutsController {

    @Autowired
    private NutsService nutsService;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryDB> countryConfigurations;

//    @Autowired
//    @Qualifier("nuts-geojson-cache")
//    private Cache geojsonCache;
    
//
//    // old: from triples store
//    @GetMapping(value = "/ts", produces = "application/json")
//    public ResponseEntity<?> getNutsTs(@RequestParam(required = false) Optional<String> parent, 
//    		                           @RequestParam(required = false) String spatialResolution) {
//        
//    	String res = nutsService.getNextNutsLevelJsonTs(parent.orElse(null), spatialResolution);
//
//        return ResponseEntity.ok(res);
//    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<?> getNutsDb(@RequestParam(required = false) Code top, 
    		                           @RequestParam(defaultValue = "true") boolean lau,
    		                           @RequestParam(defaultValue = "true") boolean stirdata,
    		                           @RequestParam(required = false) List<String> geometry) {
        
    	PlaceDB parent = null;
    	List<PlaceDB> places = new ArrayList<>();
    	
    	if (top == null) {
//    		places = nutsService.getNextNutsLauLevelListDb(null);
    		places = nutsService.getNextDeepestListDb(null, lau);
    	} else if (top.isStirdata()) { 
	        List<Code> codes = new ArrayList<>();
	        for (CountryDB cc : countryConfigurations.values()) {
	       		codes.add(Code.createNutsCode(cc.getCode()));
	        }
	        places = nutsService.getNutsLauLevelListDb(codes);
    	} else {
    		if (top.isNuts() || top.isLau()) {
    			parent = nutsService.getByCode(top);
    			
    			if (stirdata) {
    				CountryDB cc = countryConfigurations.get(parent.getCountry());
	    			if (!cc.isLau() && !cc.isNuts()) {
	    				// country supports no places
	    			} else {
	//	    			places = nutsService.getNextNutsLauLevelListDb(top);
	    				places = nutsService.getNextDeepestListDb(top, lau);
	    			}
    			} else {
    				places = nutsService.getNextDeepestListDb(top, lau);
    			}
    		}
    	}
    	
		ComplexResponse res = new ComplexResponse();
		if (parent != null) {
			res.setEntity(GenericResponse.createFromPlace(parent, geometry));
		}
		
		if (places.size() > 0) {
			res.setPlaces(places.stream().map(item -> GenericResponse.createFromPlace(item, geometry)).collect(Collectors.toList()));
		}
    	
        return ResponseEntity.ok(res);
    }
    
//    @GetMapping(value = "/getGeoJson", 
//                produces = "application/json")
//    public ResponseEntity<?> getNutsGeoJSON(@RequestParam @NotNull String nutsUri,
//                                            @RequestParam(required = false, defaultValue = "1:1000000") String spatialResolution) {
//        // Check if cached
//        Element e = geojsonCache.get(nutsUri + "&&" + spatialResolution);
//        if (e != null) {
//            return ResponseEntity.ok(e.getObjectValue());
//        }
//
//        final String res = nutsService.getNutsGeoJson(nutsUri, spatialResolution);
//        geojsonCache.put(new Element(nutsUri + "&&" + spatialResolution, res));
//        return ResponseEntity.ok(res);
//    }

}
