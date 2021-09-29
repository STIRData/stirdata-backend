package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository {

    Optional<User> findByEmail(String email);
}
