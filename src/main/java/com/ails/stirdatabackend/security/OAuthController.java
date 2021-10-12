package com.ails.stirdatabackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @PostMapping("authorize/{provider}")
    public ResponseEntity<String> authorize (
            @PathVariable String provider, @RequestBody String token) {

        return  ResponseEntity.ok().body(token);
    }
}
