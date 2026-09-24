package com.yunki.lessonpt.location.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yunki.lessonpt.location.domain.Location;

@Mapper
public interface LocationMapper {

    Location selectLocationById(Long locationId);

    Location selectActiveLocationById(Long locationId);

    Location selectLocationByIdAndTeacherId(
            @Param("locationId") Long locationId,
            @Param("teacherId") Long teacherId);

    Location selectActiveLocationByIdAndTeacherId(
            @Param("locationId") Long locationId,
            @Param("teacherId") Long teacherId);

    List<Location> selectActiveLocationsByTeacherId(Long teacherId);

    int insertLocation(Location location);

    int updateLocation(Location location);

    int softDeleteLocation(
            @Param("locationId") Long locationId,
            @Param("teacherId") Long teacherId);

    int restoreLocation(Location location);

    Integer selectMaxDisplayOrderByTeacherId(Long teacherId);

    int shiftActiveDisplayOrdersDown(
            @Param("teacherId") Long teacherId,
            @Param("displayOrder") Integer displayOrder);

    Location lockLocationById(Long locationId);
}
