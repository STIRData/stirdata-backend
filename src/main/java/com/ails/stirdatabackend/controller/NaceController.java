package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.service.NaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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
        String res = naceService.getNace(pnt, lang);

        return ResponseEntity.ok(res);
    }
    }
