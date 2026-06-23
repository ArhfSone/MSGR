package ru.vsu.cs.msgr_chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {
    private String content;
    private Long chatId;
    private String fileUrl;
    private String type;        // ← ДОБАВЛЕНО: TEXT, IMAGE, FILE
    private Long replyToId;
}