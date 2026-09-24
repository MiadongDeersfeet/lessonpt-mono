package com.yunki.lessonpt.curriculum.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.dto.CurriculumUpdateRequest;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumMapper curriculumMapper;
    private final TeacherMapper teacherMapper;

    @Transactional
    public Curriculum createCurriculum(Long teacherId, String name) {
        requireName(name);
        lockActiveTeacher(teacherId);
        Integer maxOrder = curriculumMapper.selectMaxDisplayOrderByTeacherId(teacherId);
        int nextOrder = maxOrder == null ? 1 : maxOrder + 1;

        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacherId);
        curriculum.setName(name);
        curriculum.setDisplayOrder(nextOrder);
        curriculum.setStatus(RecordStatus.ACTIVE);
        expectOne(curriculumMapper.insertCurriculum(curriculum));
        return requireActive(teacherId, curriculum.getCurriculumId());
    }

    @Transactional(readOnly = true)
    public List<Curriculum> getCurriculums(Long teacherId) {
        return curriculumMapper.selectActiveCurriculumsByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public Curriculum getCurriculum(Long teacherId, Long curriculumId) {
        return requireActive(teacherId, curriculumId);
    }

    @Transactional
    public Curriculum updateCurriculum(Long teacherId, Long curriculumId, CurriculumUpdateRequest request) {
        Curriculum curriculum = requireActive(teacherId, curriculumId);
        Curriculum locked = lockCurriculum(curriculumId);
        if (!teacherId.equals(locked.getTeacherId())
                || locked.getStatus() != RecordStatus.ACTIVE
                || locked.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (request.isNameSpecified()) {
            requireName(request.getName());
            curriculum.setName(request.getName());
        }
        expectOne(curriculumMapper.updateCurriculum(curriculum));
        return requireActive(teacherId, curriculumId);
    }

    /**
     * 커리큘럼 행만 비활성화하고, 같은 강사의 뒤 순서를 당긴다.
     * TODO: Category와 ContentDetail이 구현되면 삭제 시 하위 행도 soft delete한다. 하위는 자동 복구하지 않는다.
     */
    @Transactional
    public void deleteCurriculum(Long teacherId, Long curriculumId) {
        lockActiveTeacher(teacherId);
        Curriculum curriculum = requireActive(teacherId, curriculumId);
        expectOne(curriculumMapper.softDeleteCurriculum(curriculumId, teacherId));
        curriculumMapper.shiftActiveDisplayOrdersDown(teacherId, curriculum.getDisplayOrder());
    }

    @Transactional
    public Curriculum restoreCurriculum(Long teacherId, Long curriculumId) {
        lockActiveTeacher(teacherId);
        Curriculum curriculum = curriculumMapper.selectCurriculumById(curriculumId);
        if (curriculum == null || !teacherId.equals(curriculum.getTeacherId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (curriculum.getStatus() == RecordStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT);
        }
        Integer maxOrder = curriculumMapper.selectMaxDisplayOrderByTeacherId(teacherId);
        curriculum.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        curriculum.setStatus(RecordStatus.ACTIVE);
        curriculum.setDeletedAt(null);
        expectOne(curriculumMapper.restoreCurriculum(curriculum));
        return requireActive(teacherId, curriculumId);
    }

    private Curriculum requireActive(Long teacherId, Long curriculumId) {
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(curriculumId, teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return curriculum;
    }

    private void lockActiveTeacher(Long teacherId) {
        if (teacherMapper.selectActiveTeacherById(teacherId) == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Teacher teacher;
        try {
            teacher = teacherMapper.lockTeacherById(teacherId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (teacher == null || teacher.getStatus() != RecordStatus.ACTIVE || teacher.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private Curriculum lockCurriculum(Long curriculumId) {
        try {
            Curriculum locked = curriculumMapper.lockCurriculumById(curriculumId);
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

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
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
