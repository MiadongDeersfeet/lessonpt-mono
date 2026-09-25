package com.yunki.lessonpt.relationship.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.yunki.lessonpt.relationship.domain.StudentLoginVerification;

@Mapper
public interface StudentLoginVerificationMapper {

    StudentLoginVerification selectCurrentPendingByStudentId(Long studentId);

    StudentLoginVerification selectLatestByStudentId(Long studentId);

    int insertStudentLoginVerification(StudentLoginVerification verification);

    int invalidatePendingByStudentId(StudentLoginVerification verification);

    int incrementFailureCount(StudentLoginVerification verification);

    int markConsumed(StudentLoginVerification verification);

    int markLocked(StudentLoginVerification verification);
}
