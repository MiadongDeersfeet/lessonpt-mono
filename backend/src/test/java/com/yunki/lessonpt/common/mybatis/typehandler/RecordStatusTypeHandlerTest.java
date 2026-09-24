package com.yunki.lessonpt.common.mybatis.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import com.yunki.lessonpt.common.model.RecordStatus;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class RecordStatusTypeHandlerTest {

    private final RecordStatusTypeHandler handler = new RecordStatusTypeHandler();

    @Test
    void readsActiveAndInactive() throws Exception {
        ResultSet active = resultSet("Y");
        ResultSet inactive = resultSet("N");

        assertThat(handler.getNullableResult(active, "STATUS")).isEqualTo(RecordStatus.ACTIVE);
        assertThat(handler.getNullableResult(inactive, "STATUS")).isEqualTo(RecordStatus.INACTIVE);
    }

    @Test
    void writesActiveAndInactive() throws Exception {
        PreparedStatement active = mock(PreparedStatement.class);
        PreparedStatement inactive = mock(PreparedStatement.class);

        handler.setNonNullParameter(active, 1, RecordStatus.ACTIVE, JdbcType.CHAR);
        handler.setNonNullParameter(inactive, 1, RecordStatus.INACTIVE, JdbcType.CHAR);

        verify(active).setString(1, "Y");
        verify(inactive).setString(1, "N");
    }

    @Test
    void readsNullAsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("STATUS")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "STATUS")).isNull();
    }

    @Test
    void writesNullAsSqlNull() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null, null);

        verify(ps).setNull(1, Types.CHAR);
    }

    @Test
    void rejectsInvalidDbValue() throws Exception {
        ResultSet rs = resultSet("X");

        ResultSet blank = resultSet(" ");

        assertThatThrownBy(() -> handler.getNullableResult(rs, "STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> handler.getNullableResult(blank, "STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet resultSet(String value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("STATUS")).thenReturn(value);
        return rs;
    }
}
