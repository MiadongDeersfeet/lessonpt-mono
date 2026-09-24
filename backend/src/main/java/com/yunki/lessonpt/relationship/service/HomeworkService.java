package com.yunki.lessonpt.relationship.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.relationship.domain.Homework;
import com.yunki.lessonpt.relationship.domain.StudentCurriculum;
import com.yunki.lessonpt.relationship.domain.StudentMonitoring;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.mapper.HomeworkMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.StudentMonitoringMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final StudentMonitoringMapper studentMonitoringMapper;
    private final StudentCurriculumMapper studentCurriculumMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final TeacherStudentMapper teacherStudentMapper;

    @Transactional
    public Homework createHomework(
            Long teacherId,
            Long monitoringId,
            String homeworkContent,
            LocalDateTime deadline,
            String feedback) {
        requireOwnedMonitoring(teacherId, monitoringId);
        requireContent(homeworkContent);

        Homework created = new Homework();
        created.setMonitoringId(monitoringId);
        created.setHomeworkContent(homeworkContent);
        created.setDeadline(deadline);
        created.setCompleted(false);
        created.setFeedback(feedback);
        created.setStatus(RecordStatus.ACTIVE);
        expectOne(homeworkMapper.insertHomework(created));
        return requireActiveHomework(monitoringId, created.getHomeworkId());
    }

    @Transactional(readOnly = true)
    public List<Homework> getHomeworks(Long teacherId, Long monitoringId) {
        requireOwnedMonitoring(teacherId, monitoringId);
        return homeworkMapper.selectActiveHomeworksByMonitoringId(monitoringId);
    }

    @Transactional(readOnly = true)
    public Homework getHomework(Long teacherId, Long monitoringId, Long homeworkId) {
        requireOwnedMonitoring(teacherId, monitoringId);
        return requireActiveHomework(monitoringId, homeworkId);
    }

    @Transactional
    public Homework updateHomework(Long teacherId, Long monitoringId, Long homeworkId, HomeworkChange change) {
        requireOwnedMonitoring(teacherId, monitoringId);
        Homework homework = requireActiveHomework(monitoringId, homeworkId);
        Homework locked = lockHomework(homeworkId);
        if (!monitoringId.equals(locked.getMonitoringId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        applyChange(homework, change);
        expectOne(homeworkMapper.updateHomework(homework));
        return requireActiveHomework(monitoringId, homeworkId);
    }

    @Transactional
    public void deleteHomework(Long teacherId, Long monitoringId, Long homeworkId) {
        requireOwnedMonitoring(teacherId, monitoringId);
        requireActiveHomework(monitoringId, homeworkId);
        Homework locked = lockHomework(homeworkId);
        if (!monitoringId.equals(locked.getMonitoringId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        expectOne(homeworkMapper.softDeleteHomework(homeworkId, monitoringId));
    }

    @Transactional
    public Homework restoreHomework(Long teacherId, Long monitoringId, Long homeworkId) {
        requireOwnedMonitoring(teacherId, monitoringId);
        Homework existing = homeworkMapper.selectHomeworkById(homeworkId);
        if (existing == null || !monitoringId.equals(existing.getMonitoringId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        Homework locked = lockHomework(homeworkId);
        if (!monitoringId.equals(locked.getMonitoringId()) || locked.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(locked.getStatus() == RecordStatus.ACTIVE
                    ? ErrorCode.COMMON_CONFLICT
                    : ErrorCode.COMMON_NOT_FOUND);
        }
        expectOne(homeworkMapper.restoreHomework(existing));
        return requireActiveHomework(monitoringId, homeworkId);
    }

    private void applyChange(Homework homework, HomeworkChange change) {
        if (change.isHomeworkContentSpecified()) {
            requireContent(change.getHomeworkContent());
            homework.setHomeworkContent(change.getHomeworkContent());
        }
        if (change.isDeadlineSpecified()) {
            homework.setDeadline(change.getDeadline());
        }
        if (change.isCompletedSpecified()) {
            if (change.getCompleted() == null) {
                throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
            }
            homework.setCompleted(change.getCompleted());
        }
        if (change.isFeedbackSpecified()) {
            homework.setFeedback(change.getFeedback());
        }
    }

    private StudentMonitoring requireOwnedMonitoring(Long teacherId, Long monitoringId) {
        StudentMonitoring monitoring = studentMonitoringMapper.selectActiveStudentMonitoringById(monitoringId);
        if (monitoring == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        StudentCurriculum enrollment = studentCurriculumMapper.selectActiveStudentCurriculumById(
                monitoring.getStudentCurriculumId());
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
        return monitoring;
    }

    private Homework requireActiveHomework(Long monitoringId, Long homeworkId) {
        Homework homework = homeworkMapper.selectActiveHomeworkByIdAndMonitoringId(homeworkId, monitoringId);
        if (homework == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return homework;
    }

    private Homework lockHomework(Long homeworkId) {
        try {
            Homework locked = homeworkMapper.lockHomeworkById(homeworkId);
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

    private void requireContent(String homeworkContent) {
        if (homeworkContent == null || homeworkContent.isBlank()) {
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
