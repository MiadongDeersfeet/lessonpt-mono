package com.yunki.lessonpt.relationship.service;

import java.sql.SQLException;
import java.util.UUID;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentAccess;
import com.yunki.lessonpt.relationship.dto.TeacherStudentAccessResponse;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentAccessMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherStudentAccessService {

    private final TeacherStudentMapper teacherStudentMapper;
    private final TeacherStudentAccessMapper teacherStudentAccessMapper;
    private final StudentMapper studentMapper;

    /**
     * 활성 TeacherStudent에 접근권한을 연다.
     * 행이 없으면 하나 만들고, 폐기된 행은 같은 ID에 새 공개 키로 다시 연다.
     * 이미 활성 행이 있으면 다시 만들지 않는다.
     * 이메일이 없는 Student는 열지 않는다.
     */
    @Transactional
    public TeacherStudentAccessResponse createAccess(Long teacherId, Long studentId) {
        TeacherStudent relation = activeRelation(teacherId, studentId);
        TeacherStudent locked = lockRelation(relation.getTeacherStudentId());
        if (!teacherId.equals(locked.getTeacherId())
                || !studentId.equals(locked.getStudentId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        requireEmail(studentId);
        TeacherStudentAccess existing = teacherStudentAccessMapper.selectByTeacherStudentId(locked.getTeacherStudentId());
        if (existing == null) {
            insertAccess(locked.getTeacherStudentId());
        } else if (existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        } else {
            reactivateAccess(existing);
        }
        return toResponse(requireActive(locked.getTeacherStudentId()));
    }

    @Transactional(readOnly = true)
    public TeacherStudentAccessResponse getAccess(Long teacherId, Long studentId) {
        TeacherStudent relation = activeRelation(teacherId, studentId);
        return toResponse(requireActive(relation.getTeacherStudentId()));
    }

    /**
     * 접근권한을 폐기한다. 행은 지우지 않는다.
     * TODO: StudentAccessSession이 생기면 이 폐기에서 조회 세션도 무효화한다.
     */
    @Transactional
    public void revokeAccess(Long teacherId, Long studentId) {
        TeacherStudent relation = activeRelation(teacherId, studentId);
        lockRelation(relation.getTeacherStudentId());
        if (teacherStudentAccessMapper.softDeleteActiveByTeacherStudentId(relation.getTeacherStudentId()) != 1) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private TeacherStudent activeRelation(Long teacherId, Long studentId) {
        TeacherStudent relation = teacherStudentMapper.selectActiveByTeacherIdAndStudentId(teacherId, studentId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return relation;
    }

    private void requireEmail(Long studentId) {
        Student student = studentMapper.selectActiveStudentById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        String email = student.getEmail();
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
    }

    private TeacherStudentAccess requireActive(Long teacherStudentId) {
        TeacherStudentAccess access = teacherStudentAccessMapper.selectActiveByTeacherStudentId(teacherStudentId);
        if (access == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return access;
    }

    private TeacherStudent lockRelation(Long teacherStudentId) {
        try {
            TeacherStudent locked = teacherStudentMapper.lockTeacherStudentById(teacherStudentId);
            if (locked == null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
            return locked;
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

    private void insertAccess(Long teacherStudentId) {
        TeacherStudentAccess created = new TeacherStudentAccess();
        created.setTeacherStudentId(teacherStudentId);
        created.setPublicAccessKey(newPublicAccessKey());
        created.setStatus(RecordStatus.ACTIVE);
        try {
            expectOne(teacherStudentAccessMapper.insertTeacherStudentAccess(created));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
    }

    private void reactivateAccess(TeacherStudentAccess existing) {
        existing.setPublicAccessKey(newPublicAccessKey());
        try {
            expectOne(teacherStudentAccessMapper.reactivateTeacherStudentAccess(existing));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
    }

    private void expectOne(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
    }

    private String newPublicAccessKey() {
        return UUID.randomUUID().toString();
    }

    private TeacherStudentAccessResponse toResponse(TeacherStudentAccess access) {
        return new TeacherStudentAccessResponse(
                access.getTeacherStudentAccessId(),
                access.getTeacherStudentId(),
                access.getPublicAccessKey(),
                access.getCreatedAt(),
                access.getStatus());
    }
}
