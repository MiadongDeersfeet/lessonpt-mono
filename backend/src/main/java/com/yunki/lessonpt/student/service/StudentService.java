package com.yunki.lessonpt.student.service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.ActiveTeacherStudent;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.dto.StudentCreateRequest;
import com.yunki.lessonpt.student.dto.StudentResponse;
import com.yunki.lessonpt.student.dto.StudentUpdateRequest;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final StudentCurriculumMapper studentCurriculumMapper;
    private final TeacherStudentAccessMapper teacherStudentAccessMapper;
    private final TeacherMapper teacherMapper;

    @Transactional
    public StudentResponse createStudent(Long teacherId, StudentCreateRequest request) {
        if (teacherMapper.selectActiveTeacherById(teacherId) == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        String email = normalizeEmail(request.email());
        Student student = email == null ? null : studentMapper.selectStudentByEmail(email);
        if (student != null && student.getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        if (student == null) {
            student = new Student();
            student.setEmail(email);
            student.setName(request.name());
            student.setPhone(request.phone());
            student.setStatus(RecordStatus.ACTIVE);
            studentMapper.insertStudent(student);
        }
        TeacherStudent relation = teacherStudentMapper.selectByTeacherIdAndStudentId(teacherId, student.getStudentId());
        if (relation == null) {
            relation = new TeacherStudent();
            relation.setTeacherId(teacherId);
            relation.setStudentId(student.getStudentId());
            relation.setStatus(RecordStatus.ACTIVE);
            teacherStudentMapper.insertTeacherStudent(relation);
        } else if (relation.getStatus() == RecordStatus.INACTIVE) {
            lockRelation(relation.getTeacherStudentId());
            teacherStudentMapper.restoreTeacherStudent(teacherId, student.getStudentId());
        } else {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        return requireLinked(teacherId, student.getStudentId());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudents(Long teacherId) {
        return teacherStudentMapper.selectActiveStudentsByTeacherId(teacherId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long teacherId, Long studentId) {
        return requireLinked(teacherId, studentId);
    }

    /**
     * Student 프로필은 강사별로 나뉘지 않는다.
     * 이 수정은 같은 Student와 연결된 다른 Teacher의 화면에도 반영된다.
     */
    @Transactional
    public StudentResponse updateStudent(Long teacherId, Long studentId, StudentUpdateRequest request) {
        requireLinked(teacherId, studentId);
        Student student = studentMapper.selectActiveStudentById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (request.isNameSpecified()) {
            student.setName(request.getName());
        }
        if (request.isPhoneSpecified()) {
            student.setPhone(request.getPhone());
        }
        if (request.isEmailSpecified()) {
            String email = normalizeEmail(request.getEmail());
            if (email != null) {
                Student existing = studentMapper.selectStudentByEmail(email);
                if (existing != null && !existing.getStudentId().equals(studentId)) {
                    throw new BusinessException(ErrorCode.COMMON_CONFLICT);
                }
            }
            student.setEmail(email);
        }
        studentMapper.updateStudent(student);
        return requireLinked(teacherId, studentId);
    }

    /**
     * 관계와 접근권한, 장소 연결, 수강을 비활성화한다.
     * Student, Monitoring, Homework는 유지한다.
     * 복구는 이 관계 행만 다시 활성화한다.
     * TODO: StudentAccessSession이 생기면 이 해제에서 조회 세션도 폐기한다.
     */
    @Transactional
    public void releaseStudent(Long teacherId, Long studentId) {
        TeacherStudent relation = teacherStudentMapper.selectActiveByTeacherIdAndStudentId(teacherId, studentId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        lockRelation(relation.getTeacherStudentId());
        teacherStudentAccessMapper.softDeleteActiveByTeacherStudentId(relation.getTeacherStudentId());
        List<TeacherStudentLocation> links = teacherStudentLocationMapper
                .selectActiveByTeacherStudentId(relation.getTeacherStudentId())
                .stream()
                .sorted(Comparator.comparing(TeacherStudentLocation::getTeacherStudentLocationId))
                .toList();
        for (TeacherStudentLocation link : links) {
            lockLink(link.getTeacherStudentLocationId());
            studentCurriculumMapper.softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(
                    link.getTeacherStudentLocationId());
        }
        teacherStudentLocationMapper.softDeleteActiveByTeacherStudentId(relation.getTeacherStudentId());
        teacherStudentMapper.softDeleteTeacherStudent(teacherId, studentId);
    }

    @Transactional
    public StudentResponse restoreStudent(Long teacherId, Long studentId) {
        TeacherStudent relation = teacherStudentMapper.selectByTeacherIdAndStudentId(teacherId, studentId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (relation.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        Student student = studentMapper.selectStudentById(studentId);
        if (student == null || student.getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        lockRelation(relation.getTeacherStudentId());
        teacherStudentMapper.restoreTeacherStudent(teacherId, studentId);
        return requireLinked(teacherId, studentId);
    }

    private StudentResponse requireLinked(Long teacherId, Long studentId) {
        ActiveTeacherStudent linked = teacherStudentMapper.selectActiveStudentForTeacher(teacherId, studentId);
        if (linked == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return toResponse(linked);
    }

    private void lockLink(Long teacherStudentLocationId) {
        try {
            TeacherStudentLocation locked = teacherStudentLocationMapper.lockTeacherStudentLocationById(
                    teacherStudentLocationId);
            if (locked == null
                    || locked.getStatus() != RecordStatus.ACTIVE
                    || locked.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT);
            }
            throw exception;
        }
    }

    private void lockRelation(Long teacherStudentId) {
        try {
            if (teacherStudentMapper.lockTeacherStudentById(teacherStudentId) == null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.COMMON_CONFLICT);
            }
            throw exception;
        }
    }

    private boolean isLockTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotAcquireLockException) {
                return true;
            }
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 30006) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("ORA-30006")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private StudentResponse toResponse(ActiveTeacherStudent linked) {
        return new StudentResponse(
                linked.getStudentId(),
                linked.getEmail(),
                linked.getName(),
                linked.getPhone(),
                linked.getTeacherStudentId());
    }
}
