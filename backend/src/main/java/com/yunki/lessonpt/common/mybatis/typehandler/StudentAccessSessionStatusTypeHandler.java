package com.yunki.lessonpt.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.yunki.lessonpt.relationship.domain.StudentAccessSessionStatus;

@MappedTypes(StudentAccessSessionStatus.class)
public class StudentAccessSessionStatusTypeHandler extends BaseTypeHandler<StudentAccessSessionStatus> {

    @Override
    public void setParameter(PreparedStatement ps, int i, StudentAccessSessionStatus parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
            return;
        }
        super.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, StudentAccessSessionStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.dbValue());
    }

    @Override
    public StudentAccessSessionStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return StudentAccessSessionStatus.fromDbValue(rs.getString(columnName));
    }

    @Override
    public StudentAccessSessionStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return StudentAccessSessionStatus.fromDbValue(rs.getString(columnIndex));
    }

    @Override
    public StudentAccessSessionStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return StudentAccessSessionStatus.fromDbValue(cs.getString(columnIndex));
    }
}
