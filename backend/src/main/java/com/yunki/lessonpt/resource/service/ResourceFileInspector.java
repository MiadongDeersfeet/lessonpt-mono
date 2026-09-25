package com.yunki.lessonpt.resource.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.resource.domain.ResourceType;

public final class ResourceFileInspector {

    public static final long PDF_LIMIT = 20L * 1024 * 1024;
    public static final long AUDIO_LIMIT = 50L * 1024 * 1024;

    private ResourceFileInspector() {
    }

    public static InspectedFile inspect(MultipartFile file, ResourceType type) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        }
        long limit = type == ResourceType.SHEET ? PDF_LIMIT : AUDIO_LIMIT;
        if (file.getSize() > limit) {
            throw new BusinessException(ErrorCode.RESOURCE_FILE_TOO_LARGE);
        }
        String fileName = displayName(file.getOriginalFilename());
        String extension = extension(fileName);
        String contentType = normalizeType(file.getContentType());
        byte[] head = readHead(file);
        String storedType = match(type, extension, contentType, head);
        String objectExtension = storedType.equals("audio/mpeg") ? "mp3" : type == ResourceType.SHEET ? "pdf" : "m4a";
        return new InspectedFile(fileName, storedType, file.getSize(), objectExtension, head, file);
    }

    private static String match(ResourceType type, String extension, String contentType, byte[] head) {
        if (type == ResourceType.SHEET) {
            if (!"pdf".equals(extension) || !"application/pdf".equals(contentType) || !isPdf(head)) {
                throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
            }
            return "application/pdf";
        }
        if ("mp3".equals(extension) && "audio/mpeg".equals(contentType) && isMp3(head)) {
            return "audio/mpeg";
        }
        if ("m4a".equals(extension)
                && ("audio/mp4".equals(contentType) || "audio/x-m4a".equals(contentType))
                && isFtyp(head)) {
            return contentType;
        }
        throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
    }

    public static boolean isPdf(byte[] head) {
        return head.length >= 5
                && head[0] == '%'
                && head[1] == 'P'
                && head[2] == 'D'
                && head[3] == 'F'
                && head[4] == '-';
    }

    public static boolean isMp3(byte[] head) {
        if (head.length >= 3 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
            return true;
        }
        return head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0;
    }

    public static boolean isFtyp(byte[] head) {
        return head.length >= 8
                && head[4] == 'f'
                && head[5] == 't'
                && head[6] == 'y'
                && head[7] == 'p';
    }

    public static String displayName(String original) {
        if (original == null || original.isBlank()) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.indexOf('\0') >= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        }
        if (name.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 255) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        }
        return name;
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String value = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(16);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_INVALID_FILE);
        }
    }

    public record InspectedFile(
            String fileName,
            String contentType,
            long size,
            String objectExtension,
            byte[] head,
            MultipartFile file) {

        public InputStream openStream() throws IOException {
            return file.getInputStream();
        }
    }
}
