package com.yunki.lessonpt.common.mybatis.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * Oracle CHAR(1)의 Y/N 값을 Java Boolean으로 변환한다.
 *
 * 실제 참/거짓 의미를 가진 컬럼에만 사용하고,
 * 레코드 상태를 나타내는 STATUS에는 사용하지 않는다.
 */
@MappedTypes(Boolean.class)
public class BooleanYnTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.CHAR);
            return;
        }
        super.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, Boolean.TRUE.equals(parameter) ? "Y" : "N");
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Boolean parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return switch (value) {
            case "Y" -> Boolean.TRUE;
            case "N" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Boolean 컬럼 값이 Y 또는 N이 아니다.");
        };
    }
}
