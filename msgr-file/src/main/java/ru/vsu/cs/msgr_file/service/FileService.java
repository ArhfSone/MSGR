package ru.vsu.cs.msgr_file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.msgr_file.config.FileStorageConfig;
import ru.vsu.cs.msgr_file.dto.FileResponse;
import ru.vsu.cs.msgr_file.entity.FileEntity;
import ru.vsu.cs.msgr_file.entity.User;
import ru.vsu.cs.msgr_file.repository.FileRepository;
import ru.vsu.cs.msgr_file.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageConfig config;

    /**
     * Загрузка файла
     */
    public FileResponse uploadFile(Long userId, MultipartFile file) {
        // Проверка, что файл не пустой
        if (file.isEmpty()) {
            throw new RuntimeException("Файл пустой");
        }

        // Проверка размера
        if (file.getSize() > config.getMaxSize()) {
            throw new RuntimeException("Размер файла превышает максимально допустимый (512 МБ)");
        }

        // Проверка MIME-типа
        String mimeType = file.getContentType();
        if (mimeType == null || !config.getAllowedTypes().contains(mimeType)) {
            throw new RuntimeException("Недопустимый тип файла. Разрешены только: " + config.getAllowedTypes());
        }

        // Генерация уникального имени файла
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;

        // Путь для сохранения
        Path filePath = Paths.get(config.getUploadDir(), storedName);

        try {
            // Сохранение файла на диск
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении файла", e);
        }

        // Получение пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Сохранение информации о файле в БД
        FileEntity fileEntity = FileEntity.builder()
                .originalName(originalName)
                .storedName(storedName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .mimeType(mimeType)
                .uploadedBy(user)
                .build();

        fileRepository.save(fileEntity);

        return mapToFileResponse(fileEntity);
    }

    /**
     * Получение информации о файле
     */
    public FileResponse getFileInfo(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
        return mapToFileResponse(fileEntity);
    }

    /**
     * Получение пути к файлу для скачивания
     */
    public Path getFilePath(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        Path path = Paths.get(fileEntity.getFilePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("Файл не найден на диске");
        }

        return path;
    }

    /**
     * Получение MIME-типа файла
     */
    public String getFileMimeType(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
        return fileEntity.getMimeType();
    }

    /**
     * Получение оригинального имени файла
     */
    public String getFileOriginalName(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
        return fileEntity.getOriginalName();
    }

    private FileResponse mapToFileResponse(FileEntity fileEntity) {
        return FileResponse.builder()
                .id(fileEntity.getId())
                .originalName(fileEntity.getOriginalName())
                .mimeType(fileEntity.getMimeType())
                .size(fileEntity.getFileSize())
                .url("/api/files/" + fileEntity.getId())
                .uploadedById(fileEntity.getUploadedBy() != null ? fileEntity.getUploadedBy().getId() : null)
                .createdAt(fileEntity.getCreatedAt())
                .build();
    }
}