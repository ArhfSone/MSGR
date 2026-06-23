package ru.vsu.cs.msgr_file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.msgr_file.dto.FileResponse;
import ru.vsu.cs.msgr_file.service.FileService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * Загрузка файла
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        FileResponse response = fileService.uploadFile(userId, file);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о файле
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<FileResponse> getFileInfo(@PathVariable Long fileId) {
        FileResponse response = fileService.getFileInfo(fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Скачивание файла
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            Path filePath = fileService.getFilePath(fileId);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String mimeType = fileService.getFileMimeType(fileId);
            String originalName = fileService.getFileOriginalName(fileId);

            // Кодируем имя файла для корректной передачи в заголовке
            String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedName)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}