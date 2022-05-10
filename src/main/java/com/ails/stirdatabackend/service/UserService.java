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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired 
    private PasswordEncoder passwordEncoder;


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

    public User checkAndCreateNewUserBySolidId(String webId, Optional<String> name, Optional<String> organization, Optional<String> email) {
        Optional<User> userOpt = userRepository.findBySolidWebId(webId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        else {
            User usr = new User();
            usr.setSolidWebId(webId);
            name.ifPresent(n -> usr.setFirstName(nm));
            organization.ifPresent(org -> usr.setOrganization(org));
            email.ifPresent(mail -> usr.setEmail(mail));
            usr.setSolidWebId(webId);
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
            System.out.println(loginRequest.getPassword());
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
            System.out.println("bad creds");
            return Optional.empty();
        }
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

}
