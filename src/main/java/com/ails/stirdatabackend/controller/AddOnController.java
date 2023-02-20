package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.AddOn;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/addon")
public class AddOnController {

    @Autowired
    private DataService dataService;
    
    @Autowired
	@Qualifier("country-addons")
    private Map<String, Map<String,AddOn>> addons; 
    
    @GetMapping("/{addon}")
    public ResponseEntity<?> entity(@RequestParam(required = true) String uri, @PathVariable String addon) {
        
    	CountryDB cc = dataService.findCountry(uri);
    	
    	if (cc == null) {
    		return ResponseEntity.notFound().build();
    	}
    	
    	AddOn addOn = addons.get(cc.getCode()).get(addon);
    	
    	if (addOn == null) {
    		return ResponseEntity.notFound().build();
    	}
    	
    	Object res = addOn.ask(uri);

    	return ResponseEntity.ok(res);
    }

}
