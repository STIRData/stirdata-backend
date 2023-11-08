package com.ails.stirdatabackend.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class UpdateLogAction {

	private LogActionType type;
	private LogState state;
	
	private Date startedAt;
	private Date completedAt;

	private String message;
	
	private List<String> errors;
	
	public UpdateLogAction(LogActionType type) {
		this.type = type;
		this.startedAt = new Date();
		this.state = LogState.RUNNING;
		
	}
	
	public void completed() {
		this.completedAt = new Date();
		this.state = LogState.COMPLETED;
	}

	public void completed(String message) {
		this.completedAt = new Date();
		this.message = message;
		this.state = LogState.COMPLETED;
	}
	
	
	public void failed(String message) {
		this.completedAt = new Date();
		this.message = message;
		this.state = LogState.FAILED;
	}
	
	public void error(String error) {
		if (this.errors != null) {
			this.errors = new ArrayList<>();
			this.errors.add(error);
		}
	}
}
