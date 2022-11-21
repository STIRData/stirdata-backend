package com.ails.stirdatabackend.controller;

import java.util.Optional;
import java.util.Map;
import javax.validation.Valid;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.model.UserLoginType;
import com.ails.stirdatabackend.payload.AuthenticationResponse;
import com.ails.stirdatabackend.payload.LoginRequestDTO;
import com.ails.stirdatabackend.payload.Message;
import com.ails.stirdatabackend.payload.UserRegistrationDTO;
import com.ails.stirdatabackend.payload.UserResponse;
import com.ails.stirdatabackend.security.UserPrincipal;
import com.ails.stirdatabackend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> emailPasswordRegistration(@RequestBody @Valid UserRegistrationDTO registrationRequest) {
        try {
            Optional<User> userOpt = userService.registerUser(registrationRequest);
            if (userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(userOpt.get()));
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
                return ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponse(tokenOpt.get()));
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }        
    }

    @GetMapping("/me")
    public ResponseEntity<?> userDetails(@AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Optional<User> userOpt = userService.getUserById(currentUser.getId().toString());
            if (userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(userOpt.get()));
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }  
    }

    @PutMapping("/updateUserDetails")
    public ResponseEntity<?> updateUserDetails(@AuthenticationPrincipal UserPrincipal currentUser, @RequestBody Map<String, String> userDetailsUpdate) {
        try {
            Optional<User> userOpt = userService.getUserById(currentUser.getId().toString());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            User user = userOpt.get();
            if (userDetailsUpdate.get("firstName") != null) {
                user.setFirstName(userDetailsUpdate.get("firstName"));
            }
            if (userDetailsUpdate.get("lastName") != null) {
                user.setLastName(userDetailsUpdate.get("lastName"));
            }
            if (userDetailsUpdate.get("organization") != null) {
                user.setOrganization(userDetailsUpdate.get("organization"));
            }
            if (userDetailsUpdate.get("email") != null) {
                user.setEmail(userDetailsUpdate.get("email"));
            }
            if (userDetailsUpdate.get("password") != null && user.getUserLoginType() == UserLoginType.CUSTOM) {
                user.setPassword(userService.encodePassword(userDetailsUpdate.get("password")));
            }

            userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }  
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<?> deleteUserAccount(@AuthenticationPrincipal UserPrincipal currentUser) {
        try{
            Optional<User> userOpt = userService.getUserById(currentUser.getId().toString());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            User user = userOpt.get();
            userService.deleteUser(user);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } 
    }


}
