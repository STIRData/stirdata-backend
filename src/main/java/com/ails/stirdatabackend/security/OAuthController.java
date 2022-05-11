package com.ails.stirdatabackend.security;

import com.ails.stirdatabackend.payload.AuthenticationResponse;
import com.ails.stirdatabackend.payload.OAuthRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    @Autowired
    private OAuthService oAuthService;

    @PostMapping("/authorize/{provider}")
    public ResponseEntity<?> authorize (
            @PathVariable String provider, @RequestBody OAuthRequest oauthRequest) {
        try {
            
            if (provider.equals("google")) {
                final String jwt = oAuthService.googleOauthVerify(oauthRequest.getToken());
                return  ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponse(jwt));

            } else if (provider.equals("solid")) {
                final String jwt = oAuthService.solidOauthVerify(oauthRequest.getToken());
                return  ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponse(jwt));

            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
