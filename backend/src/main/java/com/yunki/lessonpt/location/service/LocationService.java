package com.yunki.lessonpt.location.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;
    private final TeacherMapper teacherMapper;

    @Transactional
    public LocationResponse createLocation(Long teacherId, LocationCreateRequest request) {
        lockActiveTeacher(teacherId);
        Integer maxOrder = locationMapper.selectMaxDisplayOrderByTeacherId(teacherId);
        int nextOrder = maxOrder == null ? 1 : maxOrder + 1;

        Location location = new Location();
        location.setTeacherId(teacherId);
        location.setName(request.name());
        location.setAddress(request.address());
        location.setDisplayOrder(nextOrder);
        location.setStatus(RecordStatus.ACTIVE);
        locationMapper.insertLocation(location);
        return toResponse(requireActive(teacherId, location.getLocationId()));
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long teacherId, Long locationId) {
        return toResponse(requireActive(teacherId, locationId));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations(Long teacherId) {
        return locationMapper.selectActiveLocationsByTeacherId(teacherId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LocationResponse updateLocation(Long teacherId, Long locationId, LocationUpdateRequest request) {
        Location location = requireActive(teacherId, locationId);
        if (request.isNameSpecified()) {
            location.setName(request.getName());
        }
        if (request.isAddressSpecified()) {
            location.setAddress(request.getAddress());
        }
        locationMapper.updateLocation(location);
        return toResponse(requireActive(teacherId, locationId));
    }

    @Transactional
    public void deleteLocation(Long teacherId, Long locationId) {
        lockActiveTeacher(teacherId);
        Location location = requireActive(teacherId, locationId);
        locationMapper.softDeleteLocation(locationId, teacherId);
        locationMapper.shiftActiveDisplayOrdersDown(teacherId, location.getDisplayOrder());
    }

    @Transactional
    public LocationResponse restoreLocation(Long teacherId, Long locationId) {
        lockActiveTeacher(teacherId);
        Location location = locationMapper.selectLocationByIdAndTeacherId(locationId, teacherId);
        if (location == null || location.getStatus() != RecordStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        Integer maxOrder = locationMapper.selectMaxDisplayOrderByTeacherId(teacherId);
        int nextOrder = maxOrder == null ? 1 : maxOrder + 1;
        location.setDisplayOrder(nextOrder);
        location.setStatus(RecordStatus.ACTIVE);
        location.setDeletedAt(null);
        locationMapper.restoreLocation(location);
        return toResponse(requireActive(teacherId, locationId));
    }

    private Location requireActive(Long teacherId, Long locationId) {
        Location location = locationMapper.selectActiveLocationByIdAndTeacherId(locationId, teacherId);
        if (location == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return location;
    }

    private void lockActiveTeacher(Long teacherId) {
        Teacher teacher;
        try {
            teacher = teacherMapper.lockTeacherById(teacherId);
        } catch (RuntimeException exception) {
            if (isLockTimeout(exception)) {
                throw new BusinessException(ErrorCode.ORDER_CONFLICT);
            }
            throw exception;
        }
        if (teacher == null || teacher.getStatus() != RecordStatus.ACTIVE || teacher.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private boolean isLockTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotAcquireLockException) {
                return true;
            }
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 30006) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("ORA-30006")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getLocationId(),
                location.getName(),
                location.getDisplayOrder(),
                location.getAddress(),
                location.getCreatedAt(),
                location.getUpdatedAt());
    }
}
