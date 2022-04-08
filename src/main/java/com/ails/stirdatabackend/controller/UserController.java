package com.ails.stirdatabackend.controller;

import java.util.Optional;

import javax.validation.Valid;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.LoginRequestDTO;
import com.ails.stirdatabackend.payload.Message;
import com.ails.stirdatabackend.payload.OAuthResponse;
import com.ails.stirdatabackend.payload.UserRegistrationDTO;
import com.ails.stirdatabackend.security.UserPrincipal;
import com.ails.stirdatabackend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> emailPasswordRegistration(@RequestBody @Valid UserRegistrationDTO registrationRequest) {
        try {
            Optional<User> userOpt = userService.registerUser(registrationRequest);
            if (userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(userOpt.get());
            }
            else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Message("User already exists."));
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }        
    }

    @PostMapping("/login")
    public ResponseEntity<?> userLogin(@RequestBody @Valid LoginRequestDTO loginRequest) {
        try {
            Optional<String> tokenOpt = userService.loginUser(loginRequest);
            if (tokenOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(new OAuthResponse(tokenOpt.get()));
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }        
    }

    @GetMapping("/me")
    public ResponseEntity<?> userDetails(@AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Optional<User> userOpt = userService.getUserById(currentUser.getId().toString());
            if (userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body());
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }  
    }


}
