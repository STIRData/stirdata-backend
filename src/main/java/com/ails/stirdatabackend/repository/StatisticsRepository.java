package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.model.User;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatisticsRepository extends MongoRepository<Statistic, String> {
//    Optional<User> findByEmail(String email);
	
	  public void deleteAllByCountry(String country);
	
	  public void deleteAllByCountryAndDimension(String country, Dimension dimension);
	  
	  public void deleteAllByCountryAndDimensionAndPlace(String country, Dimension dimension, String place);
	  
	  public void deleteAllByCountryAndDimensionAndActivity(String country, Dimension dimension, String activity);

	  public void deleteAllByDimension(Dimension dimension);
	  
	  @Aggregation(pipeline = {
			  "{ '$match': { 'country': ?0 } }", 
			  "{ '$group': { '_id': '$dimension', count: { '$sum': 1 }, dimension: {'$first': '$dimension' },  referenceDate: { $max: '$referenceDate'} } }" })
	  public List<Statistic> groupCountDimensionsByCountry(String country);
	  
	  public List<Statistic> findByCountry(String country);
	  
//	  public List<Statistic> findByCountryAndReferenceDate(String country, Date referenceDate);
	  public List<Statistic> findByCountryAndDimensionAndReferenceDate(String country, Dimension dimension, Date referenceDate);
	  
	  public List<Statistic> findByCountryAndDimension(String country, Dimension dimension);
	  
	  public List<Statistic> findByDimension(Dimension dimension);
	  
	  public List<Statistic> findByCountryAndDimensionAndPlace(String country, Dimension dimension, String place);
	  
	  public List<Statistic> findByCountryAndDimensionAndActivity(String country, Dimension dimension, String activity);

	  public List<Statistic> findByCountryAndDimensionAndActivityAndPlace(String country, Dimension dimension, String activity, String place);
	  
	  public List<Statistic> findByCountryAndDimensionAndPlaceAndParentActivity(String country, Dimension dimension, String place, String parentActivity);

	  public List<Statistic> findByCountryAndDimensionAndActivityAndParentPlace(String country, Dimension dimension, String activity, String parentPlace);

      @Query(value = "{ 'country' : ?0, 'dimension' : ?1, 'place' : ?2, 'parentActivity': { $exists: false }  }")
	  public List<Statistic> findByCountryAndDimensionAndPlaceAndNoParentActivity(String country, Dimension dimension, String place);

      @Query(value = "{ 'country' : ?0, 'dimension' : ?1, 'parentActivity': { $exists: false }  }")
	  public List<Statistic> findByCountryAndDimensionAndNoParentActivity(String country, Dimension dimension);

      @Query(value = "{ 'country' : ?0, 'dimension' : ?1, 'activity' : ?2, 'parentPlace': { $exists: false }  }")
	  public List<Statistic> findByCountryAndDimensionAndActivityAndNoParentPlace(String country, Dimension dimension, String activity);

      @Query(value = "{ 'country' : ?0, 'dimension' : ?1, 'parentPlace': { $exists: false }  }")
	  public List<Statistic> findByCountryAndDimensionAndNoParentPlace(String country, Dimension dimension);

      public List<Statistic> findByCountryAndDimensionAndParentPlace(String country, Dimension dimension, String parentPlace);

      public List<Statistic> findByCountryAndDimensionAndParentActivity(String country, Dimension dimension, String parentActivity);
      

}
