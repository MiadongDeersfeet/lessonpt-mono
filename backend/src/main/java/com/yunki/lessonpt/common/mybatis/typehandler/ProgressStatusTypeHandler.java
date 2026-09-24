package com.yunki.lessonpt.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.yunki.lessonpt.common.model.ProgressStatus;

/**
 * Oracle VARCHAR2 진행 상태를 애플리케이션 값으로 변환한다.
 * 레코드 활성 여부를 나타내는 STATUS와는 다른 컬럼이다.
 */
@MappedTypes(ProgressStatus.class)
public class ProgressStatusTypeHandler extends BaseTypeHandler<ProgressStatus> {

    @Override
    public void setParameter(PreparedStatement ps, int i, ProgressStatus parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
            return;
        }
        super.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProgressStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getDbValue());
    }

    @Override
    public ProgressStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return ProgressStatus.fromDbValue(rs.getString(columnName));
    }

    @Override
    public ProgressStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return ProgressStatus.fromDbValue(rs.getString(columnIndex));
    }

    @Override
    public ProgressStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return ProgressStatus.fromDbValue(cs.getString(columnIndex));
    }
}
