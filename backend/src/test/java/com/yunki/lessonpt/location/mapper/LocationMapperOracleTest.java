package com.yunki.lessonpt.location.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.dto.LocationCreateRequest;
import com.yunki.lessonpt.location.dto.LocationResponse;
import com.yunki.lessonpt.location.dto.LocationUpdateRequest;
import com.yunki.lessonpt.location.service.LocationService;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 개발 Schema의 Main Studio, Second Studio는 조회만 한다.
 * 테스트가 만든 장소는 트랜잭션 롤백으로 남기지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class LocationMapperOracleTest {

    private static final String DEV_TEACHER_EMAIL = "dev.teacher@lessonpt.local";

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private LocationMapper locationMapper;

    @Autowired
    private LocationService locationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void readsSeedStudiosAndRoundTripsATemporaryLocation() {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);
        assertThat(teacher).isNotNull();
        Long teacherId = teacher.getTeacherId();

        assertSeedStudios(teacherId);

        String name = "IT Location " + UUID.randomUUID();
        LocationResponse created = locationService.createLocation(teacherId, new LocationCreateRequest(name, "temp"));
        assertThat(created.locationId()).isNotNull().isPositive();
        assertThat(created.displayOrder()).isEqualTo(3);
        assertThat(created.name()).isEqualTo(name);

        Location stored = locationMapper.selectActiveLocationByIdAndTeacherId(created.locationId(), teacherId);
        assertThat(stored.getStatus()).isEqualTo(RecordStatus.ACTIVE);

        LocationUpdateRequest update = new LocationUpdateRequest();
        update.setName(name + " updated");
        update.setAddress(null);
        LocationResponse updated = locationService.updateLocation(teacherId, created.locationId(), update);
        assertThat(updated.name()).isEqualTo(name + " updated");
        assertThat(updated.address()).isNull();
        assertThat(updated.displayOrder()).isEqualTo(3);

        locationService.deleteLocation(teacherId, created.locationId());
        assertThat(locationMapper.selectActiveLocationByIdAndTeacherId(created.locationId(), teacherId)).isNull();
        Location deleted = locationMapper.selectLocationByIdAndTeacherId(created.locationId(), teacherId);
        assertThat(deleted.getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertSeedStudios(teacherId);

        LocationResponse restored = locationService.restoreLocation(teacherId, created.locationId());
        assertThat(restored.locationId()).isEqualTo(created.locationId());
        assertThat(restored.displayOrder()).isEqualTo(3);
        assertThat(locationMapper.selectActiveLocationByIdAndTeacherId(created.locationId(), teacherId).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void insertIdCanBeReloadedAndRollbackLeavesNoRow() {
        Teacher teacher = teacherMapper.selectActiveTeacherByEmail(DEV_TEACHER_EMAIL);
        assertThat(teacher).isNotNull();
        Long teacherId = teacher.getTeacherId();
        String name = "IT Location " + UUID.randomUUID();

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        Long locationId = template.execute(status -> {
            LocationResponse created = locationService.createLocation(teacherId, new LocationCreateRequest(name, "temp"));
            assertThat(created.locationId()).isNotNull().isPositive();
            Location stored = locationMapper.selectLocationById(created.locationId());
            assertThat(stored).isNotNull();
            assertThat(stored.getName()).isEqualTo(name);
            assertThat(stored.getTeacherId()).isEqualTo(teacherId);
            status.setRollbackOnly();
            return created.locationId();
        });

        assertThat(locationMapper.selectLocationById(locationId)).isNull();
    }

    private void assertSeedStudios(Long teacherId) {
        var locations = locationMapper.selectActiveLocationsByTeacherId(teacherId);
        Location main = locations.stream().filter(location -> "Main Studio".equals(location.getName())).findFirst().orElseThrow();
        Location second = locations.stream().filter(location -> "Second Studio".equals(location.getName())).findFirst().orElseThrow();
        assertThat(main.getDisplayOrder()).isEqualTo(1);
        assertThat(second.getDisplayOrder()).isEqualTo(2);
        assertThat(main.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(RecordStatus.ACTIVE);
    }
}
