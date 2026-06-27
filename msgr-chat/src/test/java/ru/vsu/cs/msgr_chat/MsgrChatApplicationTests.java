package ru.vsu.cs.msgr_chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.vsu.cs.msgr_chat.dto.ChatResponse;
import ru.vsu.cs.msgr_chat.dto.CreateChatRequest;
import ru.vsu.cs.msgr_chat.dto.MessageRequest;
import ru.vsu.cs.msgr_chat.dto.MessageResponse;
import ru.vsu.cs.msgr_chat.entity.*;
import ru.vsu.cs.msgr_chat.repository.ChatMemberRepository;
import ru.vsu.cs.msgr_chat.repository.ChatRepository;
import ru.vsu.cs.msgr_chat.repository.MessageRepository;
import ru.vsu.cs.msgr_chat.repository.UserRepository;
import ru.vsu.cs.msgr_chat.service.ChatService;
import ru.vsu.cs.msgr_chat.service.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("msgr-chat unit tests")
class MsgrChatApplicationTests {

    @Nested
    @DisplayName("Application startup")
    @SpringBootTest
    @ActiveProfiles("test")
    class ApplicationStartupTests {

        @Test
        @DisplayName("Spring context loads successfully")
        void contextLoads() {
        }
    }

    @Nested
    @DisplayName("ChatService")
    @ExtendWith(MockitoExtension.class)
    class ChatServiceUnitTests {

        @Mock
        private ChatRepository chatRepository;

        @Mock
        private ChatMemberRepository chatMemberRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ChatService chatService;

        private User currentUser;
        private User targetUser;

        @BeforeEach
        void setUp() {
            currentUser = User.builder().id(1L).username("ivanov")
                    .firstName("Ivan").lastName("Ivanov").build();
            targetUser = User.builder().id(2L).username("petrov")
                    .firstName("Petr").lastName("Petrov").build();
        }

        @Test
        @DisplayName("createPrivateChat creates new chat between two users")
        void createPrivateChatSuccess() {
            CreateChatRequest request = CreateChatRequest.builder()
                    .targetUsername("petrov")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("petrov")).thenReturn(Optional.of(targetUser));
            when(chatRepository.findPrivateChatBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
            when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> {
                Chat chat = inv.getArgument(0);
                chat.setId(10L);
                chat.setMembers(new ArrayList<>());
                return chat;
            });

            ChatResponse response = chatService.createPrivateChat(1L, request);

            assertEquals(10L, response.getId());
            assertEquals("PRIVATE", response.getType());
            verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
        }

        @Test
        @DisplayName("createPrivateChat fails when chatting with yourself")
        void createPrivateChatWithSelf() {
            CreateChatRequest request = CreateChatRequest.builder()
                    .targetUsername("ivanov")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(userRepository.findByUsername("ivanov")).thenReturn(Optional.of(currentUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.createPrivateChat(1L, request));

            assertEquals("Нельзя создать чат с самим собой", ex.getMessage());
        }

        @Test
        @DisplayName("getUserChats returns user chat list")
        void getUserChats() {
            Chat chat = Chat.builder()
                    .id(5L)
                    .type(ChatType.PRIVATE)
                    .createdBy(currentUser)
                    .members(new ArrayList<>())
                    .build();

            when(chatRepository.findByUserId(1L)).thenReturn(List.of(chat));

            List<ChatResponse> chats = chatService.getUserChats(1L);

            assertEquals(1, chats.size());
            assertEquals(5L, chats.get(0).getId());
        }
    }

    @Nested
    @DisplayName("MessageService")
    @ExtendWith(MockitoExtension.class)
    class MessageServiceUnitTests {

        @Mock
        private MessageRepository messageRepository;

        @Mock
        private ChatRepository chatRepository;

        @Mock
        private ChatMemberRepository chatMemberRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private MessageService messageService;

        private User sender;
        private Chat chat;

        @BeforeEach
        void setUp() {
            sender = User.builder().id(1L).username("ivanov").build();
            chat = Chat.builder().id(10L).type(ChatType.PRIVATE).build();
        }

        @Test
        @DisplayName("sendMessage fails when content and file are empty")
        void sendMessageEmpty() {
            MessageRequest request = MessageRequest.builder()
                    .chatId(10L)
                    .content("  ")
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> messageService.sendMessage(1L, request));

            assertEquals("Сообщение должно содержать текст или файл", ex.getMessage());
        }

        @Test
        @DisplayName("sendMessage saves text message for chat member")
        void sendMessageSuccess() {
            MessageRequest request = MessageRequest.builder()
                    .chatId(10L)
                    .content("Hello!")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(chatRepository.findById(10L)).thenReturn(Optional.of(chat));
            when(chatMemberRepository.existsByChatAndUser(chat, sender)).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message msg = inv.getArgument(0);
                msg.setId(100L);
                return msg;
            });

            MessageResponse response = messageService.sendMessage(1L, request);

            assertEquals(100L, response.getId());
            assertEquals("Hello!", response.getContent());
            assertEquals("TEXT", response.getType());
            verify(messageRepository).save(any(Message.class));
        }
    }
}
