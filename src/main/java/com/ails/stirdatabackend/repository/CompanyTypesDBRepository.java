package com.ails.stirdatabackend.repository;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CompanyTypeDB;

import org.springframework.data.jpa.repository.JpaRepository;


public interface CompanyTypesDBRepository extends JpaRepository<CompanyTypeDB, String> {
	
	  public void deleteAllByScheme(String scheme);
	
	  public CompanyTypeDB findByCode(Code code);


}
