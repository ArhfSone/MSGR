package ru.vsu.cs.msgr_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupRequest {

    @NotBlank(message = "Название группы обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;

    @Size(max = 500, message = "URL аватара не может быть длиннее 500 символов")
    private String avatarUrl;

    @NotEmpty(message = "Список участников не может быть пустым")
    @Size(max = 100, message = "Максимум 100 участников")
    private List<String> memberUsernames;
}