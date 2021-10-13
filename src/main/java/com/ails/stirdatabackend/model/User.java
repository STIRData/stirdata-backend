package com.ails.stirdatabackend.model;


import com.ails.stirdatabackend.payload.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

        @Id
        private ObjectId id;

        private String email;
        private String firstName;
        private String lastName;
        private UserType userType;


        public User(UserDTO request) {
                this.email = request.getEmail();
                this.firstName = request.getGiven_name();
                this.lastName = request.getFamily_name();
                this.userType = UserType.USER;

        }

}
