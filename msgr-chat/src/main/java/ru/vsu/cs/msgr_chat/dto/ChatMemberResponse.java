package ru.vsu.cs.msgr_chat.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMemberResponse {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
}