package com.ails.stirdatabackend.repository;

import java.util.Optional;

import com.ails.stirdatabackend.model.User;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySolidWebId(String solidWebId);
}
