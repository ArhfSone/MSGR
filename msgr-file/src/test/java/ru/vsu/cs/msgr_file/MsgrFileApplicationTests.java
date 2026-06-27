package ru.vsu.cs.msgr_file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.msgr_file.config.FileStorageConfig;
import ru.vsu.cs.msgr_file.dto.FileResponse;
import ru.vsu.cs.msgr_file.entity.FileEntity;
import ru.vsu.cs.msgr_file.entity.User;
import ru.vsu.cs.msgr_file.repository.FileRepository;
import ru.vsu.cs.msgr_file.repository.UserRepository;
import ru.vsu.cs.msgr_file.service.FileService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("msgr-file unit tests")
class MsgrFileApplicationTests {

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
    @DisplayName("FileService")
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class FileServiceUnitTests {

        @Mock
        private FileRepository fileRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private FileStorageConfig config;

        @InjectMocks
        private FileService fileService;

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder().id(1L).username("ivanov").build();
            when(config.getMaxSize()).thenReturn(536870912L);
            when(config.getAllowedTypes()).thenReturn(List.of("image/jpeg", "image/png"));
            when(config.getUploadDir()).thenReturn(System.getProperty("java.io.tmpdir") + "/msgr-file-unit");
        }

        @Test
        @DisplayName("uploadFile fails for empty file")
        void uploadEmptyFile() {
            MultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> fileService.uploadFile(1L, emptyFile));

            assertEquals("Файл пустой", ex.getMessage());
        }

        @Test
        @DisplayName("uploadFile fails for disallowed mime type")
        void uploadInvalidMimeType() {
            MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> fileService.uploadFile(1L, file));

            assertTrue(ex.getMessage().contains("Недопустимый тип файла"));
        }

        @Test
        @DisplayName("getFileInfo throws when file not found")
        void getFileInfoNotFound() {
            when(fileRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> fileService.getFileInfo(99L));

            assertEquals("Файл не найден", ex.getMessage());
        }

        @Test
        @DisplayName("getFileInfo returns file metadata")
        void getFileInfoSuccess() {
            FileEntity entity = FileEntity.builder()
                    .id(5L)
                    .originalName("photo.png")
                    .storedName("uuid.png")
                    .filePath("/tmp/uuid.png")
                    .fileSize(1024L)
                    .mimeType("image/png")
                    .uploadedBy(user)
                    .build();

            when(fileRepository.findById(5L)).thenReturn(Optional.of(entity));

            FileResponse response = fileService.getFileInfo(5L);

            assertEquals(5L, response.getId());
            assertEquals("photo.png", response.getOriginalName());
            assertEquals("image/png", response.getMimeType());
            assertEquals("/api/files/5", response.getUrl());
        }
    }
}
