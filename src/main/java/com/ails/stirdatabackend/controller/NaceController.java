package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.NaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@CrossOrigin
@RestController
@RequestMapping("/api/nace")
public class NaceController {

    @Autowired
    private NaceService naceService;

    @GetMapping
    public ResponseEntity<?> getNace(@RequestParam(required = false) Optional<String> parent,
                                     @RequestParam(required = false) Optional<String> language) {
        String lang = language.isPresent() ? language.get() : "en";
        String pnt = parent.isPresent() ? parent.get() : null;
        String res = naceService.getNextNaceLevel(pnt, lang);

        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/leafs")
    public ResponseEntity<?> getNaceLeafs(@RequestParam(required = true) String uri, @RequestParam(required = true) String country) {
    	Set<String> res = new HashSet<>();
    	
    	if (country.equalsIgnoreCase("no")) {
    		res = naceService.getLeafNoNaceLeaves(uri);
    	} else if (country.equalsIgnoreCase("be")) {
    		res = naceService.getLeafBeNaceLeaves(uri);
    	} else if (country.equalsIgnoreCase("el")) {
    		res = naceService.getLeafElNaceLeaves(uri);
    	}
        
        
        return ResponseEntity.ok(res.toString());
    }
}
