package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.NaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/nace")
public class NaceController {

    @Autowired
    private NaceService naceService;

    @GetMapping
    public ResponseEntity<?> getNace(@RequestParam(required = false) Optional<String> parent,
                                     @RequestParam(required = false) Optional<String> language) {
        String lang = language.orElse("en");
        String pnt = parent.orElse(null);
        String res = naceService.getNextNaceLevel(pnt, lang);

        return ResponseEntity.ok(res);
    }

}
