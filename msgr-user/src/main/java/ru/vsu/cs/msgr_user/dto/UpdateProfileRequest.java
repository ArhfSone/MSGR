package ru.vsu.cs.msgr_user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 100, message = "Имя не может быть длиннее 100 символов")
    private String firstName;

    @Size(max = 100, message = "Фамилия не может быть длиннее 100 символов")
    private String lastName;

    @Size(min = 3, max = 50, message = "Username от 3 до 50 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username может содержать только латиницу, цифры и _")
    private String username;

    @Size(max = 500, message = "URL аватара не может быть длиннее 500 символов")
    private String avatarUrl;
}