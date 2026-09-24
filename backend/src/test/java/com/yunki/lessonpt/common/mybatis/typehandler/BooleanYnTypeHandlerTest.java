package com.yunki.lessonpt.common.mybatis.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class BooleanYnTypeHandlerTest {

    private final BooleanYnTypeHandler handler = new BooleanYnTypeHandler();

    @Test
    void readsYesAndNo() throws Exception {
        ResultSet yes = resultSet("Y");
        ResultSet no = resultSet("N");

        assertThat(handler.getNullableResult(yes, "IS_COMPLETED")).isTrue();
        assertThat(handler.getNullableResult(no, "IS_COMPLETED")).isFalse();
    }

    @Test
    void writesYesAndNo() throws Exception {
        PreparedStatement yes = mock(PreparedStatement.class);
        PreparedStatement no = mock(PreparedStatement.class);

        handler.setNonNullParameter(yes, 1, true, JdbcType.CHAR);
        handler.setNonNullParameter(no, 1, false, JdbcType.CHAR);

        verify(yes).setString(1, "Y");
        verify(no).setString(1, "N");
    }

    @Test
    void readsNullAsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("IS_COMPLETED")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "IS_COMPLETED")).isNull();
    }

    @Test
    void writesNullAsSqlNull() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null, null);

        verify(ps).setNull(1, Types.CHAR);
    }

    @Test
    void rejectsInvalidDbValue() throws Exception {
        ResultSet rs = resultSet("TRUE");

        assertThatThrownBy(() -> handler.getNullableResult(rs, "IS_COMPLETED"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet resultSet(String value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("IS_COMPLETED")).thenReturn(value);
        return rs;
    }
}
