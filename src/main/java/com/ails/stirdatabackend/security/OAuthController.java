package com.ails.stirdatabackend.security;

import com.ails.stirdatabackend.payload.AuthenticationResponse;
import com.ails.stirdatabackend.payload.Message;
import com.ails.stirdatabackend.payload.OAuthRequest;
import com.ails.stirdatabackend.payload.ResultDTO;

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
                ResultDTO<String> res = oAuthService.googleOauthVerify(oauthRequest.getToken());
                if (res.isError()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Message(res.getError()));

                }
                return  ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponse(res.getResult()));

            } else if (provider.equals("solid")) {
                ResultDTO<String> res = oAuthService.solidOauthVerify(oauthRequest.getToken());
                if (res.isError()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Message(res.getError()));
                }
                return  ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponse(res.getResult()));

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
