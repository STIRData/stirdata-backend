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
    public ResponseEntity<?> getNace(@RequestParam(required = false) Optional<String> parentOpt,
                                     @RequestParam(required = false) Optional<String> languageOpt) {
        String language = languageOpt.isPresent() ? languageOpt.get() : "en";
        String parent = parentOpt.isPresent() ? parentOpt.get() : null;
        String res = naceService.getNace(parent, language);

        return ResponseEntity.ok(res);
    }
    }
