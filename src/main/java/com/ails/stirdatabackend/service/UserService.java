package com.ails.stirdatabackend.service;

import java.util.Optional;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.model.UserLoginType;
import com.ails.stirdatabackend.model.UserType;
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

    @Autowired
    private SavedViewService SavedViewService;


    public Optional<User> checkAndCreateNewUserGoogle(GoogleAccountUserInfoDTO request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent() && userOpt.get().getUserLoginType().equals(UserLoginType.GOOGLE)) {
            return userOpt;
        }
        else if (userOpt.isPresent() && !userOpt.get().getUserLoginType().equals(UserLoginType.GOOGLE)) {
            return Optional.empty();
        }
        else {
            User usr = new User(request);
            userRepository.save(usr);
            return Optional.of(usr);
        }
    }

    public Optional<User> checkAndCreateNewUserSolid( String email, Optional<String> name, Optional<String> organization  ) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getUserLoginType().equals(UserLoginType.SOLID)) {
            return userOpt;
        }
        else if (userOpt.isPresent() && !userOpt.get().getUserLoginType().equals(UserLoginType.SOLID)) {
            return Optional.empty();
        }
        else {

            User usr = new User();
            name.ifPresent(n -> usr.setFirstName(n));
            organization.ifPresent(org -> usr.setOrganization(org));
            usr.setEmail(email);
            usr.setUserLoginType(UserLoginType.SOLID);
            usr.setUserType(UserType.USER);
            userRepository.save(usr);
            return Optional.of(usr);
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

    public String encodePassword(String newPassword) {
        return passwordEncoder.encode(newPassword);
    }

    public void deleteUser(User user) {
        SavedViewService.deleteUserSavedViews(user.getId());
        userRepository.deleteById(user.getId().toString());
    }

    public Optional<User> changeUserPassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Optional.empty();
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return Optional.of(user);
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
            System.out.println("bad creds");
            return Optional.empty();
        }
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public boolean userExistsWithEmail(String email) {
        Optional<User> u =  userRepository.findByEmail(email);
        if (u.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
