package com.yunki.lessonpt.teacher.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.dto.TeacherMeResponse;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherMeService {

    private final TeacherMapper teacherMapper;

    @Transactional(readOnly = true)
    public TeacherMeResponse me(Long teacherId) {
        Teacher teacher = teacherMapper.selectActiveTeacherById(teacherId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return new TeacherMeResponse(
                teacher.getTeacherId(),
                teacher.getEmail(),
                teacher.getName(),
                teacher.getPhone());
    }
}
