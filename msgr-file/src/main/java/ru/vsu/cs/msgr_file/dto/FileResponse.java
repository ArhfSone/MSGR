package ru.vsu.cs.msgr_file.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private Long id;
    private String originalName;
    private String mimeType;
    private Long size;
    private String url;
    private Long uploadedById;
    private LocalDateTime createdAt;
}