package ru.vsu.cs.msgr_chat.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private Long id;
    private String type;
    private String name;
    private String avatarUrl;
    private Long createdById;
    private String createdByUsername;
    private List<ChatMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}