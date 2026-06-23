package ru.vsu.cs.msgr_chat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.vsu.cs.msgr_chat.dto.MessageRequest;
import ru.vsu.cs.msgr_chat.dto.MessageResponse;
import ru.vsu.cs.msgr_chat.service.JwtService;
import ru.vsu.cs.msgr_chat.service.MessageService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor  // Инжектит только jwtService и messageService
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final MessageService messageService;

    // Создаём ObjectMapper вручную, НЕ через Spring DI
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Хранилище сессий: userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Получение токена из query-параметра
        String query = session.getUri().getQuery();
        if (query == null || !query.contains("token=")) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        String token = query.split("token=")[1];

        if (!jwtService.validateAccessToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtService.getUserIdFromToken(token);
        session.getAttributes().put("userId", userId);
        userSessions.put(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.get("type").asText();

            if ("SEND_MESSAGE".equals(type)) {
                Long chatId = jsonNode.get("chatId").asLong();
                String content = jsonNode.get("content").asText();
                Long replyToId = jsonNode.has("replyToId") && !jsonNode.get("replyToId").isNull()
                        ? jsonNode.get("replyToId").asLong()
                        : null;

                MessageRequest request = MessageRequest.builder()
                        .chatId(chatId)
                        .content(content)
                        .replyToId(replyToId)
                        .build();

                MessageResponse response = messageService.sendMessage(userId, request);

                // Отправка сообщения всем подключённым пользователям
                broadcastMessage(response);
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"" + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    private void broadcastMessage(MessageResponse message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        TextMessage textMessage = new TextMessage(json);

        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }
}