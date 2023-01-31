package com.ails.stirdatabackend.security;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.GoogleAccountUserInfoDTO;
import com.ails.stirdatabackend.payload.ResultDTO;
import com.ails.stirdatabackend.service.UserService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OAuthService {

    @Autowired
    private UserService userService;
    private RestTemplate restTemplate;

    @Value("${app.oauth.google.url.userinfo}")
    private String googleUrl;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public OAuthService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResultDTO<String> googleOauthVerify(String token) {
        String url =  googleUrl + "?access_token=" + token;
        GoogleAccountUserInfoDTO jsonResponse = restTemplate.getForObject(url, GoogleAccountUserInfoDTO.class);

        Optional<User> u = userService.checkAndCreateNewUserGoogle(jsonResponse);
        if (!u.isPresent()) {
            return ResultDTO.fail("Google Login failed. Account with email already exists");
        }

        String jwt = tokenProvider.generateToken(u.get().getId().toString());

        return ResultDTO.ok(jwt);
    }


}
