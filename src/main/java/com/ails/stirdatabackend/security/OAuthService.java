package com.ails.stirdatabackend.security;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.GoogleAPIResponse;
import com.ails.stirdatabackend.repository.UserRepository;
import com.ails.stirdatabackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.vocabulary.OA;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OAuthService {

    @Autowired
    private UserService userService;
    private RestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public OAuthService (RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String googleOauthVerify(String token) {
        String url = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + token;
        System.out.println(url);
        GoogleAPIResponse jsonResponse = restTemplate.getForObject(url, GoogleAPIResponse.class);
        System.out.printf(jsonResponse.getEmail());

        User u = userService.checkAndCreateNewUser(jsonResponse);
        String jwt = tokenProvider.generateToken(u.getId().toString());

        return jwt;
    }

    public void solidOauthVerify(String token) {

    }

}
