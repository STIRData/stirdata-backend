package com.ails.stirdatabackend.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleAccountUserInfoDTO {

    private String given_name;
    private String family_name;
    private String email;
    private String picture;

}
