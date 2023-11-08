package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.UpdateLog;
import com.ails.stirdatabackend.model.User;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UpdateLogRepository extends MongoRepository<UpdateLog, String> {
      

}
