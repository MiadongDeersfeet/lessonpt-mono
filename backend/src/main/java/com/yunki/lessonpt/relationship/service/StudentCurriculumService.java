package com.yunki.lessonpt.relationship.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentCurriculumService {

    private final StudentCurriculumMapper studentCurriculumMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final CurriculumMapper curriculumMapper;

    @Transactional
    public StudentCurriculum assignStudentCurriculum(
            Long teacherId, Long teacherStudentLocationId, Long curriculumId, String memo) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        requireOwnedCurriculum(teacherId, curriculumId);
        lockOwnedLocation(teacherId, teacherStudentLocationId);
        lockOwnedCurriculum(teacherId, curriculumId);

        StudentCurriculum existing = studentCurriculumMapper.selectByTeacherStudentLocationIdAndCurriculumId(
                teacherStudentLocationId, curriculumId);
        if (existing != null && existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        if (existing != null) {
            expectOne(studentCurriculumMapper.restoreStudentCurriculum(existing.getStudentCurriculumId()));
            return requireActive(teacherStudentLocationId, existing.getStudentCurriculumId());
        }

        StudentCurriculum created = new StudentCurriculum();
        created.setTeacherStudentLocationId(teacherStudentLocationId);
        created.setCurriculumId(curriculumId);
        created.setReenrolled(false);
        created.setMemo(memo);
        created.setStatus(RecordStatus.ACTIVE);
        expectOne(studentCurriculumMapper.insertStudentCurriculum(created));
        return requireActive(teacherStudentLocationId, created.getStudentCurriculumId());
    }

    @Transactional(readOnly = true)
    public List<StudentCurriculum> getStudentCurriculums(Long teacherId, Long teacherStudentLocationId) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        return studentCurriculumMapper.selectActiveStudentCurriculumsByTeacherStudentLocationId(teacherStudentLocationId);
    }

    @Transactional(readOnly = true)
    public StudentCurriculum getStudentCurriculum(
            Long teacherId, Long teacherStudentLocationId, Long studentCurriculumId) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        return requireActive(teacherStudentLocationId, studentCurriculumId);
    }

    @Transactional
    public StudentCurriculum updateStudentCurriculum(
            Long teacherId,
            Long teacherStudentLocationId,
            Long studentCurriculumId,
            StudentCurriculumChange change) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        StudentCurriculum studentCurriculum = requireActive(teacherStudentLocationId, studentCurriculumId);
        StudentCurriculum locked = lockStudentCurriculum(studentCurriculumId);
        if (!teacherStudentLocationId.equals(locked.getTeacherStudentLocationId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (change.isMemoSpecified()) {
            studentCurriculum.setMemo(change.getMemo());
        }
        expectOne(studentCurriculumMapper.updateStudentCurriculum(studentCurriculum));
        return requireActive(teacherStudentLocationId, studentCurriculumId);
    }

    /**
     * 수강 배정만 비활성화한다.
     * 학습 이력은 지우지 않는다.
     */
    @Transactional
    public void deleteStudentCurriculum(Long teacherId, Long teacherStudentLocationId, Long studentCurriculumId) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        requireActive(teacherStudentLocationId, studentCurriculumId);
        StudentCurriculum locked = lockStudentCurriculum(studentCurriculumId);
        if (!teacherStudentLocationId.equals(locked.getTeacherStudentLocationId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        expectOne(studentCurriculumMapper.softDeleteStudentCurriculum(studentCurriculumId, teacherStudentLocationId));
    }

    @Transactional
    public StudentCurriculum restoreStudentCurriculum(
            Long teacherId, Long teacherStudentLocationId, Long studentCurriculumId) {
        requireOwnedLocation(teacherId, teacherStudentLocationId);
        StudentCurriculum existing = studentCurriculumMapper.selectStudentCurriculumById(studentCurriculumId);
        if (existing == null || !teacherStudentLocationId.equals(existing.getTeacherStudentLocationId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        requireOwnedCurriculum(teacherId, existing.getCurriculumId());
        lockOwnedLocation(teacherId, teacherStudentLocationId);
        lockOwnedCurriculum(teacherId, existing.getCurriculumId());
        StudentCurriculum current = studentCurriculumMapper.selectStudentCurriculumById(studentCurriculumId);
        if (current == null || !teacherStudentLocationId.equals(current.getTeacherStudentLocationId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (current.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        expectOne(studentCurriculumMapper.restoreStudentCurriculum(studentCurriculumId));
        return requireActive(teacherStudentLocationId, studentCurriculumId);
    }

    private TeacherStudentLocation requireOwnedLocation(Long teacherId, Long teacherStudentLocationId) {
        TeacherStudentLocation location = teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(
                teacherStudentLocationId);
        if (location == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        requireActiveRelation(teacherId, location.getTeacherStudentId());
        return location;
    }

    private TeacherStudent requireActiveRelation(Long teacherId, Long teacherStudentId) {
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(teacherStudentId);
        if (relation == null
                || !teacherId.equals(relation.getTeacherId())
                || relation.getStatus() != RecordStatus.ACTIVE
                || relation.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return relation;
    }

    private Curriculum requireOwnedCurriculum(Long teacherId, Long curriculumId) {
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return curriculum;
    }

    private TeacherStudentLocation lockOwnedLocation(Long teacherId, Long teacherStudentLocationId) {
        TeacherStudentLocation locked;
        try {
            locked = teacherStudentLocationMapper.lockTeacherStudentLocationById(teacherStudentLocationId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (locked == null
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        requireActiveRelation(teacherId, locked.getTeacherStudentId());
        return locked;
    }

    private Curriculum lockOwnedCurriculum(Long teacherId, Long curriculumId) {
        Curriculum locked;
        try {
            locked = curriculumMapper.lockCurriculumById(curriculumId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (locked == null
                || !teacherId.equals(locked.getTeacherId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return locked;
    }

    private StudentCurriculum requireActive(Long teacherStudentLocationId, Long studentCurriculumId) {
        StudentCurriculum studentCurriculum = studentCurriculumMapper.selectActiveStudentCurriculumById(studentCurriculumId);
        if (studentCurriculum == null || !teacherStudentLocationId.equals(studentCurriculum.getTeacherStudentLocationId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return studentCurriculum;
    }

    private StudentCurriculum lockStudentCurriculum(Long studentCurriculumId) {
        try {
            StudentCurriculum locked = studentCurriculumMapper.lockStudentCurriculumById(studentCurriculumId);
            if (locked == null) {
                throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
            }
            return locked;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
    }

    private void expectOne(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
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
}
