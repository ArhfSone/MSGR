package ru.vsu.cs.msgr_chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.msgr_chat.dto.MessageRequest;
import ru.vsu.cs.msgr_chat.dto.MessageResponse;
import ru.vsu.cs.msgr_chat.entity.Chat;
import ru.vsu.cs.msgr_chat.entity.Message;
import ru.vsu.cs.msgr_chat.entity.MessageType;
import ru.vsu.cs.msgr_chat.entity.User;
import ru.vsu.cs.msgr_chat.repository.ChatMemberRepository;
import ru.vsu.cs.msgr_chat.repository.ChatRepository;
import ru.vsu.cs.msgr_chat.repository.MessageRepository;
import ru.vsu.cs.msgr_chat.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * Отправка сообщения (текст, изображение или файл)
     */
    @Transactional
    public MessageResponse sendMessage(Long userId, MessageRequest request) {
        // Проверка: должно быть либо content, либо fileUrl
        boolean hasContent = request.getContent() != null
                && !request.getContent().trim().isEmpty();
        boolean hasFile = request.getFileUrl() != null
                && !request.getFileUrl().trim().isEmpty();

        if (!hasContent && !hasFile) {
            throw new RuntimeException(
                    "Сообщение должно содержать текст или файл");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Проверка, что пользователь является участником чата
        if (!chatMemberRepository.existsByChatAndUser(chat, sender)) {
            throw new RuntimeException("Вы не являетесь участником этого чата");
        }

        Message replyTo = null;
        if (request.getReplyToId() != null) {
            replyTo = messageRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new RuntimeException(
                            "Сообщение для ответа не найдено"));
        }

        // Определяем тип сообщения
        MessageType messageType;

        // Если тип передан в запросе — используем его
        if (request.getType() != null && !request.getType().trim().isEmpty()) {
            try {
                messageType = MessageType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Если тип невалидный — определяем автоматически
                messageType = determineMessageType(hasFile, hasContent, request.getFileUrl());
            }
        } else {
            // Иначе определяем автоматически
            messageType = determineMessageType(hasFile, hasContent, request.getFileUrl());
        }

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(hasContent ? request.getContent().trim() : null)
                .fileUrl(hasFile ? request.getFileUrl() : null)
                .type(messageType)
                .replyTo(replyTo)
                .build();

        messageRepository.save(message);
        return mapToMessageResponse(message);
    }

    /**
     * Автоматическое определение типа сообщения
     */
    private MessageType determineMessageType(boolean hasFile, boolean hasContent, String fileUrl) {
        if (hasFile) {
            String url = fileUrl.toLowerCase();
            if (url.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp).*") ||
                    url.contains("image/")) {
                return MessageType.IMAGE;
            } else {
                return MessageType.FILE;
            }
        } else {
            return MessageType.TEXT;
        }
    }

    /**
     * Получение истории сообщений чата с пагинацией
     */
    public List<MessageResponse> getChatMessages(Long chatId, Long userId,
                                                 int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Проверка, что пользователь является участником чата
        if (!chatMemberRepository.existsByChatAndUser(chat, User.builder().id(userId).build())) {
            throw new RuntimeException("Вы не являетесь участником этого чата");
        }

        Page<Message> messages = messageRepository.findByChatOrderByCreatedAtDesc(
                chat, PageRequest.of(page, size));

        return messages.getContent().stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Маппинг Entity Message в DTO MessageResponse
     */
    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .type(message.getType().name())
                .fileUrl(message.getFileUrl())
                .replyToId(message.getReplyTo() != null ? message.getReplyTo().getId() : null)
                .isEdited(message.getIsEdited())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}