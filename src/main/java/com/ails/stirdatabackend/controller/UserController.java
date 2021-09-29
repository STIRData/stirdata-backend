package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.payload.CreateNewUserRequest;
import com.ails.stirdatabackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.xml.bind.ValidationException;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity createNewUser(@RequestBody @Valid CreateNewUserRequest newUserRequest) {
        try {
            userService.createNewUser(newUserRequest);
            return ResponseEntity.ok(null);
        }
        catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
