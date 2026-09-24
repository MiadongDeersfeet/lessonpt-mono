package com.yunki.lessonpt.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;
import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;

@ExtendWith(MockitoExtension.class)
class ContentDetailServiceTest {

    @Mock
    private ContentDetailMapper contentDetailMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CurriculumMapper curriculumMapper;

    private ContentDetailService contentDetailService;

    @BeforeEach
    void setUp() {
        contentDetailService = new ContentDetailService(contentDetailMapper, categoryMapper, curriculumMapper);
    }

    @Test
    void createAssignsOrderOneWhenNoneExist() {
        stubOwnedParents();
        when(contentDetailMapper.selectMaxDisplayOrderByCategoryId(50L)).thenReturn(null);
        when(contentDetailMapper.insertContentDetail(any())).thenAnswer(invocation -> {
            invocation.<ContentDetail>getArgument(0).setContentDetailId(90L);
            return 1;
        });
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));

        ContentDetail created = contentDetailService.createContentDetail(8L, 40L, 50L, named("스케일"));

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        InOrder order = inOrder(curriculumMapper, categoryMapper, contentDetailMapper);
        order.verify(curriculumMapper).selectActiveCurriculumByIdAndTeacherId(40L, 8L);
        order.verify(categoryMapper).selectActiveCategoryByIdAndCurriculumId(50L, 40L);
        order.verify(categoryMapper).lockCategoryById(50L);
        order.verify(contentDetailMapper).selectMaxDisplayOrderByCategoryId(50L);
        order.verify(contentDetailMapper).insertContentDetail(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getCategoryId()).isEqualTo(50L);
        assertThat(created.getContentDetailId()).isEqualTo(90L);
    }

    @Test
    void createAppendsAfterExistingMax() {
        stubOwnedParents();
        when(contentDetailMapper.selectMaxDisplayOrderByCategoryId(50L)).thenReturn(3);
        when(contentDetailMapper.insertContentDetail(any())).thenAnswer(invocation -> {
            invocation.<ContentDetail>getArgument(0).setContentDetailId(91L);
            return 1;
        });
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(91L, 50L))
                .thenReturn(saved(91L, 50L, 4, "코드"));

        contentDetailService.createContentDetail(8L, 40L, 50L, named("코드"));

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        verify(contentDetailMapper).insertContentDetail(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(4);
    }

    @Test
    void createAcceptsBpmBoundsAndNull() {
        stubOwnedParents();
        when(contentDetailMapper.selectMaxDisplayOrderByCategoryId(50L)).thenReturn(null);
        when(contentDetailMapper.insertContentDetail(any())).thenAnswer(invocation -> {
            invocation.<ContentDetail>getArgument(0).setContentDetailId(90L);
            return 1;
        });
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));

        contentDetailService.createContentDetail(8L, 40L, 50L, namedWithBpm("스케일", null));
        contentDetailService.createContentDetail(8L, 40L, 50L, namedWithBpm("스케일", 60));
        contentDetailService.createContentDetail(8L, 40L, 50L, namedWithBpm("스케일", 240));

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        verify(contentDetailMapper, org.mockito.Mockito.times(3)).insertContentDetail(captor.capture());
        assertThat(captor.getAllValues()).extracting(ContentDetail::getTargetBpm).containsExactly(null, 60, 240);
    }

    @Test
    void createRejectsBpmOutOfRange() {
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> contentDetailService.createContentDetail(8L, 40L, 50L, namedWithBpm("스케일", 59)));
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> contentDetailService.createContentDetail(8L, 40L, 50L, namedWithBpm("스케일", 241)));
        verify(categoryMapper, never()).lockCategoryById(any());
    }

    @Test
    void createRejectsMissingParentsAndLockTimeout() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> contentDetailService.createContentDetail(9L, 40L, 50L, named("스케일")));

        stubOwnedCurriculum();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(51L, 40L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> contentDetailService.createContentDetail(8L, 40L, 51L, named("다른분류")));

        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> contentDetailService.createContentDetail(8L, 40L, 50L, named("비활성")));
        verify(categoryMapper, never()).lockCategoryById(any());

        stubOwnedParents();
        when(categoryMapper.lockCategoryById(50L)).thenThrow(new CannotAcquireLockException("ORA-30006"));
        assertCode(ErrorCode.ORDER_CONFLICT,
                () -> contentDetailService.createContentDetail(8L, 40L, 50L, named("스케일")));
        verify(contentDetailMapper, never()).insertContentDetail(any());
    }

    @Test
    void getReturnsOwnDetailsAndHidesOtherParents() {
        stubReadableParents();
        when(contentDetailMapper.selectActiveContentDetailsByCategoryId(50L))
                .thenReturn(List.of(saved(90L, 50L, 1, "스케일")));
        assertThat(contentDetailService.getContentDetails(8L, 40L, 50L))
                .extracting(ContentDetail::getName)
                .containsExactly("스케일");

        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));
        assertThat(contentDetailService.getContentDetail(8L, 40L, 50L, 90L).getName()).isEqualTo("스케일");

        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 9L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> contentDetailService.getContentDetail(9L, 40L, 50L, 90L));

        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(91L, 50L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> contentDetailService.getContentDetail(8L, 40L, 50L, 91L));
    }

    @Test
    void updateChangesSpecifiedFieldsAndKeepsDisplayOrder() {
        stubReadableParents();
        ContentDetail current = saved(90L, 50L, 1, "스케일");
        current.setMemo("기존");
        current.setTargetBpm(80);
        current.setSheetUrl("https://example.com/old");
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L)).thenReturn(current);
        when(contentDetailMapper.lockContentDetailById(90L)).thenReturn(saved(90L, 50L, 1, "스케일"));
        when(contentDetailMapper.updateContentDetail(any())).thenReturn(1);

        ContentDetailChange change = named("스케일 연습");
        change.setMemo("새 메모");
        change.setTargetBpm(90);
        change.setSheetUrl("https://example.com/sheet");
        change.setYoutubeUrl("https://youtu.be/scale");
        change.setAudioUrl("https://example.com/scale.mp3");
        contentDetailService.updateContentDetail(8L, 40L, 50L, 90L, change);

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        verify(contentDetailMapper).updateContentDetail(captor.capture());
        ContentDetail updated = captor.getValue();
        assertThat(updated.getName()).isEqualTo("스케일 연습");
        assertThat(updated.getMemo()).isEqualTo("새 메모");
        assertThat(updated.getTargetBpm()).isEqualTo(90);
        assertThat(updated.getSheetUrl()).isEqualTo("https://example.com/sheet");
        assertThat(updated.getYoutubeUrl()).isEqualTo("https://youtu.be/scale");
        assertThat(updated.getAudioUrl()).isEqualTo("https://example.com/scale.mp3");
        assertThat(updated.getDisplayOrder()).isEqualTo(1);
        verify(categoryMapper, never()).lockCategoryById(any());
    }

    @Test
    void updateClearsNullableFieldsWhenExplicitlyNull() {
        stubReadableParents();
        ContentDetail current = saved(90L, 50L, 2, "스케일");
        current.setMemo("기존");
        current.setTargetBpm(80);
        current.setEvaluationMemo("평가");
        current.setSheetUrl("https://example.com/sheet");
        current.setYoutubeUrl("https://youtu.be/scale");
        current.setAudioUrl("https://example.com/scale.mp3");
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L)).thenReturn(current);
        when(contentDetailMapper.lockContentDetailById(90L)).thenReturn(saved(90L, 50L, 2, "스케일"));
        when(contentDetailMapper.updateContentDetail(any())).thenReturn(1);

        ContentDetailChange change = new ContentDetailChange();
        change.setMemo(null);
        change.setTargetBpm(null);
        change.setEvaluationMemo(null);
        change.setSheetUrl(null);
        change.setYoutubeUrl(null);
        change.setAudioUrl(null);
        contentDetailService.updateContentDetail(8L, 40L, 50L, 90L, change);

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        verify(contentDetailMapper).updateContentDetail(captor.capture());
        ContentDetail updated = captor.getValue();
        assertThat(updated.getName()).isEqualTo("스케일");
        assertThat(updated.getMemo()).isNull();
        assertThat(updated.getTargetBpm()).isNull();
        assertThat(updated.getEvaluationMemo()).isNull();
        assertThat(updated.getSheetUrl()).isNull();
        assertThat(updated.getYoutubeUrl()).isNull();
        assertThat(updated.getAudioUrl()).isNull();
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void updateRejectsBlankNameBadBpmOtherCategoryAndBadRowCount() {
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> contentDetailService.updateContentDetail(8L, 40L, 50L, 90L, named(" ")));
        assertCode(ErrorCode.COMMON_INVALID_INPUT,
                () -> contentDetailService.updateContentDetail(8L, 40L, 50L, 90L, namedWithBpm("스케일", 59)));
        verify(contentDetailMapper, never()).updateContentDetail(any());

        stubReadableParents();
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(91L, 50L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND,
                () -> contentDetailService.updateContentDetail(8L, 40L, 50L, 91L, named("다른내용")));

        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));
        when(contentDetailMapper.lockContentDetailById(90L)).thenReturn(saved(90L, 50L, 1, "스케일"));
        when(contentDetailMapper.updateContentDetail(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR,
                () -> contentDetailService.updateContentDetail(8L, 40L, 50L, 90L, named("스케일 연습")));
    }

    @Test
    void deleteSoftDeletesAndCompressesOnlyThatCategory() {
        stubOwnedParents();
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));
        when(contentDetailMapper.softDeleteContentDetail(90L, 50L)).thenReturn(1);

        contentDetailService.deleteContentDetail(8L, 40L, 50L, 90L);

        InOrder order = inOrder(categoryMapper, contentDetailMapper);
        order.verify(categoryMapper).lockCategoryById(50L);
        order.verify(contentDetailMapper).selectActiveContentDetailByIdAndCategoryId(90L, 50L);
        order.verify(contentDetailMapper).softDeleteContentDetail(90L, 50L);
        order.verify(contentDetailMapper).shiftActiveDisplayOrdersDown(50L, 1);
        verify(contentDetailMapper, never()).shiftActiveDisplayOrdersDown(eq(51L), any());
    }

    @Test
    void deleteRejectsUnexpectedRowCount() {
        stubOwnedParents();
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 1, "스케일"));
        when(contentDetailMapper.softDeleteContentDetail(90L, 50L)).thenReturn(0);

        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> contentDetailService.deleteContentDetail(8L, 40L, 50L, 90L));
        verify(contentDetailMapper, never()).shiftActiveDisplayOrdersDown(any(), any());
    }

    @Test
    void restoreAppendsInactiveContentDetail() {
        stubOwnedParents();
        ContentDetail inactive = saved(90L, 50L, 1, "스케일");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(contentDetailMapper.selectContentDetailById(90L)).thenReturn(inactive);
        when(contentDetailMapper.selectMaxDisplayOrderByCategoryId(50L)).thenReturn(2);
        when(contentDetailMapper.restoreContentDetail(any())).thenReturn(1);
        when(contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(90L, 50L))
                .thenReturn(saved(90L, 50L, 3, "스케일"));

        ContentDetail restored = contentDetailService.restoreContentDetail(8L, 40L, 50L, 90L);

        ArgumentCaptor<ContentDetail> captor = ArgumentCaptor.forClass(ContentDetail.class);
        InOrder order = inOrder(categoryMapper, contentDetailMapper);
        order.verify(categoryMapper).lockCategoryById(50L);
        order.verify(contentDetailMapper).selectMaxDisplayOrderByCategoryId(50L);
        order.verify(contentDetailMapper).restoreContentDetail(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
        assertThat(restored.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void restoreRejectsActiveOtherCategoryInactiveParentAndBadRowCount() {
        stubOwnedParents();
        when(contentDetailMapper.selectContentDetailById(90L)).thenReturn(saved(90L, 50L, 1, "스케일"));
        assertCode(ErrorCode.COMMON_CONFLICT, () -> contentDetailService.restoreContentDetail(8L, 40L, 50L, 90L));

        ContentDetail otherCategory = saved(91L, 51L, 1, "다른분류");
        otherCategory.setStatus(RecordStatus.INACTIVE);
        when(contentDetailMapper.selectContentDetailById(91L)).thenReturn(otherCategory);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> contentDetailService.restoreContentDetail(8L, 40L, 50L, 91L));

        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(null);
        assertCode(ErrorCode.COMMON_NOT_FOUND, () -> contentDetailService.restoreContentDetail(8L, 40L, 50L, 90L));

        stubOwnedParents();
        ContentDetail inactive = saved(90L, 50L, 1, "스케일");
        inactive.setStatus(RecordStatus.INACTIVE);
        when(contentDetailMapper.selectContentDetailById(90L)).thenReturn(inactive);
        when(contentDetailMapper.selectMaxDisplayOrderByCategoryId(50L)).thenReturn(null);
        when(contentDetailMapper.restoreContentDetail(any())).thenReturn(0);
        assertCode(ErrorCode.COMMON_INTERNAL_ERROR, () -> contentDetailService.restoreContentDetail(8L, 40L, 50L, 90L));
    }

    private void stubOwnedCurriculum() {
        Curriculum curriculum = ownedCurriculum();
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(curriculum);
    }

    private void stubOwnedParents() {
        stubOwnedCurriculum();
        Category category = ownedCategory();
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(category);
        when(categoryMapper.lockCategoryById(50L)).thenReturn(category);
    }

    private void stubReadableParents() {
        when(curriculumMapper.selectActiveCurriculumByIdAndTeacherId(40L, 8L)).thenReturn(ownedCurriculum());
        when(categoryMapper.selectActiveCategoryByIdAndCurriculumId(50L, 40L)).thenReturn(ownedCategory());
    }

    private Curriculum ownedCurriculum() {
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumId(40L);
        curriculum.setTeacherId(8L);
        curriculum.setName("기초");
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private Category ownedCategory() {
        Category category = new Category();
        category.setCategoryId(50L);
        category.setCurriculumId(40L);
        category.setName("준비");
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private ContentDetail saved(Long contentDetailId, Long categoryId, int displayOrder, String name) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setContentDetailId(contentDetailId);
        contentDetail.setCategoryId(categoryId);
        contentDetail.setName(name);
        contentDetail.setDisplayOrder(displayOrder);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
    }

    private ContentDetailChange named(String name) {
        ContentDetailChange change = new ContentDetailChange();
        change.setName(name);
        return change;
    }

    private ContentDetailChange namedWithBpm(String name, Integer targetBpm) {
        ContentDetailChange change = named(name);
        change.setTargetBpm(targetBpm);
        return change;
    }

    private void assertCode(ErrorCode errorCode, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(errorCode);
    }
}
