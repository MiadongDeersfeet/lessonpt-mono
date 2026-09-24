package com.yunki.lessonpt.relationship.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.student.domain.Student;
import com.yunki.lessonpt.student.mapper.StudentMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 학생·장소·관계를 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class TeacherStudentLocationMapperOracleTest {

    private static final String DEV_TEACHER_EMAIL = "dev.teacher@lessonpt.local";

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Autowired
    private LocationMapper locationMapper;

    @Autowired
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Test
    void insertsReadsSoftDeletesAndRestores() {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);
        assertThat(teacher).isNotNull();

        Student student = new Student();
        student.setName("IT Location Link " + UUID.randomUUID());
        student.setStatus(RecordStatus.ACTIVE);
        studentMapper.insertStudent(student);

        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacher.getTeacherId());
        relation.setStudentId(student.getStudentId());
        relation.setStatus(RecordStatus.ACTIVE);
        teacherStudentMapper.insertTeacherStudent(relation);

        Integer maxOrder = locationMapper.selectMaxDisplayOrderByTeacherId(teacher.getTeacherId());
        Location location = new Location();
        location.setTeacherId(teacher.getTeacherId());
        String locationName = "IT Link " + UUID.randomUUID();
        location.setName(locationName);
        location.setAddress("Seoul");
        location.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        location.setStatus(RecordStatus.ACTIVE);
        locationMapper.insertLocation(location);

        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentId(relation.getTeacherStudentId());
        link.setLocationId(location.getLocationId());
        link.setStatus(RecordStatus.ACTIVE);
        teacherStudentLocationMapper.insertTeacherStudentLocation(link);

        assertThat(link.getTeacherStudentLocationId()).isNotNull().isPositive();
        TeacherStudentLocation stored = teacherStudentLocationMapper
                .selectTeacherStudentLocationById(link.getTeacherStudentLocationId());
        assertThat(stored.getTeacherStudentId()).isEqualTo(relation.getTeacherStudentId());
        assertThat(stored.getLocationId()).isEqualTo(location.getLocationId());
        assertThat(stored.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(stored.getDeletedAt()).isNull();
        var view = teacherStudentLocationMapper.selectActiveViewById(link.getTeacherStudentLocationId());
        assertThat(view.getLocationName()).isEqualTo(locationName);
        assertThat(view.getAddress()).isEqualTo("Seoul");
        assertThat(view.getLocationId()).isEqualTo(location.getLocationId());
        assertThat(teacherStudentLocationMapper.selectActiveViewsByTeacherStudentId(relation.getTeacherStudentId()))
                .extracting(com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView::getLocationName)
                .contains(locationName);
        assertThat(teacherStudentLocationMapper.selectActiveByTeacherStudentId(relation.getTeacherStudentId()))
                .extracting(TeacherStudentLocation::getTeacherStudentLocationId)
                .contains(link.getTeacherStudentLocationId());

        assertThat(teacherStudentLocationMapper.softDeleteTeacherStudentLocation(link.getTeacherStudentLocationId())).isEqualTo(1);
        assertThat(teacherStudentLocationMapper.selectActiveTeacherStudentLocationById(link.getTeacherStudentLocationId())).isNull();
        assertThat(teacherStudentLocationMapper.selectTeacherStudentLocationById(link.getTeacherStudentLocationId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);

        assertThat(teacherStudentLocationMapper.restoreTeacherStudentLocation(link.getTeacherStudentLocationId())).isEqualTo(1);
        assertThat(teacherStudentLocationMapper
                .selectActiveByTeacherStudentIdAndLocationId(relation.getTeacherStudentId(), location.getLocationId())
                .getStatus()).isEqualTo(RecordStatus.ACTIVE);
    }
}
