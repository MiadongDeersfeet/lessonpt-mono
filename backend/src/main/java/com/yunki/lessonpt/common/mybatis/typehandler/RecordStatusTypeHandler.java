package com.yunki.lessonpt.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.yunki.lessonpt.common.model.RecordStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * Oracle의 STATUS Y/N 값을 애플리케이션 상태 값으로 변환한다.
 *
 * STATUS는 단순 Boolean이 아니라 Soft Delete 정책과 연결되어 있어
 * IS_COMPLETED 같은 Boolean 컬럼과 같은 Handler를 사용하지 않는다.
 */
@MappedTypes(RecordStatus.class)
public class RecordStatusTypeHandler extends BaseTypeHandler<RecordStatus> {

    @Override
    public void setParameter(PreparedStatement ps, int i, RecordStatus parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.CHAR);
            return;
        }
        super.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RecordStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, toDb(parameter));
    }

    @Override
    public RecordStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public RecordStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public RecordStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private String toDb(RecordStatus status) {
        return switch (status) {
            case ACTIVE -> "Y";
            case INACTIVE -> "N";
        };
    }

    private RecordStatus parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return switch (value) {
            case "Y" -> RecordStatus.ACTIVE;
            case "N" -> RecordStatus.INACTIVE;
            default -> throw new IllegalArgumentException("STATUS 값이 Y 또는 N이 아니다.");
        };
    }
}
