package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.CreateNewUserRequest;
import com.ails.stirdatabackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity createNewUser(@RequestBody CreateNewUserRequest newUserRequest) {
        userService.createNewUser();

        return ResponseEntity.ok("");
    }
}
