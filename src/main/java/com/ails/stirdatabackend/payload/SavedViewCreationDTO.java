package com.ails.stirdatabackend.payload;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SavedViewCreationDTO {

    @NotNull
    private String name;
    private List<String> activity;
    private List<String> place;
    private List<String> feature;
    private String startDate;
    private String endDate;
}
