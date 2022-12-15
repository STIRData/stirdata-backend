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
@Document(collection = "logs")
public class UpdateLog {
	   @Id
	   private String id;
	   
	   private String dcat;
	   
	   private LogState state;
	   
	   private String country;

	   private Date startedAt;
	   
	   private Date completedAt;
	   
	   private LogActionType type;

	   private List<UpdateLogAction> actions;
	   
	   public UpdateLog() {
		   startedAt = new Date();
		   state = LogState.RUNNING;
	   }
	   
	   public void addAction(UpdateLogAction action) {
		   if (actions == null) {
			   actions = new ArrayList<>();
		   }
		   
		   actions.add(action);
	   }
	   
	   public void completed() {
		   this.completedAt = new Date();
		   this.state = LogState.COMPLETED;
	   }
	   
	   public void failed() {
		   this.completedAt = new Date();
		   this.state = LogState.FAILED;
	   }

}
