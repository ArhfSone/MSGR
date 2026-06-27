package ru.vsu.cs.msgr_chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.msgr_chat.dto.*;
import ru.vsu.cs.msgr_chat.service.ChatService;
import ru.vsu.cs.msgr_chat.service.MessageService;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    // ==================== ЛИЧНЫЕ ЧАТЫ ====================

    /**
     * Создание личного чата (один на один)
     */
    @PostMapping
    public ResponseEntity<ChatResponse> createPrivateChat(
            Authentication authentication,
            @Valid @RequestBody CreateChatRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.createPrivateChat(userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== ГРУППОВЫЕ ЧАТЫ ====================

    /**
     * Создание группового чата
     */
    @PostMapping("/group")
    public ResponseEntity<ChatResponse> createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateGroupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.createGroup(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление группы (название, аватар)
     */
    @PutMapping("/{chatId}")
    public ResponseEntity<ChatResponse> updateGroup(
            Authentication authentication,
            @PathVariable Long chatId,
            @Valid @RequestBody UpdateGroupRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.updateGroup(chatId, userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== ОБЩИЕ МЕТОДЫ ДЛЯ ЧАТОВ ====================

    /**
     * Получить список всех чатов текущего пользователя
     */
    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ChatResponse> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    /**
     * Получить информацию о конкретном чате
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(
            Authentication authentication,
            @PathVariable Long chatId) {
        Long userId = (Long) authentication.getPrincipal();
        ChatResponse response = chatService.getChatById(chatId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить чат
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(
            Authentication authentication,
            @PathVariable Long chatId) {
        Long userId = (Long) authentication.getPrincipal();
        chatService.deleteChat(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== УПРАВЛЕНИЕ УЧАСТНИКАМИ ГРУППЫ ====================

    /**
     * Добавить участника в группу
     */
    @PostMapping("/{chatId}/members")
    public ResponseEntity<ChatMemberResponse> addMember(
            Authentication authentication,
            @PathVariable Long chatId,
            @Valid @RequestBody AddMemberRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ChatMemberResponse response = chatService.addMember(chatId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Исключить участника из группы
     */
    @DeleteMapping("/{chatId}/members/{username}")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable Long chatId,
            @PathVariable String username) {
        Long userId = (Long) authentication.getPrincipal();
        chatService.removeMember(chatId, userId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Выйти из группы
     */
    @PostMapping("/{chatId}/leave")
    public ResponseEntity<Void> leaveGroup(
            Authentication authentication,
            @PathVariable Long chatId) {
        Long userId = (Long) authentication.getPrincipal();
        chatService.leaveGroup(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== СООБЩЕНИЯ ====================

    /**
     * Отправить сообщение в чат (REST)
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable Long chatId,
            @Valid @RequestBody MessageRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        request.setChatId(chatId);
        MessageResponse response = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить историю сообщений чата (пагинация)
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        List<MessageResponse> messages = messageService.getChatMessages(chatId, userId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Редактировать текстовое сообщение
     */
    @PutMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            Authentication authentication,
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        MessageResponse response = messageService.editMessage(
                chatId, messageId, userId, request.getContent());
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить сообщение
     */
    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            Authentication authentication,
            @PathVariable Long chatId,
            @PathVariable Long messageId) {
        Long userId = (Long) authentication.getPrincipal();
        messageService.deleteMessage(chatId, messageId, userId);
        return ResponseEntity.noContent().build();
    }
}