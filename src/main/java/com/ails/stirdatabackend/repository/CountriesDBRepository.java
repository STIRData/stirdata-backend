package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface CountriesDBRepository extends JpaRepository<CountryDB, String> {
	
	  public CountryDB findByCode(String code);

//	  public List<ActivityDB> findByParent(ActivityDB parent);

}
