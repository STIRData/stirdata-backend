package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.payload.ComplexResponse;
import com.ails.stirdatabackend.payload.GenericResponse;
import com.ails.stirdatabackend.service.DataService;
import com.ails.stirdatabackend.service.NaceService;

import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.sparql.util.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController {

	@Autowired
	@Qualifier("model-jsonld-context")
    private JsonLDWriteContext context;    

	@Autowired
    private DataService dataService;    

    @GetMapping(value = "/context/stirdata.jsonld", produces = "application/json")
    public ResponseEntity<?> getContext() {
		return ResponseEntity.ok(context.getAsString(Symbol.create("http://jena.apache.org/riot/jsonld#JSONLD_CONTEXT")));
    }
    
    @GetMapping(value = "/entity", produces = "application/json")
    public ResponseEntity<?> getEntity(@RequestParam(required = false) String uri) {
    	return ResponseEntity.ok(dataService.getEntity(uri));
    	
    }

//    	if (top == null) {
//    		activities = naceService.getNextNaceLevelListDb(null);
//    	} else {
//    		if (top.isNaceRev2()) {
//    			parent = naceService.getByCode(top);
//    			activities = naceService.getNextNaceLevelListDb(top);
//    		}
//    	}
//
//		ComplexResponse res = new ComplexResponse();
//		if (parent != null) {
//			res.setEntity(GenericResponse.createFromActivity(parent, language));
//		}
//		
//		if (activities.size() > 0) {
//			res.setActivities(activities.stream().map(item -> GenericResponse.createFromActivity(item, language)).collect(Collectors.toList()));
//		}
//		
//        return ResponseEntity.ok(res);
//    }

}
