package com.yunki.lessonpt.student.dto;

import java.util.List;

public record StudentCategoryView(String name, int totalContentCount, List<StudentContentView> contents) {
}
