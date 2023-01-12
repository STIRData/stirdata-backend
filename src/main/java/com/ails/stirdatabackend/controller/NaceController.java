package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.payload.ComplexResponse;
import com.ails.stirdatabackend.payload.GenericResponse;
import com.ails.stirdatabackend.service.NaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nace")
public class NaceController {

    @Autowired
    private NaceService naceService;

//    // old: from triples store
//    @GetMapping(value = "/ts", produces = "application/json")
//    public ResponseEntity<?> getNaceTs(@RequestParam(required = false) Optional<String> parent,
//                                       @RequestParam(required = false) Optional<String> language) {
//
//        String res = naceService.getNextNaceLevelJsonTs(parent.orElse(null), language.orElse("en"));
//
//        return ResponseEntity.ok(res);
//    }
    
    @GetMapping(produces = "application/json")
    public ResponseEntity<?> getNace(@RequestParam(required = false) Code top,
                                     @RequestParam(defaultValue = "en") String language) {

    	ActivityDB parent = null;
    	List<ActivityDB> activities = new ArrayList<>();
    	
    	if (top == null) {
    		activities = naceService.getNextNaceLevelListDb(null);
    	} else {
    		if (top.isNaceRev2()) {
    			parent = naceService.getByCode(top);
    			activities = naceService.getNextNaceLevelListDb(top);
    		}
    	}

		ComplexResponse res = new ComplexResponse();
		if (parent != null) {
			res.setEntity(GenericResponse.createFromActivity(parent, language));
		}
		
		if (activities.size() > 0) {
			res.setActivities(activities.stream().map(item -> GenericResponse.createFromActivity(item, language)).collect(Collectors.toList()));
		}
		
        return ResponseEntity.ok(res);
    }

	@GetMapping(value = "/getByCode", produces = "application/json")
    public ResponseEntity<?> getNuts(@RequestParam String naceCode, @RequestParam Optional<String> language) {
		Map<String, String> responseMap = new HashMap<>();
		ActivityDB activity = naceService.getByCode(new Code(naceCode));
		if (activity == null) {
			responseMap.put("error", "Activity does not exist with requested code");
			return ResponseEntity.badRequest().body(responseMap);
		}
        String label;
		if (language.isPresent()) {
			label = activity.getLabel(language.get());
		}
		else {
			label = activity.getLabel(null);
		}
		responseMap.put("code", naceCode);
		responseMap.put("label", label);
        return ResponseEntity.ok(responseMap);
    }

}
