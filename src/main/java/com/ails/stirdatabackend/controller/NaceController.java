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

    @GetMapping(produces = "application/json")
    public ResponseEntity<?> getNace(@RequestParam(required = false) Optional<String> parent,
                                     @RequestParam(required = false) Optional<String> language) {

        String res = naceService.getNextNaceLevelJson(parent.orElse(null), language.orElse("en"));

        return ResponseEntity.ok(res);
    }

}
