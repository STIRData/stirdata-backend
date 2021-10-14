package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.GoogleAccountUserInfoDTO;
import com.ails.stirdatabackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


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
}
