package com.yunki.lessonpt.relationship.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherStudentLocationService {

    private final TeacherStudentMapper teacherStudentMapper;
    private final LocationMapper locationMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final StudentCurriculumMapper studentCurriculumMapper;

    @Transactional
    public TeacherStudentLocationView assignLocation(Long teacherId, Long studentId, Long locationId) {
        OwnedRows owned = lockOwnedRows(teacherId, studentId, locationId);
        TeacherStudentLocation existing = teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(
                owned.teacherStudent().getTeacherStudentId(), locationId);
        if (existing != null && existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        if (existing != null) {
            expectOne(teacherStudentLocationMapper.restoreTeacherStudentLocation(existing.getTeacherStudentLocationId()));
            return requireActiveLink(existing.getTeacherStudentLocationId());
        }
        TeacherStudentLocation created = new TeacherStudentLocation();
        created.setTeacherStudentId(owned.teacherStudent().getTeacherStudentId());
        created.setLocationId(locationId);
        created.setStatus(RecordStatus.ACTIVE);
        expectOne(teacherStudentLocationMapper.insertTeacherStudentLocation(created));
        return requireActiveLink(created.getTeacherStudentLocationId());
    }

    @Transactional(readOnly = true)
    public List<TeacherStudentLocationView> getStudentLocations(Long teacherId, Long studentId) {
        TeacherStudent relation = activeRelation(teacherId, studentId);
        return teacherStudentLocationMapper.selectActiveViewsByTeacherStudentId(relation.getTeacherStudentId());
    }

    /**
     * 이 장소 연결의 활성 수강만 먼저 비활성화하고 연결을 해제한다.
     * Monitoring과 Homework는 학습 이력으로 남긴다.
     * 복구는 연결 행만 다시 활성화한다.
     */
    @Transactional
    public void releaseLocation(Long teacherId, Long studentId, Long locationId) {
        OwnedRows owned = lockOwnedRows(teacherId, studentId, locationId);
        TeacherStudentLocation existing = teacherStudentLocationMapper.selectActiveByTeacherStudentIdAndLocationId(
                owned.teacherStudent().getTeacherStudentId(), locationId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        TeacherStudentLocation locked = lockLink(existing.getTeacherStudentLocationId());
        if (!owned.teacherStudent().getTeacherStudentId().equals(locked.getTeacherStudentId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        studentCurriculumMapper.softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(
                existing.getTeacherStudentLocationId());
        expectOne(teacherStudentLocationMapper.softDeleteTeacherStudentLocation(existing.getTeacherStudentLocationId()));
    }

    @Transactional
    public TeacherStudentLocationView restoreLocation(Long teacherId, Long studentId, Long locationId) {
        OwnedRows owned = lockOwnedRows(teacherId, studentId, locationId);
        TeacherStudentLocation existing = teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(
                owned.teacherStudent().getTeacherStudentId(), locationId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (existing.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        expectOne(teacherStudentLocationMapper.restoreTeacherStudentLocation(existing.getTeacherStudentLocationId()));
        return requireActiveLink(existing.getTeacherStudentLocationId());
    }

    private OwnedRows lockOwnedRows(Long teacherId, Long studentId, Long locationId) {
        TeacherStudent relation = activeRelation(teacherId, studentId);
        TeacherStudent lockedRelation = lockTeacherStudent(relation.getTeacherStudentId());
        if (!teacherId.equals(lockedRelation.getTeacherId())
                || lockedRelation.getStatus() != RecordStatus.ACTIVE
                || lockedRelation.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Location lockedLocation = lockLocation(locationId);
        if (!teacherId.equals(lockedLocation.getTeacherId())
                || lockedLocation.getStatus() != RecordStatus.ACTIVE
                || lockedLocation.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return new OwnedRows(lockedRelation, lockedLocation);
    }

    private TeacherStudent activeRelation(Long teacherId, Long studentId) {
        TeacherStudent relation = teacherStudentMapper.selectActiveByTeacherIdAndStudentId(teacherId, studentId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return relation;
    }

    private TeacherStudent lockTeacherStudent(Long teacherStudentId) {
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

    private Location lockLocation(Long locationId) {
        try {
            Location locked = locationMapper.lockLocationById(locationId);
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

    private TeacherStudentLocation lockLink(Long teacherStudentLocationId) {
        try {
            TeacherStudentLocation locked = teacherStudentLocationMapper.lockTeacherStudentLocationById(
                    teacherStudentLocationId);
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

    private TeacherStudentLocationView requireActiveLink(Long teacherStudentLocationId) {
        TeacherStudentLocationView stored = teacherStudentLocationMapper.selectActiveViewById(teacherStudentLocationId);
        if (stored == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return stored;
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

    private record OwnedRows(TeacherStudent teacherStudent, Location location) {
    }
}
