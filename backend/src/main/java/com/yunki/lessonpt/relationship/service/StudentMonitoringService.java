package com.yunki.lessonpt.relationship.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.ProgressStatus;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentMonitoringService {

    private static final int MIN_BPM = 60;
    private static final int MAX_BPM = 240;

    private final StudentMonitoringMapper studentMonitoringMapper;
    private final StudentCurriculumMapper studentCurriculumMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final ContentDetailMapper contentDetailMapper;
    private final CategoryMapper categoryMapper;
    private final CurriculumMapper curriculumMapper;

    @Transactional
    public StudentMonitoring assignStudentMonitoring(
            Long teacherId,
            Long studentCurriculumId,
            Long contentDetailId,
            Integer currentBpm,
            ProgressStatus progressStatus,
            String memo) {
        requireBpm(currentBpm);
        StudentCurriculum enrollment = requireOwnedEnrollment(teacherId, studentCurriculumId);
        requireAssignableContent(teacherId, enrollment.getCurriculumId(), contentDetailId);
        lockEnrollment(studentCurriculumId);

        StudentMonitoring existing = studentMonitoringMapper.selectByStudentCurriculumIdAndContentDetailId(
                studentCurriculumId, contentDetailId);
        if (existing != null && existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        int displayOrder = nextDisplayOrder(studentCurriculumId);
        if (existing != null) {
            existing.setDisplayOrder(displayOrder);
            expectOne(studentMonitoringMapper.restoreStudentMonitoring(existing));
            return requireActiveMonitoring(studentCurriculumId, existing.getMonitoringId());
        }

        StudentMonitoring created = new StudentMonitoring();
        created.setStudentCurriculumId(studentCurriculumId);
        created.setContentDetailId(contentDetailId);
        created.setDisplayOrder(displayOrder);
        created.setCurrentBpm(currentBpm);
        created.setProgressStatus(progressStatus == null ? ProgressStatus.YET : progressStatus);
        created.setMemo(memo);
        created.setStatus(RecordStatus.ACTIVE);
        expectOne(studentMonitoringMapper.insertStudentMonitoring(created));
        return requireActiveMonitoring(studentCurriculumId, created.getMonitoringId());
    }

    @Transactional(readOnly = true)
    public List<StudentMonitoring> getStudentMonitorings(Long teacherId, Long studentCurriculumId) {
        requireOwnedEnrollment(teacherId, studentCurriculumId);
        return studentMonitoringMapper.selectActiveStudentMonitoringsByStudentCurriculumId(studentCurriculumId);
    }

    @Transactional(readOnly = true)
    public StudentMonitoring getStudentMonitoring(Long teacherId, Long studentCurriculumId, Long monitoringId) {
        requireOwnedEnrollment(teacherId, studentCurriculumId);
        return requireActiveMonitoring(studentCurriculumId, monitoringId);
    }

    @Transactional
    public StudentMonitoring updateStudentMonitoring(
            Long teacherId,
            Long studentCurriculumId,
            Long monitoringId,
            StudentMonitoringChange change) {
        requireOwnedEnrollment(teacherId, studentCurriculumId);
        StudentMonitoring monitoring = requireActiveMonitoring(studentCurriculumId, monitoringId);
        StudentMonitoring locked = lockMonitoring(monitoringId);
        if (!studentCurriculumId.equals(locked.getStudentCurriculumId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        applyChange(monitoring, change);
        expectOne(studentMonitoringMapper.updateStudentMonitoring(monitoring));
        return requireActiveMonitoring(studentCurriculumId, monitoringId);
    }

    /**
     * 모니터링 행만 비활성화하고, 같은 수강 안의 뒤 순서를 당긴다.
     * TODO: Homework가 구현되면 활성 과제도 같은 트랜잭션에서 soft delete한다. 복구 때 과제는 자동 복구하지 않는다.
     */
    @Transactional
    public void deleteStudentMonitoring(Long teacherId, Long studentCurriculumId, Long monitoringId) {
        requireOwnedEnrollment(teacherId, studentCurriculumId);
        lockEnrollment(studentCurriculumId);
        StudentMonitoring monitoring = requireActiveMonitoring(studentCurriculumId, monitoringId);
        expectOne(studentMonitoringMapper.softDeleteStudentMonitoring(monitoringId, studentCurriculumId));
        studentMonitoringMapper.shiftActiveDisplayOrdersDown(studentCurriculumId, monitoring.getDisplayOrder());
    }

    @Transactional
    public StudentMonitoring restoreStudentMonitoring(Long teacherId, Long studentCurriculumId, Long monitoringId) {
        StudentCurriculum enrollment = requireOwnedEnrollment(teacherId, studentCurriculumId);
        lockEnrollment(studentCurriculumId);
        StudentMonitoring existing = studentMonitoringMapper.selectStudentMonitoringById(monitoringId);
        if (existing == null || !studentCurriculumId.equals(existing.getStudentCurriculumId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        requireAssignableContent(teacherId, enrollment.getCurriculumId(), existing.getContentDetailId());
        existing.setDisplayOrder(nextDisplayOrder(studentCurriculumId));
        expectOne(studentMonitoringMapper.restoreStudentMonitoring(existing));
        return requireActiveMonitoring(studentCurriculumId, monitoringId);
    }

    private void applyChange(StudentMonitoring monitoring, StudentMonitoringChange change) {
        if (change.isCurrentBpmSpecified()) {
            requireBpm(change.getCurrentBpm());
            monitoring.setCurrentBpm(change.getCurrentBpm());
        }
        if (change.isProgressStatusSpecified()) {
            if (change.getProgressStatus() == null) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
            }
            monitoring.setProgressStatus(change.getProgressStatus());
        }
        if (change.isMemoSpecified()) {
            monitoring.setMemo(change.getMemo());
        }
    }

    private StudentCurriculum requireOwnedEnrollment(Long teacherId, Long studentCurriculumId) {
        StudentCurriculum enrollment = studentCurriculumMapper.selectActiveStudentCurriculumById(studentCurriculumId);
        if (enrollment == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudentLocation location = teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(
                enrollment.getTeacherStudentLocationId());
        if (location == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudent relation = teacherStudentMapper.selectTeacherStudentById(location.getTeacherStudentId());
        if (relation == null
                || !teacherId.equals(relation.getTeacherId())
                || relation.getStatus() != RecordStatus.ACTIVE
                || relation.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return enrollment;
    }

    private void requireAssignableContent(Long teacherId, Long curriculumId, Long contentDetailId) {
        ContentDetail contentDetail = contentDetailMapper.selectActiveContentDetailById(contentDetailId);
        if (contentDetail == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Category category = categoryMapper.selectActiveCategoryById(contentDetail.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(
                category.getCurriculumId(), teacherId);
        if (curriculum == null || !curriculumId.equals(curriculum.getCurriculumId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private StudentCurriculum lockEnrollment(Long studentCurriculumId) {
        try {
            StudentCurriculum locked = studentCurriculumMapper.lockStudentCurriculumById(studentCurriculumId);
            if (locked == null
                    || locked.getStatus() != RecordStatus.ACTIVE
                    || locked.getDeletedAt() != null) {
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

    private StudentMonitoring lockMonitoring(Long monitoringId) {
        try {
            StudentMonitoring locked = studentMonitoringMapper.lockStudentMonitoringById(monitoringId);
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

    private StudentMonitoring requireActiveMonitoring(Long studentCurriculumId, Long monitoringId) {
        StudentMonitoring monitoring = studentMonitoringMapper.selectActiveStudentMonitoringById(monitoringId);
        if (monitoring == null || !studentCurriculumId.equals(monitoring.getStudentCurriculumId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return monitoring;
    }

    private int nextDisplayOrder(Long studentCurriculumId) {
        Integer maxOrder = studentMonitoringMapper.selectMaxDisplayOrderByStudentCurriculumId(studentCurriculumId);
        return maxOrder == null ? 1 : maxOrder + 1;
    }

    private void requireBpm(Integer currentBpm) {
        if (currentBpm == null) {
            return;
        }
        if (currentBpm < MIN_BPM || currentBpm > MAX_BPM) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
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
