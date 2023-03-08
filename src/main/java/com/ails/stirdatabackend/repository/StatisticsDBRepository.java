package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.model.StatisticDB;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface StatisticsDBRepository extends JpaRepository<StatisticDB, Integer> {
	
	  @Transactional
	  public void deleteAllByCountry(String country);

	  @Transactional
	  public void deleteAllByCountryAndDimension(String country, String dimension);
	  
	  @Transactional
	  public void deleteAllByCountryAndDimensionAndPlace(String country, String dimension, String place);

	  @Transactional
	  public void deleteAllByCountryAndDimensionAndReferenceDateNot(String country, String dimension, Date referenceDate);
	  
	  @Transactional
	  public void deleteAllByDimension(String dimension);

	  public List<StatisticDB> findByCountry(String country);

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

      public List<StatisticDB> findByCountryAndDimensionAndFoundingFromDateAndFoundingToDateAndFoundingDateInterval(String country, String dimension, Date foundingFromDate, Date foundingToDate, String foundingDateInterval);
      public List<StatisticDB> findByCountryAndDimensionAndDissolutionFromDateAndDissolutionToDateAndDissolutionDateInterval(String country, String dimension, Date dissolutionFromDate, Date dissolutionToDate, String dissolutionDateInterval);

      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndFoundingFromDateAndFoundingToDateAndFoundingDateInterval(String country, String dimension, PlaceDB place, Date foundingFromDate, Date foundingToDate, String foundingDateInterval);
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndDissolutionFromDateAndDissolutionToDateAndDissolutionDateInterval(String country, String dimension, PlaceDB place, Date dissolutionFromDate, Date dissolutionToDate, String dissolutionDateInterval);

      public List<StatisticDB> findByCountryAndDimensionAndActivityAndFoundingFromDateAndFoundingToDateAndFoundingDateInterval(String country, String dimension, ActivityDB activity, Date foundingFromDate, Date foundingToDate, String foundingDateInterval);
      public List<StatisticDB> findByCountryAndDimensionAndActivityAndDissolutionFromDateAndDissolutionToDateAndDissolutionDateInterval(String country, String dimension, ActivityDB activity, Date dissolutionFromDate, Date dissolutionToDate, String dissolutionDateInterval);

      public List<StatisticDB> findByCountryAndDimensionAndParentActivityAndParentPlaceAndFoundingFromDateGreaterThanEqualAndFoundingToDateLessThanEqualAndFoundingDateInterval(String country, String dimension, ActivityDB parentActivity, PlaceDB parentPlace, Date foundingFromDate, Date foundingToDate, String interval);

	  public List<StatisticDB> findByDimensionAndParentPlace(String dimension, PlaceDB parentPlace);
	  
      public List<StatisticDB> findByDimensionAndActivity(@Param("dimension") String dimension, @Param("activity") ActivityDB activity);

      @Query("SELECT DISTINCT dimension FROM StatisticDB WHERE country = :country")
      List<Dimension> findDimensionsByCountry(@Param("country") String country);

//      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, s.activity, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.parentActivity IS NULL GROUP BY DATE_TRUNC(:range, s.foundingToDate), s.country, s.activity, s.dimension ORDER BY MIN(s.foundingFromDate)")
//      public List<StatisticDB> findByCountryAndDimensionAndParentActivityIsNullAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, s.activity, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.parentActivity IS NULL GROUP BY s.country, s.activity, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentActivityIsNullAndFoundingDateRange2(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval);

//      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, s.activity, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval AND s.parentActivity IS NULL GROUP BY DATE_TRUNC(:range, s.dissolutionToDate), s.country, s.activity, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
//      public List<StatisticDB> findByCountryAndDimensionAndParentActivityIsNullAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, s.activity, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval AND s.parentActivity IS NULL GROUP BY s.country, s.activity, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentActivityIsNullAndDissolutionDateRange2(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval);

//      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.parentPlace IS NULL GROUP BY DATE_TRUNC(:range, s.foundingToDate), s.place, s.dimension ORDER BY MIN(s.foundingFromDate)")
//      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceIsNullAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.parentPlace IS NULL GROUP BY s.place, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceIsNullAndFoundingDateRange2(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval);

//      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval AND s.parentPlace IS NULL GROUP BY DATE_TRUNC(:range, s.dissolutionToDate), s.place, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
//      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceIsNullAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval AND s.parentPlace IS NULL GROUP BY s.place, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceIsNullAndDissolutionDateRange2(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.place = :place GROUP BY s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval AND s.parentPlace = :parentPlace GROUP BY s.place, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("parentPlace") PlaceDB parentPlace, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval);
      
      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY DATE_TRUNC(:range, s.foundingToDate), s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval")  String foundingDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY DATE_TRUNC(:range, s.dissolutionToDate), s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval")  String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.place = :place AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY DATE_TRUNC(:range, s.foundingToDate), s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval") String foundingDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.place = :place AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY DATE_TRUNC(:range, s.dissolutionToDate), s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.place = :place AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndPlaceAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.place, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.place = :place AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY s.place, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentPlaceAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("place") PlaceDB place, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.activity = :activity AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY DATE_TRUNC(:range, s.foundingToDate), s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndActivityAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("activity") ActivityDB activity, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval") String foundingDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.activity = :activity AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndActivityAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("activity") ActivityDB activity, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval") String foundingDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.foundingFromDate), MAX(s.foundingToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.parentActivity = :parentActivity AND s.foundingFromDate >= :foundingFromDate AND s.foundingToDate <= :foundingToDate AND s.foundingDateInterval = :foundingDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.foundingFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentActivityAndFoundingDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("parentActivity") ActivityDB parentActivity, @Param("foundingFromDate") Date foundingFromDate, @Param("foundingToDate") Date foundingToDate, @Param("foundingDateInterval") String foundingDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.activity = :activity AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY DATE_TRUNC(:range, s.dissolutionToDate), s,country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndActivityAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("activity") ActivityDB activity, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval, @Param("range") String range);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.activity = :activity AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndActivityAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("activity") ActivityDB activity, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(s.dimension, s.country, SUM(s.count), MIN(s.dissolutionFromDate), MAX(s.dissolutionToDate)) FROM StatisticDB s WHERE s.country = :country AND s.dimension = :dimension AND s.parentActivity = :parentActivity AND s.dissolutionFromDate >= :dissolutionFromDate AND s.dissolutionToDate <= :dissolutionToDate AND s.dissolutionDateInterval = :dissolutionDateInterval GROUP BY s.country, s.dimension ORDER BY MIN(s.dissolutionFromDate)")
      public List<StatisticDB> findByCountryAndDimensionAndParentActivityAndDissolutionDateRange(@Param("country") String country, @Param("dimension") String dimension, @Param("parentActivity") ActivityDB parentActivity, @Param("dissolutionFromDate") Date dissolutionFromDate, @Param("dissolutionToDate") Date dissolutionToDate, @Param("dissolutionDateInterval") String dissolutionDateInterval);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), s.activity) FROM StatisticDB s WHERE s.dimension = :dimension AND s.parentActivity is null GROUP BY s.activity")
      public List<StatisticDB> findByDimensionAndParentActivityIsNullGroupByActivity(@Param("dimension") String dimension);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), s.activity) FROM StatisticDB s WHERE s.dimension = :dimension AND s.activity = :activity GROUP BY s.activity")
      public List<StatisticDB> findByDimensionAndActivityGroupByActivity(@Param("dimension") String dimension, @Param("activity") ActivityDB activity);

      @Query("SELECT new com.ails.stirdatabackend.model.StatisticDB(SUM(s.count), s.activity) FROM StatisticDB s WHERE s.dimension = :dimension AND s.parentActivity = :parentActivity GROUP BY s.activity")
      public List<StatisticDB> findByDimensionAndParentActivityGroupByActivity(@Param("dimension") String dimension, @Param("parentActivity") ActivityDB parentActivity);


}
