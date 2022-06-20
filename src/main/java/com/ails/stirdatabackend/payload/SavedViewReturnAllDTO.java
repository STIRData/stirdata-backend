package com.ails.stirdatabackend.payload;

import java.util.Date;

import com.ails.stirdatabackend.model.SavedView;

import org.bson.types.ObjectId;
import lombok.Data;

@Data
public class SavedViewReturnAllDTO {
    private ObjectId id;
    private String name;
    private Date creationDate;

    public SavedViewReturnAllDTO(SavedView view) {
        this.id = view.getId();
        this.name = view.getName();
        this.creationDate = view.getCreationDate();
    }
}
