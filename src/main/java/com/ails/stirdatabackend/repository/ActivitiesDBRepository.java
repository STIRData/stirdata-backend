package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ActivitiesDBRepository extends JpaRepository<ActivityDB, String> {
	
	  public ActivityDB findByCode(Code code);

	  public List<ActivityDB> findByParent(ActivityDB parent);
	  
      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5 WHERE a5 = :activity AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel5(@Param("activity") ActivityDB activity);

      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5, ActivityDB a6 WHERE a6 = :activity AND a6.parent = a5 AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel6(@Param("activity") ActivityDB activity);

      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5, ActivityDB a6, ActivityDB a7 WHERE a7 = :activity AND a7.parent = a6 AND a6.parent = a5 AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel7(@Param("activity") ActivityDB activity);

}
