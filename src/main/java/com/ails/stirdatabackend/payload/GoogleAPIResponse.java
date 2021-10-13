package com.ails.stirdatabackend.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleAPIResponse {

    private String given_name;
    private String family_name;
    private String email;
    private String picture;

}
