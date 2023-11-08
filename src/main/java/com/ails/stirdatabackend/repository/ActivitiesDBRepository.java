package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ActivitiesDBRepository extends JpaRepository<ActivityDB, String> {
	
	  public void deleteAllByScheme(String scheme);
	  
	  public List<ActivityDB> findByScheme(String cheme);
	
	  public ActivityDB findByCode(Code code);

	  public List<ActivityDB> findBySchemeAndParent(String scheme, ActivityDB parent);
	  
      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5 WHERE a5 = :activity AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel5(@Param("activity") ActivityDB activity);

      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5, ActivityDB a6 WHERE a6 = :activity AND a6.parent = a5 AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel6(@Param("activity") ActivityDB activity);

      @Query("SELECT s FROM ActivityDB s, ActivityDB a4, ActivityDB a5, ActivityDB a6, ActivityDB a7 WHERE a7 = :activity AND a7.parent = a6 AND a6.parent = a5 AND a5.parent = a4 AND a4.exactMatch = s")
      public ActivityDB findLevel4NaceRev2AncestorFromLevel7(@Param("activity") ActivityDB activity);

      @Query("SELECT a5 FROM ActivityDB a1, ActivityDB a2, ActivityDB a3, ActivityDB a4, ActivityDB a5 WHERE a1.scheme = :scheme AND a1.exactMatch = :activity AND a2.parent = a1 AND a3.parent = a2 AND a4.parent = a3 AND a5.parent = a4")
      public List<ActivityDB> findLevel4AfterDescendentFromNaceRev2(@Param("activity") ActivityDB activity, @Param("scheme") String scheme);

      @Query("SELECT a5 FROM ActivityDB a2, ActivityDB a3, ActivityDB a4, ActivityDB a5 WHERE a2.scheme = :scheme AND a2.exactMatch = :activity AND a3.parent = a2 AND a4.parent = a3 AND a5.parent = a4")
      public List<ActivityDB> findLevel3AfterDescendentFromNaceRev2(@Param("activity") ActivityDB activity, @Param("scheme") String scheme);

      @Query("SELECT a5 FROM ActivityDB a3, ActivityDB a4, ActivityDB a5 WHERE a3.scheme = :scheme AND a3.exactMatch = :activity AND a4.parent = a3 AND a5.parent = a4")
      public List<ActivityDB> findLevel2AfterDescendentFromNaceRev2(@Param("activity") ActivityDB activity, @Param("scheme") String scheme);

      @Query("SELECT a5 FROM ActivityDB a4, ActivityDB a5 WHERE a4.scheme = :scheme AND a4.exactMatch = :activity AND a5.parent = a4")
      public List<ActivityDB> findLevel1AfterDescendentFromNaceRev2(@Param("activity") ActivityDB activity, @Param("scheme") String scheme);

      @Query("SELECT a5 FROM ActivityDB a5 WHERE a5.scheme = :scheme AND a5.exactMatch = :activity")
      public List<ActivityDB> findLevel0AfterDescendentFromNaceRev2(@Param("activity") ActivityDB activity, @Param("scheme") String scheme);

      @Modifying
      @Query("UPDATE ActivityDB a SET a.code = ?2, a.scheme = ?3 where a.code = ?1")
      void changeActivityCode(Code oldCode, Code newCode, String scheme);
      
}
