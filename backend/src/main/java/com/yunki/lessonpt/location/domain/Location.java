package com.yunki.lessonpt.location.domain;

import java.time.LocalDateTime;

import com.yunki.lessonpt.common.model.RecordStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사에게 속한 연습 장소다.
 * 응답으로는 쓰지 않고, 삭제 시각은 서비스 안에서만 다룬다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Location {

    private Long locationId;
    private Long teacherId;
    private String name;
    private Integer displayOrder;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecordStatus status;
    private LocalDateTime deletedAt;
}
