package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.payload.Country;
import com.ails.stirdatabackend.payload.Message;
import com.ails.stirdatabackend.repository.CountriesDBRepository;
import com.ails.stirdatabackend.service.CountriesService;
import com.ails.stirdatabackend.service.NutsService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//@RestController
//@RequestMapping("/api/countries")
//public class CountryController {
//
//    @Autowired
//    private CountriesService countriesService;
//
////    @PostMapping(name = "/add", produces = "aplication/json")
////	public ResponseEntity<?> add(@RequestBody Country country)  {
////
////    	boolean ok = countriesService.add(country);
////
////    	if (ok) {
////    		return ResponseEntity.ok(new Message("Country " + country.getCode() + " already exists."));
////    	} else {
////    		return ResponseEntity.ok(new Message("Country " + country.getCode() + " added."));
////    	}
////    }
//
//	@PostMapping(value = "/reload")
//	public ResponseEntity<?> reload()  {
//
//		countriesService.reload();
//
//		return ResponseEntity.ok().build();
//	}
//}
