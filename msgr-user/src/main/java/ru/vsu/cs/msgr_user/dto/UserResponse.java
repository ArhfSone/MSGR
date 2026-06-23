package ru.vsu.cs.msgr_user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String avatarUrl;
    private Boolean isEmailVerified;
}