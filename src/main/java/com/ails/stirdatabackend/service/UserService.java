package com.ails.stirdatabackend.service;

import java.util.Optional;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.GoogleAccountUserInfoDTO;
import com.ails.stirdatabackend.payload.LoginRequestDTO;
import com.ails.stirdatabackend.payload.UserRegistrationDTO;
import com.ails.stirdatabackend.repository.UserRepository;
import com.ails.stirdatabackend.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider tokenProvider;


    public User checkAndCreateNewUser(GoogleAccountUserInfoDTO request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        else {
            User usr = new User(request);
            userRepository.save(usr);
            return usr;
        }
    }

    public Optional<User> registerUser(UserRegistrationDTO registrationRequest) {
        Optional<User> userOpt = userRepository.findByEmail(registrationRequest.getEmail());
        if (userOpt.isPresent()) {
            return Optional.empty();
        }
        else {
            User u = new User(registrationRequest);
            u.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
            userRepository.save(u);
            return Optional.of(u);
        }
    }

    

    public Optional<String> loginUser(LoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                ));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            return Optional.of(jwt);
        }
        catch (BadCredentialsException e) {
            return Optional.empty();
        }
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

}
