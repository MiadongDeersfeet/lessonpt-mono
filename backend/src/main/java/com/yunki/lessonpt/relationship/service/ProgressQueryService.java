package com.yunki.lessonpt.relationship.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.yunki.lessonpt.relationship.mapper.ProgressQueryMapper;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;
import com.yunki.lessonpt.relationship.dto.ProgressSummary;
import com.yunki.lessonpt.relationship.dto.StudentCurriculumProgressResult;
import com.yunki.lessonpt.relationship.query.StudentCurriculumProgressView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgressQueryService {

    private static final int PERCENTAGE_SCALE = 1;

    private final StudentCurriculumMapper studentCurriculumMapper;
    private final TeacherStudentLocationMapper teacherStudentLocationMapper;
    private final TeacherStudentMapper teacherStudentMapper;
    private final CurriculumMapper curriculumMapper;
    private final ProgressQueryMapper progressQueryMapper;

    /**
     * 소유한 배정은 항상 한 건이다. 활성 내용이 없으면 progress는 null이다.
     */
    @Transactional(readOnly = true)
    public StudentCurriculumProgressResult getProgress(Long teacherId, Long studentCurriculumId) {
        requireOwnedEnrollment(teacherId, studentCurriculumId);
        StudentCurriculumProgressView view = progressQueryMapper.selectProgressByStudentCurriculumId(studentCurriculumId);
        if (view == null || !studentCurriculumId.equals(view.getStudentCurriculumId())) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
        return new StudentCurriculumProgressResult(studentCurriculumId, summarize(view));
    }

    /**
     * 요청한 배정마다 한 건이다. 활성 내용이 없는 배정도 progress null로 남긴다.
     */
    @Transactional(readOnly = true)
    public List<StudentCurriculumProgressResult> getProgresses(Long teacherId, List<Long> studentCurriculumIds) {
        if (studentCurriculumIds == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        if (studentCurriculumIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = studentCurriculumIds.stream().distinct().sorted().toList();
        List<Long> ownedIds = progressQueryMapper.selectOwnedActiveStudentCurriculumIds(teacherId, ids);
        if (ownedIds == null || !new HashSet<>(ownedIds).equals(new HashSet<>(ids))) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Map<Long, StudentCurriculumProgressView> views = progressQueryMapper
                .selectProgressByStudentCurriculumIds(ids)
                .stream()
                .collect(Collectors.toMap(StudentCurriculumProgressView::getStudentCurriculumId, Function.identity()));
        return ids.stream()
                .map(id -> new StudentCurriculumProgressResult(id, summarize(requireProgressView(views, id))))
                .toList();
    }

    public ProgressSummary summarize(StudentCurriculumProgressView view) {
        if (view == null) {
            return null;
        }
        requireConsistentCounts(view);
        if (view.getTotalCount() == 0) {
            return null;
        }
        BigDecimal percentage = BigDecimal.valueOf(view.getCompletedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(view.getTotalCount()), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return new ProgressSummary(view.getCompletedCount(), view.getTotalCount(), percentage);
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
        Curriculum curriculum = curriculumMapper.selectActiveCurriculumByIdAndTeacherId(
                enrollment.getCurriculumId(), teacherId);
        if (curriculum == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return enrollment;
    }

    private StudentCurriculumProgressView requireProgressView(
            Map<Long, StudentCurriculumProgressView> views, Long studentCurriculumId) {
        StudentCurriculumProgressView view = views.get(studentCurriculumId);
        if (view == null) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
        requireConsistentCounts(view);
        return view;
    }

    private void requireConsistentCounts(StudentCurriculumProgressView view) {
        Integer completedCount = view.getCompletedCount();
        Integer totalCount = view.getTotalCount();
        if (completedCount == null
                || totalCount == null
                || completedCount < 0
                || totalCount < 0
                || completedCount > totalCount) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

}
