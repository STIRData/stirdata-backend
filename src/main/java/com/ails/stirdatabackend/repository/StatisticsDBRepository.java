package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StatisticsDBRepository extends JpaRepository<StatisticDB, Integer> {
//    Optional<User> findByEmail(String email);
	
	  public void deleteAllByCountry(String country);
	
	  public void deleteAllByDimension(String dimension);
	  
	  public void deleteAllByCountryAndDimension(String country, String dimension);
	  
	  public void deleteAllByCountryAndDimensionAndPlace(String country, String dimension, String place);
	  
	  public List<StatisticDB> findByDimension(String dimension);
	  
	  public List<StatisticDB> findByCountryAndDimension(String country, String dimension);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndPlace(String country, String dimension, PlaceDB place);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndActivity(String country, String dimension, ActivityDB activity);

	  public List<StatisticDB> findByCountryAndDimensionAndActivityAndPlace(String country, String dimension, ActivityDB activity, PlaceDB place);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndPlaceAndParentActivity(String country, String dimension, PlaceDB place, ActivityDB parentActivity);
	  public List<StatisticDB> findByCountryAndDimensionAndPlaceAndParentActivityIsNull(String country, String dimension, PlaceDB place);

	  public List<StatisticDB> findByCountryAndDimensionAndActivityAndParentPlace(String country, String dimension, ActivityDB activity, PlaceDB parentPlace);
	  public List<StatisticDB> findByCountryAndDimensionAndActivityAndParentPlaceIsNull(String country, String dimension, ActivityDB activity);

      public List<StatisticDB> findByCountryAndDimensionAndParentPlace(String country, String dimension, PlaceDB parentPlace);
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceIsNull(String country, String dimension);
      
      public List<StatisticDB> findByCountryAndDimensionAndParentActivity(String country, String dimension, ActivityDB parentActivity);
      public List<StatisticDB> findByCountryAndDimensionAndParentActivityIsNull(String country, String dimension);

      public List<StatisticDB> findByCountryAndDimensionAndParentActivityAndParentPlaceAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDateInterval(String country, String dimension, ActivityDB parentActivity, PlaceDB parentPlace, Date fromDate, Date toDate, String interval);

	  public List<StatisticDB> findByDimensionAndParentPlace(String dimension, PlaceDB parentPlace);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND place = :place AND s.fromDate >= :fromDate AND s.toDate <= :toDate")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndDateInterval(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND parentPlace = :parentPlace AND s.fromDate >= :fromDate AND s.toDate <= :toDate GROUP BY s.parentPlace")
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceAndDateInterval(@Param("country") String country, @Param("dimension") String dimension, @Param("parentPlace") PlaceDB parentPlace, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

      @Query("SELECT DISTINCT dimension FROM StatisticDB WHERE country = :country")
      List<Dimension> findDimensionsByCountry(@Param("country") String country);
      
      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), MIN(s.fromDate), MAX(s.toDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.fromDate >= :fromDate AND s.toDate <= :toDate AND s.dateInterval = :dateInterval GROUP BY DATE_TRUNC(:range, s.toDate) ORDER BY MIN(s.fromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate, @Param("dateInterval")  String dateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), MIN(s.fromDate), MAX(s.toDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.place = :place AND s.fromDate >= :fromDate AND s.toDate <= :toDate AND s.dateInterval = :dateInterval GROUP BY DATE_TRUNC(:range, s.toDate) ORDER BY MIN(s.fromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("fromDate") Date fromDate, @Param("toDate") Date toDate, @Param("dateInterval") String dateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), s.activity) FROM StatisticDB s WHERE s.dimension = :dimension AND s.parentActivity is null GROUP BY s.activity")
      public List<StatisticDB> findByDimensionAndParentActivityIsNullGroupByActivity(@Param("dimension") String dimension);

      
}
