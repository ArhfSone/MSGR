package ru.vsu.cs.msgr_chat.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private String type;
    private String fileUrl;
    private Long replyToId;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}