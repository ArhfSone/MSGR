package ru.vsu.cs.msgr_auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import ru.vsu.cs.msgr_auth.dto.AuthResponse;
import ru.vsu.cs.msgr_auth.dto.LoginRequest;
import ru.vsu.cs.msgr_auth.dto.RegisterRequest;
import ru.vsu.cs.msgr_auth.entity.User;
import ru.vsu.cs.msgr_auth.repository.UserRepository;
import ru.vsu.cs.msgr_auth.service.AuthService;
import ru.vsu.cs.msgr_auth.service.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("msgr-auth unit tests")
class MsgrApplicationTests {

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
    @DisplayName("AuthService")
    @ExtendWith(MockitoExtension.class)
    class AuthServiceUnitTests {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @InjectMocks
        private AuthService authService;

        private RegisterRequest registerRequest;
        private User savedUser;

        @BeforeEach
        void setUp() {
            registerRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("secret123")
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .username("ivanov")
                    .build();

            savedUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .passwordHash("hashed")
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .username("ivanov")
                    .build();
        }

        @Test
        @DisplayName("register creates user and returns tokens")
        void registerSuccess() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            when(jwtService.generateAccessToken(1L, "ivanov")).thenReturn("access-token");
            when(jwtService.generateRefreshToken(1L)).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

            AuthResponse response = authService.register(registerRequest);

            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals(1L, response.getUserId());
            assertEquals("ivanov", response.getUsername());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("register fails when email already exists")
        void registerDuplicateEmail() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Пользователь с таким email уже существует", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("login returns tokens for valid credentials")
        void loginSuccess() {
            LoginRequest loginRequest = new LoginRequest("test@example.com", "secret123");

            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPasswordHash())).thenReturn(true);
            when(jwtService.generateAccessToken(1L, "ivanov")).thenReturn("access-token");
            when(jwtService.generateRefreshToken(1L)).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

            AuthResponse response = authService.login(loginRequest);

            assertEquals("access-token", response.getAccessToken());
            assertEquals(1L, response.getUserId());
        }

        @Test
        @DisplayName("login fails with wrong password")
        void loginWrongPassword() {
            LoginRequest loginRequest = new LoginRequest("test@example.com", "wrong");

            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPasswordHash())).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Неверный пароль", ex.getMessage());
        }

        @Test
        @DisplayName("refresh fails for invalid token")
        void refreshInvalidToken() {
            when(jwtService.validateRefreshToken("bad-token")).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.refreshToken("bad-token"));

            assertEquals("Невалидный refresh-токен", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("JwtService")
    class JwtServiceUnitTests {

        private JwtService jwtService;

        @BeforeEach
        void setUp() {
            jwtService = new JwtService();
            ReflectionTestUtils.setField(jwtService, "secret",
                    "test-secret-key-minimum-256-bits-long-for-hmac-sha256-algorithm!!!");
            ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
            ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800L);
        }

        @Test
        @DisplayName("generates and validates access token")
        void accessTokenLifecycle() {
            String token = jwtService.generateAccessToken(42L, "testuser");

            assertNotNull(token);
            assertTrue(jwtService.validateAccessToken(token));
            assertEquals(42L, jwtService.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("generates and validates refresh token")
        void refreshTokenLifecycle() {
            String token = jwtService.generateRefreshToken(42L);

            assertTrue(jwtService.validateRefreshToken(token));
            jwtService.revokeRefreshToken(token);
            assertFalse(jwtService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("rejects invalid token string")
        void invalidToken() {
            assertFalse(jwtService.validateAccessToken("not-a-jwt"));
        }
    }
}
