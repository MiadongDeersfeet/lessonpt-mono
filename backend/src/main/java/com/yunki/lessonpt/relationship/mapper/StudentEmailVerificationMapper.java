package com.yunki.lessonpt.relationship.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.yunki.lessonpt.relationship.domain.StudentEmailVerification;

@Mapper
public interface StudentEmailVerificationMapper {

    StudentEmailVerification selectByVerificationId(Long verificationId);

    StudentEmailVerification selectCurrentPendingByAccessId(Long teacherStudentAccessId);

    StudentEmailVerification selectLatestByAccessId(Long teacherStudentAccessId);

    int insertStudentEmailVerification(StudentEmailVerification verification);

    int invalidatePendingByAccessId(StudentEmailVerification verification);

    int incrementFailureCount(StudentEmailVerification verification);

    int markConsumed(StudentEmailVerification verification);

    int markLocked(StudentEmailVerification verification);
}
