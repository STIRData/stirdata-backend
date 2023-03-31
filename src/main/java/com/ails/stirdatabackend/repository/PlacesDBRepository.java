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

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2 WHERE s.level = '3' and s.type = 'NUTS' and s.parent = s1 and s1.parent = s2 and s2.parent = ?1")
      public List<PlaceDB> findNUTS0NUTS3Leaves(PlaceDB nuts0);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1 WHERE s.level = '3' and s.type = 'NUTS' and s.parent = s1 and s1.parent = ?1")
      public List<PlaceDB> findNUTS1NUTS3Leaves(PlaceDB nuts1);

      @Query("SELECT s FROM PlaceDB s WHERE s.level = '3' and s.type = 'NUTS'  and s.parent = ?1")
      public List<PlaceDB> findNUTS2NUTS3Leaves(PlaceDB nuts2);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2, PlaceDB s3 WHERE s.level = '0' and s.type = 'LAU' and s.parent = s1 and s1.parent = s2 and s2.parent = s3 and s3.parent = ?1")
      public List<PlaceDB> findNUTS0LAULeaves(PlaceDB nuts0);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2, PlaceDB s3 WHERE s.level = '0' and s.type = 'LAU' and s.parent = s1 and s1.parent = s2 and s2.parent = ?1")
      public List<PlaceDB> findNUTS1LAULeaves(PlaceDB nuts0);

      @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2, PlaceDB s3 WHERE s.level = '0' and s.type = 'LAU' and s.parent = s1 and s1.parent = ?1")
      public List<PlaceDB> findNUTS2LAULeaves(PlaceDB nuts0);

      @Query("SELECT s FROM PlaceDB s WHERE s.level = '0' and s.type = 'LAU'  and s.parent = ?1")
      public List<PlaceDB> findNUTS3LAULeaves(PlaceDB nuts3);

	@Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(COUNT(s), s.parent) FROM PlaceDB s WHERE s.extraRegio = false GROUP BY s.parent")
    public List<StatisticDB> groupByParentPlace();
	
    @Query("SELECT s FROM PlaceDB s WHERE s.type = 'NUTS' and s.parent = ?1")
    public List<PlaceDB> findNUTSChildren1(PlaceDB nuts);

    @Query("SELECT s FROM PlaceDB s, PlaceDB s1 WHERE s.type = 'NUTS' and s.parent = s1 and s1.parent = ?1")
    public List<PlaceDB> findNUTSChildren2(PlaceDB nuts);

    @Query("SELECT s FROM PlaceDB s, PlaceDB s1, PlaceDB s2 WHERE s.type = 'NUTS' and s.parent = s1 and s1.parent = s2 and s2.parent = ?1")
    public List<PlaceDB> findNUTSChildren3(PlaceDB nuts);



}
