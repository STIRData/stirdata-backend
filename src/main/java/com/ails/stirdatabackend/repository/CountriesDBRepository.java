package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.CountryDB;

import org.springframework.data.jpa.repository.JpaRepository;


public interface CountriesDBRepository extends JpaRepository<CountryDB, String> {
	
//	  public CountryDB findByCode(String code);
	  
	  public CountryDB findByDcat(String dcat);

}
