package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.NutsService;
import com.sun.istack.internal.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.util.Optional;

@CrossOrigin
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
                                            @RequestParam(required = false, defaultValue = "1000000") String spatialResolution) {
        String res;
        res = nutsService.getNutsGeoJson(nutsUri, spatialResolution);
        return ResponseEntity.ok(res);
    }

}
