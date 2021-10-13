package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.CreateNewUserRequest;
import com.ails.stirdatabackend.payload.UserDTO;
import com.ails.stirdatabackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.ValidationException;

import java.util.Optional;

import static java.lang.String.format;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public User checkAndCreateNewUser(UserDTO request) {
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
