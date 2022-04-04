package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface PlacesDBRepository extends JpaRepository<PlaceDB, String> {

	  public PlaceDB findByCode(Code code);

	  @Query("SELECT s FROM PlaceDB s WHERE s.parent = ?1 AND s.extraRegio = false")
	  public List<PlaceDB> findByParent(PlaceDB parent);
	  
	  public List<PlaceDB> findByType(String type);
	  
	  public List<PlaceDB> findByCodeIn(List<Code> codes);
	  
      @Query("SELECT s FROM PlaceDB s WHERE s.level = '3' and s.parent = ?1")
      public List<PlaceDB> findNUTS2Leaves(PlaceDB nuts2);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1 WHERE s.level = '3' and s.parent = s1 and s1.parent = ?1")
      public List<PlaceDB> findNUTS1Leaves(PlaceDB nuts2);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2 WHERE s.level = '3' and s.parent = s1 and s1.parent = s2 and s2.parent = ?1")
      public List<PlaceDB> findNUTS0Leaves(PlaceDB nuts2);

	@Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(COUNT(s), s.parent) FROM PlaceDB s WHERE s.extraRegio = false GROUP BY s.parent")
    public List<StatisticDB> groupByParentPlace();

}
