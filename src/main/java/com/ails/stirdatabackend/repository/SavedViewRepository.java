package com.ails.stirdatabackend.repository;

import java.util.List;
import java.util.Optional;

import com.ails.stirdatabackend.model.SavedView;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SavedViewRepository extends MongoRepository<SavedView, ObjectId>{
    List<SavedView> findByCreatorId(ObjectId creatorId);
    Optional<SavedView> findByCreatorIdAndId(ObjectId creatorId, ObjectId id);
}
