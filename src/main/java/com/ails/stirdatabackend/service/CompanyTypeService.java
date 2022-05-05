package com.ails.stirdatabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CompanyTypeDB;
import com.ails.stirdatabackend.repository.CompanyTypesDBRepository;

@Service
public class CompanyTypeService {

//	private final static Logger logger = LoggerFactory.getLogger(NaceService.class);
	

    @Autowired
    private CompanyTypesDBRepository companyTypesRepository;

    
    public CompanyTypeDB getByCode(Code code) {
    	return companyTypesRepository.findByCode(code);
    }


}
