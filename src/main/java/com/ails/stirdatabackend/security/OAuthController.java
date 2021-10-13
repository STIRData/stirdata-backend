package com.ails.stirdatabackend.security;

import com.ails.stirdatabackend.payload.OAuthResponse;
import com.ails.stirdatabackend.payload.OAuthRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    @Autowired
    private OAuthService oAuthService;

    @PostMapping("/authorize/{provider}")
    public ResponseEntity<?> authorize (
            @PathVariable String provider, @RequestBody OAuthRequest oauthRequest) {

        if (provider.equals("google")) {
            final String jwt = oAuthService.googleOauthVerify(oauthRequest.getToken());
            return  ResponseEntity.status(HttpStatus.OK).body(new OAuthResponse(jwt));

        } else if (provider.equals("solid")) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        } else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
        }
    }
}
