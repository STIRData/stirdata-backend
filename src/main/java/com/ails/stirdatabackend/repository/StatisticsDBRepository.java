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
	  
	  public List<StatisticDB> findByCountryAndDimension(String country, String dimension);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndPlace(String country, String dimension, PlaceDB place);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndActivity(String country, String dimension, ActivityDB activity);

	  public List<StatisticDB> findByCountryAndDimensionAndActivityAndPlace(String country, String dimension, ActivityDB activity, PlaceDB place);
	  
	  public List<StatisticDB> findByCountryAndDimensionAndPlaceAndParentActivity(String country, String dimension, PlaceDB place, ActivityDB parentActivity);

	  public List<StatisticDB> findByCountryAndDimensionAndActivityAndParentPlace(String country, String dimension, ActivityDB activity, PlaceDB parentPlace);

      public List<StatisticDB> findByCountryAndDimensionAndParentPlace(String country, String dimension, PlaceDB parentPlace);
      
      public List<StatisticDB> findByCountryAndDimensionAndParentActivity(String country, String dimension, ActivityDB parentActivity);

      public List<StatisticDB> findByCountryAndDimensionAndParentActivityAndParentPlaceAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDateInterval(String country, String dimension, ActivityDB parentActivity, PlaceDB parentPlace, Date fromDate, Date toDate, String interval);

      @Query("SELECT DISTINCT dimension FROM StatisticDB WHERE country = :country")
      List<Dimension> findDimensionsByCountry(@Param("country") String country);
      
}
