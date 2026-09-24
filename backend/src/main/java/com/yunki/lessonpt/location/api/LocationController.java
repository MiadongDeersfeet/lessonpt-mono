package com.yunki.lessonpt.location.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.yunki.lessonpt.auth.security.TeacherPrincipal;
import com.yunki.lessonpt.location.dto.LocationCreateRequest;
import com.yunki.lessonpt.location.dto.LocationResponse;
import com.yunki.lessonpt.location.dto.LocationUpdateRequest;
import com.yunki.lessonpt.location.service.LocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> create(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @Valid @RequestBody LocationCreateRequest request) {
        LocationResponse body = locationService.createLocation(principal.teacherId(), request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{locationId}")
                .buildAndExpand(body.locationId())
                .toUri();
        return ResponseEntity.created(uri).body(body);
    }

    @GetMapping
    public List<LocationResponse> list(@AuthenticationPrincipal TeacherPrincipal principal) {
        return locationService.getLocations(principal.teacherId());
    }

    @GetMapping("/{locationId}")
    public LocationResponse get(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long locationId) {
        return locationService.getLocation(principal.teacherId(), locationId);
    }

    @PatchMapping("/{locationId}")
    public LocationResponse update(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long locationId,
            @Valid @RequestBody LocationUpdateRequest request) {
        return locationService.updateLocation(principal.teacherId(), locationId, request);
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long locationId) {
        locationService.deleteLocation(principal.teacherId(), locationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{locationId}/restore")
    public LocationResponse restore(
            @AuthenticationPrincipal TeacherPrincipal principal,
            @PathVariable Long locationId) {
        return locationService.restoreLocation(principal.teacherId(), locationId);
    }
}
