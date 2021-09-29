package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.CreateNewUserRequest;
import com.ails.stirdatabackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.ValidationException;

import static java.lang.String.format;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

//     checks if user exists, creates new user, calls repository to save it.
    @Transactional
    public void createNewUser(CreateNewUserRequest request) throws ValidationException {
        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("An account for this e-mail already exists.");
        }

        final User user = new User(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
    }

//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        return userRepository
//                .findByEmail(email)
//                .orElseThrow(
//                        () -> new UsernameNotFoundException(format("User with email - %s not found.", email))
//                );
//    }
}
