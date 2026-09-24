package com.yunki.lessonpt.common.mybatis.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.model.ProgressStatus;

class ProgressStatusTypeHandlerTest {

    private final ProgressStatusTypeHandler handler = new ProgressStatusTypeHandler();

    @Test
    void mapsDbValues() throws Exception {
        assertThat(ProgressStatus.YET.getDbValue()).isEqualTo("Yet");
        assertThat(ProgressStatus.IN_PROGRESS.getDbValue()).isEqualTo("InProgress");
        assertThat(ProgressStatus.COMPLETED.getDbValue()).isEqualTo("Completed");
        assertThat(ProgressStatus.STOPPED.getDbValue()).isEqualTo("Stopped");
        assertThat(handler.getNullableResult(resultSet("Yet"), "PROGRESS_STATUS")).isEqualTo(ProgressStatus.YET);
        assertThat(handler.getNullableResult(resultSet("InProgress"), "PROGRESS_STATUS"))
                .isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(handler.getNullableResult(resultSet("Completed"), "PROGRESS_STATUS"))
                .isEqualTo(ProgressStatus.COMPLETED);
        assertThat(handler.getNullableResult(resultSet("Stopped"), "PROGRESS_STATUS")).isEqualTo(ProgressStatus.STOPPED);

        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 1, ProgressStatus.IN_PROGRESS, JdbcType.VARCHAR);
        verify(statement).setString(1, "InProgress");
    }

    @Test
    void rejectsUnknownDbValue() throws Exception {
        assertThatThrownBy(() -> handler.getNullableResult(resultSet("unknown"), "PROGRESS_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProgressStatus.fromDbValue("Done"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet resultSet(String value) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("PROGRESS_STATUS")).thenReturn(value);
        return resultSet;
    }
}
