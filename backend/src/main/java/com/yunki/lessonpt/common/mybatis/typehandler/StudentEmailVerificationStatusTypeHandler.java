package com.yunki.lessonpt.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.yunki.lessonpt.relationship.domain.StudentEmailVerificationStatus;

@MappedTypes(StudentEmailVerificationStatus.class)
public class StudentEmailVerificationStatusTypeHandler extends BaseTypeHandler<StudentEmailVerificationStatus> {

    @Override
    public void setParameter(PreparedStatement ps, int i, StudentEmailVerificationStatus parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
            return;
        }
        super.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, StudentEmailVerificationStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.dbValue());
    }

    @Override
    public StudentEmailVerificationStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return StudentEmailVerificationStatus.fromDbValue(rs.getString(columnName));
    }

    @Override
    public StudentEmailVerificationStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return StudentEmailVerificationStatus.fromDbValue(rs.getString(columnIndex));
    }

    @Override
    public StudentEmailVerificationStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return StudentEmailVerificationStatus.fromDbValue(cs.getString(columnIndex));
    }
}
