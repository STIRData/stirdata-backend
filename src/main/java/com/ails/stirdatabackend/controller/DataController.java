package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.DataService;

import org.apache.jena.riot.JsonLDWriteContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataController {

	@Autowired
	@Qualifier("model-jsonld-context")
    private JsonLDWriteContext context;    

	@Autowired
    private DataService dataService;    

//    @GetMapping(value = "/context/stirdata.jsonld", produces = "application/json")
//    public ResponseEntity<?> getContext() {
//		return ResponseEntity.ok(context.getAsString(Symbol.create("http://jena.apache.org/riot/jsonld#JSONLD_CONTEXT")));
//    }
    
    @GetMapping(value = "/entity", produces = "application/json")
    public ResponseEntity<?> getEntity(@RequestParam(required = false) String uri) {
    	return ResponseEntity.ok(dataService.getEntity(uri));
    	
    }

}
