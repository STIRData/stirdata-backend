package com.ails.stirdatabackend.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "views")
public class SavedView {

    @Id
    private ObjectId id;
    private ObjectId creatorId;
    private String name;
    private Date creationDate;
    private List<String> activity;
    private List<String> place;
    private String startDate;
    private String endDate;
    private List<String> feature;
    private List<String> eurostat;

    public SavedView() {
        this.creationDate = new Date();
        this.activity = new ArrayList<String>();
        this.place = new ArrayList<String>();
        this.feature = new ArrayList<String>();
        this.eurostat = new ArrayList<String>();
    }

}   
