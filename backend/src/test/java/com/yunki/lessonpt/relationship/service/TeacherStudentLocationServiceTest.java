package com.yunki.lessonpt.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.location.domain.Location;
import com.yunki.lessonpt.location.mapper.LocationMapper;
import com.yunki.lessonpt.relationship.domain.TeacherStudent;
import com.yunki.lessonpt.relationship.domain.TeacherStudentLocation;
import com.yunki.lessonpt.relationship.dto.TeacherStudentLocationView;
import com.yunki.lessonpt.relationship.mapper.StudentCurriculumMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentLocationMapper;
import com.yunki.lessonpt.relationship.mapper.TeacherStudentMapper;

@ExtendWith(MockitoExtension.class)
class TeacherStudentLocationServiceTest {

    @Mock
    private TeacherStudentMapper teacherStudentMapper;

    @Mock
    private LocationMapper locationMapper;

    @Mock
    private TeacherStudentLocationMapper teacherStudentLocationMapper;

    @Mock
    private StudentCurriculumMapper studentCurriculumMapper;

    private TeacherStudentLocationService service;

    @BeforeEach
    void setUp() {
        service = new TeacherStudentLocationService(
                teacherStudentMapper, locationMapper, teacherStudentLocationMapper, studentCurriculumMapper);
    }

    @Test
    void assignInsertsAfterLockingTeacherStudentThenLocation() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L)).thenReturn(null);
        when(teacherStudentLocationMapper.insertTeacherStudentLocation(any())).thenAnswer(invocation -> {
            invocation.<TeacherStudentLocation>getArgument(0).setTeacherStudentLocationId(90L);
            return 1;
        });
        when(teacherStudentLocationMapper.selectActiveViewById(90L)).thenReturn(view(90L, "Main Studio", "Seoul"));

        TeacherStudentLocationView assigned = service.assignLocation(8L, 41L, 30L);

        InOrder order = inOrder(teacherStudentMapper, locationMapper, teacherStudentLocationMapper);
        order.verify(teacherStudentMapper).selectActiveByTeacherIdAndStudentId(8L, 41L);
        order.verify(teacherStudentMapper).lockTeacherStudentById(70L);
        order.verify(locationMapper).lockLocationById(30L);
        order.verify(teacherStudentLocationMapper).selectByTeacherStudentIdAndLocationId(70L, 30L);
        order.verify(teacherStudentLocationMapper).insertTeacherStudentLocation(any());
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
        assertThat(assigned.getTeacherStudentLocationId()).isEqualTo(90L);
    }

    @Test
    void assignRestoresInactiveLink() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.INACTIVE));
        when(teacherStudentLocationMapper.restoreTeacherStudentLocation(90L)).thenReturn(1);
        when(teacherStudentLocationMapper.selectActiveViewById(90L)).thenReturn(view(90L, "Main Studio", "Seoul"));

        service.assignLocation(8L, 41L, 30L);

        verify(teacherStudentLocationMapper).restoreTeacherStudentLocation(90L);
        verify(teacherStudentLocationMapper, never()).insertTeacherStudentLocation(any());
    }

    @Test
    void assignRejectsActiveLink() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.ACTIVE));

        assertConflict(() -> service.assignLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).insertTeacherStudentLocation(any());
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
    }

    @Test
    void assignHidesStudentThatIsNotMine() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);

        assertNotFound(() -> service.assignLocation(8L, 41L, 30L));
        verify(locationMapper, never()).lockLocationById(any());
    }

    @Test
    void assignHidesLocationOwnedByAnotherTeacher() {
        stubOwnedRows(8L, 9L);

        assertNotFound(() -> service.assignLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).insertTeacherStudentLocation(any());
    }

    @Test
    void assignRejectsWhenTeacherStudentAndLocationTeachersDiffer() {
        stubOwnedRows(8L, 9L);

        assertNotFound(() -> service.assignLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).selectByTeacherStudentIdAndLocationId(any(), any());
    }

    @Test
    void releaseSoftDeletesActiveLink() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.lockTeacherStudentLocationById(90L)).thenReturn(link(90L, RecordStatus.ACTIVE));
        when(teacherStudentLocationMapper.softDeleteTeacherStudentLocation(90L)).thenReturn(1);

        service.releaseLocation(8L, 41L, 30L);

        InOrder order = inOrder(teacherStudentMapper, locationMapper, teacherStudentLocationMapper, studentCurriculumMapper);
        order.verify(teacherStudentMapper).lockTeacherStudentById(70L);
        order.verify(locationMapper).lockLocationById(30L);
        order.verify(teacherStudentLocationMapper).lockTeacherStudentLocationById(90L);
        order.verify(studentCurriculumMapper).softDeleteActiveStudentCurriculumsByTeacherStudentLocationId(90L);
        order.verify(teacherStudentLocationMapper).softDeleteTeacherStudentLocation(90L);
    }

    @Test
    void restoreDoesNotRestoreEnrollments() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.INACTIVE));
        when(teacherStudentLocationMapper.restoreTeacherStudentLocation(90L)).thenReturn(1);
        when(teacherStudentLocationMapper.selectActiveViewById(90L)).thenReturn(view(90L, "Main Studio", "Seoul"));

        service.restoreLocation(8L, 41L, 30L);

        verify(studentCurriculumMapper, never()).restoreStudentCurriculum(any());
    }

    @Test
    void releaseReturnsNotFoundWhenLinkIsMissing() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectActiveByTeacherStudentIdAndLocationId(70L, 30L)).thenReturn(null);

        assertNotFound(() -> service.releaseLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).softDeleteTeacherStudentLocation(any());
    }

    @Test
    void releaseBlocksAnotherTeachersStudent() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);

        assertNotFound(() -> service.releaseLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).softDeleteTeacherStudentLocation(any());
    }

    @Test
    void restoreActivatesInactiveLink() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.INACTIVE));
        when(teacherStudentLocationMapper.restoreTeacherStudentLocation(90L)).thenReturn(1);
        when(teacherStudentLocationMapper.selectActiveViewById(90L)).thenReturn(view(90L, "Main Studio", "Seoul"));

        TeacherStudentLocationView restored = service.restoreLocation(8L, 41L, 30L);

        verify(teacherStudentLocationMapper).restoreTeacherStudentLocation(90L);
        assertThat(restored.getTeacherStudentLocationId()).isEqualTo(90L);
        assertThat(restored.getLocationName()).isEqualTo("Main Studio");
    }

    @Test
    void restoreRejectsAlreadyActiveLink() {
        stubOwnedRows(8L, 8L);
        when(teacherStudentLocationMapper.selectByTeacherStudentIdAndLocationId(70L, 30L))
                .thenReturn(link(90L, RecordStatus.ACTIVE));

        assertConflict(() -> service.restoreLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
    }

    @Test
    void restoreFailsWhenTeacherStudentIsInactive() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(null);

        assertNotFound(() -> service.restoreLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
    }

    @Test
    void restoreFailsWhenLocationIsInactive() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(8L));
        when(teacherStudentMapper.lockTeacherStudentById(70L)).thenReturn(relation(8L));
        Location inactive = location(8L);
        inactive.setStatus(RecordStatus.INACTIVE);
        when(locationMapper.lockLocationById(30L)).thenReturn(inactive);

        assertNotFound(() -> service.restoreLocation(8L, 41L, 30L));
        verify(teacherStudentLocationMapper, never()).restoreTeacherStudentLocation(any());
    }

    @Test
    void listReturnsActiveLinksForTheTeachersStudent() {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(8L));
        when(teacherStudentLocationMapper.selectActiveViewsByTeacherStudentId(70L))
                .thenReturn(List.of(view(90L, "Main Studio", "Seoul")));

        List<TeacherStudentLocationView> locations = service.getStudentLocations(8L, 41L);

        assertThat(locations).extracting(TeacherStudentLocationView::getTeacherStudentLocationId).containsExactly(90L);
        verify(teacherStudentLocationMapper).selectActiveViewsByTeacherStudentId(70L);
    }

    private void stubOwnedRows(Long relationTeacherId, Long locationTeacherId) {
        when(teacherStudentMapper.selectActiveByTeacherIdAndStudentId(8L, 41L)).thenReturn(relation(relationTeacherId));
        when(teacherStudentMapper.lockTeacherStudentById(70L)).thenReturn(relation(relationTeacherId));
        when(locationMapper.lockLocationById(30L)).thenReturn(location(locationTeacherId));
    }

    private TeacherStudent relation(Long teacherId) {
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherStudentId(70L);
        relation.setTeacherId(teacherId);
        relation.setStudentId(41L);
        relation.setStatus(RecordStatus.ACTIVE);
        return relation;
    }

    private Location location(Long teacherId) {
        Location location = new Location();
        location.setLocationId(30L);
        location.setTeacherId(teacherId);
        location.setStatus(RecordStatus.ACTIVE);
        return location;
    }

    private TeacherStudentLocationView view(Long id, String locationName, String address) {
        TeacherStudentLocationView view = new TeacherStudentLocationView();
        view.setTeacherStudentLocationId(id);
        view.setLocationId(30L);
        view.setLocationName(locationName);
        view.setAddress(address);
        return view;
    }

    private TeacherStudentLocation link(Long id, RecordStatus status) {
        TeacherStudentLocation link = new TeacherStudentLocation();
        link.setTeacherStudentLocationId(id);
        link.setTeacherStudentId(70L);
        link.setLocationId(30L);
        link.setStatus(status);
        return link;
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);
    }
}
