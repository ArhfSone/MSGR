package ru.vsu.cs.msgr_user;

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
import ru.vsu.cs.msgr_user.dto.UpdateProfileRequest;
import ru.vsu.cs.msgr_user.dto.UserResponse;
import ru.vsu.cs.msgr_user.entity.Contact;
import ru.vsu.cs.msgr_user.entity.User;
import ru.vsu.cs.msgr_user.repository.ContactRepository;
import ru.vsu.cs.msgr_user.repository.UserRepository;
import ru.vsu.cs.msgr_user.service.ContactService;
import ru.vsu.cs.msgr_user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("msgr-user unit tests")
class MsgrUserApplicationTests {

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
    @DisplayName("UserService")
    @ExtendWith(MockitoExtension.class)
    class UserServiceUnitTests {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private UserService userService;

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .username("ivanov")
                    .isEmailVerified(true)
                    .build();
        }

        @Test
        @DisplayName("getUserById returns user profile")
        void getUserByIdSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(1L);

            assertEquals(1L, response.getId());
            assertEquals("ivanov", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
        }

        @Test
        @DisplayName("getUserById throws when user not found")
        void getUserByIdNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.getUserById(99L));

            assertEquals("Пользователь не найден", ex.getMessage());
        }

        @Test
        @DisplayName("updateUser changes profile fields")
        void updateUserSuccess() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("petrov")
                    .firstName("Petr")
                    .lastName("Petrov")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("petrov")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.updateUser(1L, request);

            assertEquals("petrov", response.getUsername());
            assertEquals("Petr", response.getFirstName());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("searchUsers returns matching users")
        void searchUsers() {
            when(userRepository.searchUsers("ivan")).thenReturn(List.of(user));

            List<UserResponse> results = userService.searchUsers("ivan");

            assertEquals(1, results.size());
            assertEquals("ivanov", results.get(0).getUsername());
        }
    }

    @Nested
    @DisplayName("ContactService")
    @ExtendWith(MockitoExtension.class)
    class ContactServiceUnitTests {

        @Mock
        private ContactRepository contactRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ContactService contactService;

        private User user;
        private User contactUser;

        @BeforeEach
        void setUp() {
            user = User.builder().id(1L).username("ivanov").email("a@test.com")
                    .firstName("Ivan").lastName("Ivanov").isEmailVerified(true).build();
            contactUser = User.builder().id(2L).username("petrov").email("b@test.com")
                    .firstName("Petr").lastName("Petrov").isEmailVerified(true).build();
        }

        @Test
        @DisplayName("addContact saves new contact")
        void addContactSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("petrov")).thenReturn(Optional.of(contactUser));
            when(contactRepository.existsByUserAndContact(user, contactUser)).thenReturn(false);
            when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = contactService.addContact(1L, "petrov");

            assertEquals(2L, response.getId());
            assertEquals("petrov", response.getUsername());
            verify(contactRepository).save(any(Contact.class));
        }

        @Test
        @DisplayName("addContact fails when adding yourself")
        void addContactSelf() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findByUsername("ivanov")).thenReturn(Optional.of(user));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> contactService.addContact(1L, "ivanov"));

            assertEquals("Нельзя добавить себя в контакты", ex.getMessage());
        }

        @Test
        @DisplayName("getContacts returns contact list")
        void getContacts() {
            Contact contact = Contact.builder().user(user).contact(contactUser).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(contactRepository.findByUser(user)).thenReturn(List.of(contact));

            List<UserResponse> contacts = contactService.getContacts(1L);

            assertEquals(1, contacts.size());
            assertEquals("petrov", contacts.get(0).getUsername());
        }
    }
}
