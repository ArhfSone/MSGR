package ru.vsu.cs.msgr_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatRequest {
    @NotBlank(message = "Username собеседника обязателен")
    private String targetUsername;
}