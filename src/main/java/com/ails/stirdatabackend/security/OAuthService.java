package com.ails.stirdatabackend.security;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.UserDTO;
import com.ails.stirdatabackend.service.UserService;
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

    public String googleOauthVerify(String token) {
        String url =  googleUrl + "?id_token=" + token;
        UserDTO jsonResponse = restTemplate.getForObject(url, UserDTO.class);

        User u = userService.checkAndCreateNewUser(jsonResponse);
        String jwt = tokenProvider.generateToken(u.getId().toString());

        return jwt;
    }

    public void solidOauthVerify(String token) {

    }

}
