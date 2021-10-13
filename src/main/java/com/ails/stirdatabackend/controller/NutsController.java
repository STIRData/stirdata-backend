package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.NutsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/nuts")
public class NutsController {

    @Autowired
    private NutsService nutsService;

    @GetMapping
    public ResponseEntity<?> getNuts(@RequestParam(required = false) Optional<String> parent) {
        String res;
        if (parent.isPresent()) {
            res = nutsService.getNextNutsLevel(parent.get());
        }
        else {
            res = nutsService.getNextNutsLevel(null);
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/getGeoJson")
    public ResponseEntity<?> getNutsGeoJSON(@RequestParam @NotNull String nutsUri,
                                            @RequestParam(required = false, defaultValue = "1:1000000") String spatialResolution) {
        final String res = nutsService.getNutsGeoJson(nutsUri, spatialResolution);
        return ResponseEntity.ok(res);
    }

}
