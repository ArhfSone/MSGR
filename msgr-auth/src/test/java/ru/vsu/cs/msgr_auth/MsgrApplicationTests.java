package ru.vsu.cs.msgr_auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import ru.vsu.cs.msgr_auth.controller.AuthController;
import ru.vsu.cs.msgr_auth.dto.AuthResponse;
import ru.vsu.cs.msgr_auth.dto.LoginRequest;
import ru.vsu.cs.msgr_auth.dto.RegisterRequest;
import ru.vsu.cs.msgr_auth.entity.User;
import ru.vsu.cs.msgr_auth.exception.GlobalExceptionHandler;
import ru.vsu.cs.msgr_auth.repository.UserRepository;
import ru.vsu.cs.msgr_auth.service.AuthService;
import ru.vsu.cs.msgr_auth.service.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
            assertEquals(3600L, response.getExpiresIn());
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
        @DisplayName("register fails when username already exists")
        void registerDuplicateUsername() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Username уже занят", ex.getMessage());
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
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals(3600L, response.getExpiresIn());
            assertEquals(1L, response.getUserId());
            assertEquals("ivanov", response.getUsername());
        }

        @Test
        @DisplayName("login fails when user not found")
        void loginUserNotFound() {
            LoginRequest loginRequest = new LoginRequest("missing@example.com", "secret123");

            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.login(loginRequest));

            assertEquals("Пользователь не найден", ex.getMessage());
            verify(passwordEncoder, never()).matches(any(), any());
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
            verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("refresh returns new tokens for valid refresh token")
        void refreshSuccess() {
            when(jwtService.validateRefreshToken("valid-refresh")).thenReturn(true);
            when(jwtService.getUserIdFromToken("valid-refresh")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));
            when(jwtService.generateAccessToken(1L, "ivanov")).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(1L)).thenReturn("new-refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

            AuthResponse response = authService.refreshToken("valid-refresh");

            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
            assertEquals(3600L, response.getExpiresIn());
            assertEquals(1L, response.getUserId());
            assertEquals("ivanov", response.getUsername());
            verify(jwtService).revokeRefreshToken("valid-refresh");
        }

        @Test
        @DisplayName("refresh fails for invalid token")
        void refreshInvalidToken() {
            when(jwtService.validateRefreshToken("bad-token")).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.refreshToken("bad-token"));

            assertEquals("Невалидный refresh-токен", ex.getMessage());
            verify(jwtService, never()).revokeRefreshToken(any());
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("refresh fails when user not found")
        void refreshUserNotFound() {
            when(jwtService.validateRefreshToken("valid-refresh")).thenReturn(true);
            when(jwtService.getUserIdFromToken("valid-refresh")).thenReturn(99L);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.refreshToken("valid-refresh"));

            assertEquals("Пользователь не найден", ex.getMessage());
            verify(jwtService, never()).revokeRefreshToken(any());
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
            assertFalse(jwtService.validateRefreshToken("not-a-jwt"));
        }

        @Test
        @DisplayName("refresh token is not valid as access token")
        void refreshTokenNotValidAsAccess() {
            String refreshToken = jwtService.generateRefreshToken(42L);

            assertFalse(jwtService.validateAccessToken(refreshToken));
        }

        @Test
        @DisplayName("access token is not valid as refresh token")
        void accessTokenNotValidAsRefresh() {
            String accessToken = jwtService.generateAccessToken(42L, "testuser");

            assertFalse(jwtService.validateRefreshToken(accessToken));
        }
    }

    @Nested
    @DisplayName("AuthController")
    @WebMvcTest(controllers = AuthController.class)
    @Import(GlobalExceptionHandler.class)
    @AutoConfigureMockMvc(addFilters = false)
    class AuthControllerWebMvcTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @Test
        @DisplayName("POST /api/auth/register returns 200 and tokens")
        void registerReturns200() throws Exception {
            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .expiresIn(3600L)
                    .userId(1L)
                    .username("ivanov")
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "password": "secret123",
                                      "firstName": "Ivan",
                                      "lastName": "Ivanov",
                                      "username": "ivanov"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.username").value("ivanov"));
        }

        @Test
        @DisplayName("POST /api/auth/register returns 400 on validation error")
        void registerValidationError() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "not-an-email",
                                      "password": "123",
                                      "firstName": "",
                                      "lastName": "",
                                      "username": "ab"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.password").exists())
                    .andExpect(jsonPath("$.username").exists());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("POST /api/auth/login returns 200")
        void loginReturns200() throws Exception {
            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .expiresIn(3600L)
                    .userId(1L)
                    .username("ivanov")
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "password": "secret123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.userId").value(1));
        }

        @Test
        @DisplayName("POST /api/auth/login returns 400 on validation error")
        void loginValidationError() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "",
                                      "password": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.password").exists());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("POST /api/auth/refresh returns 200")
        void refreshReturns200() throws Exception {
            AuthResponse response = AuthResponse.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(3600L)
                    .userId(1L)
                    .username("ivanov")
                    .build();

            when(authService.refreshToken("old-refresh-token")).thenReturn(response);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"old-refresh-token\""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("POST /api/auth/login returns 400 when service throws RuntimeException")
        void loginServiceErrorReturns400() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Неверный пароль"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "password": "wrong"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Неверный пароль"));
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler")
    @WebMvcTest(controllers = AuthController.class)
    @Import(GlobalExceptionHandler.class)
    @AutoConfigureMockMvc(addFilters = false)
    class GlobalExceptionHandlerTests {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @Test
        @DisplayName("RuntimeException returns 400 with error field")
        void runtimeExceptionReturns400() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new RuntimeException("Пользователь с таким email уже существует"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "test@example.com",
                                      "password": "secret123",
                                      "firstName": "Ivan",
                                      "lastName": "Ivanov",
                                      "username": "ivanov"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error")
                            .value("Пользователь с таким email уже существует"));
        }

        @Test
        @DisplayName("MethodArgumentNotValidException returns field errors")
        void validationExceptionReturnsFieldErrors() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "bad-email",
                                      "password": "123",
                                      "firstName": "",
                                      "lastName": "",
                                      "username": "1"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.password").exists())
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists())
                    .andExpect(jsonPath("$.username").exists());
        }
    }
}