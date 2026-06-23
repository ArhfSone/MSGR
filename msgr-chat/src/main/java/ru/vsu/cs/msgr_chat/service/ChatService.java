package ru.vsu.cs.msgr_chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vsu.cs.msgr_chat.dto.*;
import ru.vsu.cs.msgr_chat.entity.*;
import ru.vsu.cs.msgr_chat.repository.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    // ==================== ЛИЧНЫЕ ЧАТЫ ====================

    /**
     * Создание личного чата (один на один)
     */
    public ChatResponse createPrivateChat(Long userId, CreateChatRequest request) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        User targetUser = userRepository.findByUsername(request.getTargetUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new RuntimeException("Нельзя создать чат с самим собой");
        }

        // Проверка, существует ли уже чат между этими пользователями
        Chat existingChat = chatRepository.findPrivateChatBetweenUsers(
                currentUser.getId(), targetUser.getId()).orElse(null);

        if (existingChat != null) {
            return mapToChatResponse(existingChat);
        }

        // Создание нового чата
        Chat chat = Chat.builder()
                .type(ChatType.PRIVATE)
                .createdBy(currentUser)
                .build();

        chatRepository.save(chat);

        // Добавление участников
        ChatMember member1 = ChatMember.builder()
                .chat(chat)
                .user(currentUser)
                .build();

        ChatMember member2 = ChatMember.builder()
                .chat(chat)
                .user(targetUser)
                .build();

        chatMemberRepository.save(member1);
        chatMemberRepository.save(member2);

        return mapToChatResponse(chat);
    }

    // ==================== ГРУППОВЫЕ ЧАТЫ ====================

    /**
     * Создание группового чата
     */
    @Transactional
    public ChatResponse createGroup(Long userId, CreateGroupRequest request) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Chat group = Chat.builder()
                .type(ChatType.GROUP)
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(currentUser)
                .build();

        // Добавляем создателя в коллекцию members
        ChatMember ownerMember = ChatMember.builder()
                .chat(group)
                .user(currentUser)
                .role(MemberRole.OWNER)
                .build();
        group.getMembers().add(ownerMember);

        // Добавляем остальных участников в коллекцию members
        for (String username : request.getMemberUsernames()) {
            User member = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь " + username + " не найден"));

            if (member.getId().equals(currentUser.getId())) {
                continue;
            }

            ChatMember chatMember = ChatMember.builder()
                    .chat(group)
                    .user(member)
                    .role(MemberRole.MEMBER)
                    .build();
            group.getMembers().add(chatMember);
        }

        // Каскадное сохранение: сохраняем группу + всех участников одной операцией
        chatRepository.save(group);

        return mapToChatResponse(group);
    }

    /**
     * Обновление группы (название, аватар)
     */
    public ChatResponse updateGroup(Long chatId, Long userId, UpdateGroupRequest request) {
        Chat group = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (group.getType() != ChatType.GROUP) {
            throw new RuntimeException("Это не групповой чат");
        }

        // Проверка прав (только OWNER или ADMIN)
        ChatMember member = chatMemberRepository.findByChatAndUser(group, User.builder().id(userId).build())
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new RuntimeException("Только владелец или администратор может изменять группу");
        }

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getAvatarUrl() != null) {
            group.setAvatarUrl(request.getAvatarUrl());
        }

        chatRepository.save(group);
        return mapToChatResponse(group);
    }

    // ==================== УПРАВЛЕНИЕ УЧАСТНИКАМИ ====================

    /**
     * Добавление участника в группу
     */
    public ChatMemberResponse addMember(Long chatId, Long userId, AddMemberRequest request) {
        Chat group = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (group.getType() != ChatType.GROUP) {
            throw new RuntimeException("Это не групповой чат");
        }

        // Проверка прав (только OWNER или ADMIN)
        ChatMember currentMember = chatMemberRepository.findByChatAndUser(group, User.builder().id(userId).build())
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (currentMember.getRole() != MemberRole.OWNER && currentMember.getRole() != MemberRole.ADMIN) {
            throw new RuntimeException("Только владелец или администратор может добавлять участников");
        }

        User newUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (chatMemberRepository.existsByChatAndUser(group, newUser)) {
            throw new RuntimeException("Пользователь уже является участником группы");
        }

        ChatMember newMember = ChatMember.builder()
                .chat(group)
                .user(newUser)
                .role(MemberRole.MEMBER)
                .build();

        chatMemberRepository.save(newMember);

        return ChatMemberResponse.builder()
                .userId(newUser.getId())
                .username(newUser.getUsername())
                .firstName(newUser.getFirstName())
                .lastName(newUser.getLastName())
                .role(newMember.getRole().name())
                .build();
    }

    /**
     * Исключение участника из группы (только OWNER или ADMIN)
     */
    public void removeMember(Long chatId, Long userId, String usernameToRemove) {
        Chat group = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (group.getType() != ChatType.GROUP) {
            throw new RuntimeException("Это не групповой чат");
        }

        // Проверка прав
        ChatMember currentMember = chatMemberRepository.findByChatAndUser(group, User.builder().id(userId).build())
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (currentMember.getRole() != MemberRole.OWNER && currentMember.getRole() != MemberRole.ADMIN) {
            throw new RuntimeException("Только владелец или администратор может исключать участников");
        }

        User userToRemove = userRepository.findByUsername(usernameToRemove)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя исключить владельца
        if (userToRemove.getId().equals(group.getCreatedBy().getId())) {
            throw new RuntimeException("Нельзя исключить владельца группы");
        }

        ChatMember memberToRemove = chatMemberRepository.findByChatAndUser(group, userToRemove)
                .orElseThrow(() -> new RuntimeException("Пользователь не является участником группы"));

        chatMemberRepository.delete(memberToRemove);
    }

    /**
     * Выход из группы
     */
    public void leaveGroup(Long chatId, Long userId) {
        Chat group = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (group.getType() != ChatType.GROUP) {
            throw new RuntimeException("Это не групповой чат");
        }

        ChatMember member = chatMemberRepository.findByChatAndUser(group, User.builder().id(userId).build())
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        // Владелец не может выйти (должен передать права или удалить группу)
        if (member.getRole() == MemberRole.OWNER) {
            throw new RuntimeException("Владелец не может выйти из группы. Удалите группу или передайте права.");
        }

        chatMemberRepository.delete(member);
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    /**
     * Получить список всех чатов пользователя
     */
    public List<ChatResponse> getUserChats(Long userId) {
        return chatRepository.findByUserId(userId).stream()
                .map(this::mapToChatResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить информацию о конкретном чате
     */
    public ChatResponse getChatById(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Проверка, что пользователь является участником чата
        if (!chatMemberRepository.existsByChatAndUser(chat, User.builder().id(userId).build())) {
            throw new RuntimeException("Вы не являетесь участником этого чата");
        }

        return mapToChatResponse(chat);
    }

    /**
     * Удалить чат
     */
    public void deleteChat(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"));

        // Проверка, что пользователь является участником чата
        if (!chatMemberRepository.existsByChatAndUser(chat, User.builder().id(userId).build())) {
            throw new RuntimeException("Вы не являетесь участником этого чата");
        }

        chatRepository.delete(chat);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Маппинг Entity Chat в DTO ChatResponse
     */
    private ChatResponse mapToChatResponse(Chat chat) {
        List<ChatMemberResponse> members = chat.getMembers().stream()
                .map(member -> ChatMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .username(member.getUser().getUsername())
                        .firstName(member.getUser().getFirstName())
                        .lastName(member.getUser().getLastName())
                        .role(member.getRole().name())
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .id(chat.getId())
                .type(chat.getType().name())
                .name(chat.getName())
                .avatarUrl(chat.getAvatarUrl())
                .createdById(chat.getCreatedBy() != null ? chat.getCreatedBy().getId() : null)
                .createdByUsername(chat.getCreatedBy() != null ? chat.getCreatedBy().getUsername() : null)
                .members(members)
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .build();
    }
}