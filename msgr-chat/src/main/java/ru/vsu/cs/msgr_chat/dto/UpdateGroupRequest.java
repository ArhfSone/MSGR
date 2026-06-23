package ru.vsu.cs.msgr_chat.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGroupRequest {

    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;

    @Size(max = 500, message = "URL аватара не может быть длиннее 500 символов")
    private String avatarUrl;
}