package com.yunki.lessonpt.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.dto.LocationCreateRequest;
import com.yunki.lessonpt.location.dto.LocationResponse;
import com.yunki.lessonpt.location.dto.LocationUpdateRequest;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private TeacherMapper teacherMapper;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(locationMapper, teacherMapper);
    }

    @Test
    void createLocksTeacherAndAssignsOrderOneWhenNoneExist() {
        when(teacherMapper.lockTeacherById(8L)).thenReturn(activeTeacher());
        when(locationMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(null);
        when(locationMapper.insertLocation(any())).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setLocationId(30L);
            return 1;
        });
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(saved(30L, 8L, 1, "Main"));

        LocationResponse response = locationService.createLocation(8L, new LocationCreateRequest("Main", null));

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).insertLocation(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getTeacherId()).isEqualTo(8L);
        assertThat(response.locationId()).isEqualTo(30L);
        assertThat(response.displayOrder()).isEqualTo(1);
        verify(teacherMapper).lockTeacherById(8L);
    }

    @Test
    void createAppendsAfterTwoExistingLocations() {
        when(teacherMapper.lockTeacherById(8L)).thenReturn(activeTeacher());
        when(locationMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(2);
        when(locationMapper.insertLocation(any())).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setLocationId(31L);
            return 1;
        });
        when(locationMapper.selectActiveLocationByIdAndTeacherId(31L, 8L)).thenReturn(saved(31L, 8L, 3, "Third"));

        LocationResponse response = locationService.createLocation(8L, new LocationCreateRequest("Third", "addr"));

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).insertLocation(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
        assertThat(response.displayOrder()).isEqualTo(3);
    }

    @Test
    void createMapsLockTimeoutToOrderConflict() {
        when(teacherMapper.lockTeacherById(8L)).thenThrow(new CannotAcquireLockException("ORA-30006"));

        assertThatThrownBy(() -> locationService.createLocation(8L, new LocationCreateRequest("Main", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ORDER_CONFLICT);
        verify(locationMapper, never()).insertLocation(any());
    }

    @Test
    void getReturnsOwnLocation() {
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(saved(30L, 8L, 1, "Main"));

        LocationResponse response = locationService.getLocation(8L, 30L);

        assertThat(response.name()).isEqualTo("Main");
        assertThat(response.locationId()).isEqualTo(30L);
    }

    @Test
    void getHidesMissingAndOtherTeachersLocation() {
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(null);

        assertThatThrownBy(() -> locationService.getLocation(8L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void updateAppliesSpecifiedFields() {
        Location current = saved(30L, 8L, 1, "Main");
        current.setAddress("old");
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(current);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setName("Renamed");

        locationService.updateLocation(8L, 30L, request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).updateLocation(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Renamed");
        assertThat(captor.getValue().getAddress()).isEqualTo("old");
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void updateClearsAddressWhenNullIsSent() {
        Location current = saved(30L, 8L, 1, "Main");
        current.setAddress("old");
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(current);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setAddress(null);

        locationService.updateLocation(8L, 30L, request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationMapper).updateLocation(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Main");
        assertThat(captor.getValue().getAddress()).isNull();
    }

    @Test
    void updateRejectsLocationTheTeacherDoesNotOwn() {
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(null);

        assertThatThrownBy(() -> locationService.updateLocation(8L, 30L, new LocationUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(locationMapper, never()).updateLocation(any());
    }

    @Test
    void deleteSoftDeletesAndCompressesLaterOrders() {
        when(teacherMapper.lockTeacherById(8L)).thenReturn(activeTeacher());
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(saved(30L, 8L, 1, "Main"));

        locationService.deleteLocation(8L, 30L);

        verify(teacherMapper).lockTeacherById(8L);
        verify(locationMapper).softDeleteLocation(30L, 8L);
        verify(locationMapper).shiftActiveDisplayOrdersDown(8L, 1);
    }

    @Test
    void restorePlacesInactiveLocationAtTheEnd() {
        when(teacherMapper.lockTeacherById(8L)).thenReturn(activeTeacher());
        Location inactive = saved(30L, 8L, 1, "Main");
        inactive.setStatus(RecordStatus.INACTIVE);
        inactive.setDeletedAt(LocalDateTime.now());
        when(locationMapper.selectLocationByIdAndTeacherId(30L, 8L)).thenReturn(inactive);
        when(locationMapper.selectMaxDisplayOrderByTeacherId(8L)).thenReturn(2);
        when(locationMapper.selectActiveLocationByIdAndTeacherId(30L, 8L)).thenReturn(saved(30L, 8L, 3, "Main"));

        LocationResponse response = locationService.restoreLocation(8L, 30L);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(teacherMapper).lockTeacherById(8L);
        verify(locationMapper).restoreLocation(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
        assertThat(captor.getValue().getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(response.displayOrder()).isEqualTo(3);
    }

    @Test
    void listReturnsActiveLocationsOnly() {
        when(locationMapper.selectActiveLocationsByTeacherId(8L)).thenReturn(List.of(saved(30L, 8L, 1, "Main")));

        List<LocationResponse> locations = locationService.getLocations(8L);

        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).name()).isEqualTo("Main");
    }

    private Teacher activeTeacher() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(8L);
        teacher.setStatus(RecordStatus.ACTIVE);
        return teacher;
    }

    private Location saved(Long locationId, Long teacherId, int order, String name) {
        Location location = new Location();
        location.setLocationId(locationId);
        location.setTeacherId(teacherId);
        location.setName(name);
        location.setDisplayOrder(order);
        location.setStatus(RecordStatus.ACTIVE);
        location.setCreatedAt(LocalDateTime.of(2026, 9, 24, 10, 0));
        location.setUpdatedAt(LocalDateTime.of(2026, 9, 24, 10, 0));
        return location;
    }
}
