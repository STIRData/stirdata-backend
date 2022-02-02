package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ActivitiesDBRepository extends JpaRepository<ActivityDB, String> {
	
	  public ActivityDB findByCode(Code code);

	  public List<ActivityDB> findByParent(ActivityDB parent);

}
