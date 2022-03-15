package com.ails.stirdatabackend.payload;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class Page {

	private Integer pageSize;
	private Integer pageNumber;
	private Integer totalResults;
//	private Integer totalPages;
	
}
