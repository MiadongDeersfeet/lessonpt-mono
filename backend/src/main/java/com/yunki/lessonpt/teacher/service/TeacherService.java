package com.yunki.lessonpt.teacher.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 강사 계정의 생성과 상태 변경을 한 트랜잭션에서 처리한다.
 * 비밀번호는 해시로만 저장하고, 없는 계정과 비활성 계정을 호출자가 구분하게 한다.
 */
@Service
public class TeacherService {

    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherMapper teacherMapper, PasswordEncoder passwordEncoder) {
        this.teacherMapper = teacherMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Teacher createTeacher(String email, String rawPassword, String name, String phone) {
        if (teacherMapper.selectTeacherByEmail(email) != null) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        Teacher teacher = new Teacher();
        teacher.setEmail(email);
        teacher.setPasswordHash(passwordEncoder.encode(rawPassword));
        teacher.setName(name);
        teacher.setPhone(phone);
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        try {
            teacherMapper.insertTeacher(teacher);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        return teacherMapper.selectTeacherById(teacher.getTeacherId());
    }

    @Transactional(readOnly = true)
    public Teacher getTeacher(Long teacherId) {
        return requireActive(teacherId);
    }

    @Transactional
    public Teacher updateTeacher(Long teacherId, String name, String phone) {
        Teacher teacher = requireActive(teacherId);
        teacher.setName(name);
        teacher.setPhone(phone);
        teacherMapper.updateTeacher(teacher);
        return teacherMapper.selectActiveTeacherById(teacherId);
    }

    @Transactional
    public void deleteTeacher(Long teacherId) {
        requireActive(teacherId);
        teacherMapper.softDeleteTeacher(teacherId);
    }

    @Transactional
    public Teacher restoreTeacher(Long teacherId) {
        Teacher teacher = teacherMapper.selectTeacherById(teacherId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        teacherMapper.restoreTeacher(teacherId);
        return teacherMapper.selectTeacherById(teacherId);
    }

    private Teacher requireActive(Long teacherId) {
        Teacher teacher = teacherMapper.selectActiveTeacherById(teacherId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return teacher;
    }
}
