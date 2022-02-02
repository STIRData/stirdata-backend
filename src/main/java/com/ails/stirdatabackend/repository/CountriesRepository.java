package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.CountryConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountriesRepository extends MongoRepository<CountryConfiguration, String> {
//    Optional<User> findByEmail(String email);
	
	  public void deleteAllByCountryCode(String countryCode);
//	  
	  public Optional<CountryConfiguration> findOneByCountryCode(String countryCode);
//	  
//	  public List<Statistic> findAllByCountryAndDimension(String country, Dimension dimension);
}
