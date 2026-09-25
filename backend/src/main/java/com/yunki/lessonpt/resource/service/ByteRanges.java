package com.yunki.lessonpt.resource.service;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;

public final class ByteRanges {

    private ByteRanges() {
    }

    public static Resolved resolve(String header, long size) {
        if (header == null || header.isBlank()) {
            return new Resolved(null, 0, size - 1, size, false);
        }
        String value = header.trim();
        if (!value.startsWith("bytes=") || size < 0) {
            throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        }
        String spec = value.substring("bytes=".length());
        if (spec.contains(",")) {
            throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        }
        int dash = spec.indexOf('-');
        if (dash < 0) {
            throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        }
        try {
            long start;
            long end;
            if (dash == 0) {
                long suffix = Long.parseLong(spec.substring(1));
                if (suffix <= 0) {
                    throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
                }
                start = Math.max(0, size - suffix);
                end = size - 1;
            } else {
                start = Long.parseLong(spec.substring(0, dash));
                String endText = spec.substring(dash + 1);
                end = endText.isEmpty() ? size - 1 : Long.parseLong(endText);
            }
            if (start < 0 || start >= size || end < start) {
                throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
            }
            if (end >= size) {
                end = size - 1;
            }
            String http = "bytes=" + start + "-" + end;
            return new Resolved(http, start, end, end - start + 1, true);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.RESOURCE_RANGE_NOT_SATISFIABLE);
        }
    }

    public record Resolved(String httpRange, long start, long end, long length, boolean partial) {
    }
}
