package ru.vsu.cs.msgr_chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditMessageRequest {

    @NotBlank(message = "Текст сообщения не может быть пустым")
    private String content;
}
