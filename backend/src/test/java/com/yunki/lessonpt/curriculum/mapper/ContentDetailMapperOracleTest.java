package com.yunki.lessonpt.curriculum.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 강사, 커리큘럼, 카테고리, 내용을 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class ContentDetailMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CurriculumMapper curriculumMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ContentDetailMapper contentDetailMapper;

    @Test
    void insertsUpdatesDeletesAndRestoresInsideOneCategory() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.content." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        curriculumMapper.insertCurriculum(curriculum);

        Category categoryA = category(curriculum.getCurriculumId(), "준비", 1);
        Category categoryB = category(curriculum.getCurriculumId(), "다른분류", 2);
        categoryMapper.insertCategory(categoryA);
        categoryMapper.insertCategory(categoryB);

        String memo = "악보 메모 " + "가".repeat(400);
        String evaluationMemo = "평가 메모 " + "나".repeat(400);
        ContentDetail first = detail(categoryA.getCategoryId(), "스케일", 1, 60, memo, evaluationMemo,
                "https://example.com/sheet", "https://youtu.be/scale", "https://example.com/scale.mp3");
        ContentDetail second = detail(categoryA.getCategoryId(), "코드", 2, 240, null, null, null, null, null);
        ContentDetail third = detail(categoryA.getCategoryId(), "마무리", 3, null, null, null, null, null, null);
        ContentDetail other = detail(categoryB.getCategoryId(), "유지", 1, 80, "다른 메모", null, null, null, null);
        contentDetailMapper.insertContentDetail(first);
        contentDetailMapper.insertContentDetail(second);
        contentDetailMapper.insertContentDetail(third);
        contentDetailMapper.insertContentDetail(other);

        assertThat(first.getContentDetailId()).isNotNull().isPositive();
        ContentDetail reloaded = contentDetailMapper.selectContentDetailById(first.getContentDetailId());
        assertThat(reloaded.getName()).isEqualTo("스케일");
        assertThat(reloaded.getMemo()).isEqualTo(memo);
        assertThat(reloaded.getEvaluationMemo()).isEqualTo(evaluationMemo);
        assertThat(reloaded.getTargetBpm()).isEqualTo(60);
        assertThat(reloaded.getSheetUrl()).isEqualTo("https://example.com/sheet");
        assertThat(reloaded.getYoutubeUrl()).isEqualTo("https://youtu.be/scale");
        assertThat(reloaded.getAudioUrl()).isEqualTo("https://example.com/scale.mp3");
        assertThat(contentDetailMapper.selectActiveContentDetailById(second.getContentDetailId()).getTargetBpm())
                .isEqualTo(240);
        assertThat(contentDetailMapper.selectActiveContentDetailById(third.getContentDetailId()).getTargetBpm())
                .isNull();
        assertThat(contentDetailMapper.selectActiveContentDetailsByCategoryId(categoryA.getCategoryId()))
                .extracting(ContentDetail::getDisplayOrder)
                .containsExactly(1, 2, 3);
        assertThat(contentDetailMapper.selectMaxDisplayOrderByCategoryId(categoryA.getCategoryId())).isEqualTo(3);

        first.setName("스케일 연습");
        first.setMemo(null);
        first.setTargetBpm(null);
        first.setEvaluationMemo(null);
        first.setSheetUrl(null);
        first.setYoutubeUrl(null);
        first.setAudioUrl(null);
        assertThat(contentDetailMapper.updateContentDetail(first)).isEqualTo(1);
        ContentDetail cleared = contentDetailMapper.selectActiveContentDetailByIdAndCategoryId(
                first.getContentDetailId(), categoryA.getCategoryId());
        assertThat(cleared.getName()).isEqualTo("스케일 연습");
        assertThat(cleared.getDisplayOrder()).isEqualTo(1);
        assertThat(cleared.getMemo()).isNull();
        assertThat(cleared.getTargetBpm()).isNull();
        assertThat(cleared.getEvaluationMemo()).isNull();
        assertThat(cleared.getSheetUrl()).isNull();
        assertThat(cleared.getYoutubeUrl()).isNull();
        assertThat(cleared.getAudioUrl()).isNull();

        assertThat(contentDetailMapper.softDeleteContentDetail(first.getContentDetailId(), categoryA.getCategoryId()))
                .isEqualTo(1);
        assertThat(contentDetailMapper.shiftActiveDisplayOrdersDown(categoryA.getCategoryId(), 1)).isEqualTo(2);
        assertThat(contentDetailMapper.selectActiveContentDetailById(first.getContentDetailId())).isNull();
        assertThat(contentDetailMapper.selectActiveContentDetailById(second.getContentDetailId()).getDisplayOrder())
                .isEqualTo(1);
        assertThat(contentDetailMapper.selectActiveContentDetailById(third.getContentDetailId()).getDisplayOrder())
                .isEqualTo(2);
        assertThat(contentDetailMapper.selectActiveContentDetailById(other.getContentDetailId()).getDisplayOrder())
                .isEqualTo(1);

        first.setDisplayOrder(3);
        assertThat(contentDetailMapper.restoreContentDetail(first)).isEqualTo(1);
        assertThat(contentDetailMapper.selectActiveContentDetailsByCategoryId(categoryA.getCategoryId()))
                .extracting(ContentDetail::getName, ContentDetail::getDisplayOrder)
                .containsExactly(tuple("코드", 1), tuple("마무리", 2), tuple("스케일 연습", 3));
        assertThat(contentDetailMapper.selectActiveContentDetailsByCategoryId(categoryB.getCategoryId()))
                .extracting(ContentDetail::getName, ContentDetail::getDisplayOrder)
                .containsExactly(tuple("유지", 1));
    }

    @Test
    void categoryAndCurriculumCascadeLeaveTheOtherParentAndDoNotRestoreChildren() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.cascade." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Curriculum curriculumA = curriculum(teacher.getTeacherId(), "A", 1);
        Curriculum curriculumB = curriculum(teacher.getTeacherId(), "B", 2);
        curriculumMapper.insertCurriculum(curriculumA);
        curriculumMapper.insertCurriculum(curriculumB);

        Category categoryA1 = category(curriculumA.getCurriculumId(), "A1", 1);
        Category categoryA2 = category(curriculumA.getCurriculumId(), "A2", 2);
        Category categoryB1 = category(curriculumB.getCurriculumId(), "B1", 1);
        categoryMapper.insertCategory(categoryA1);
        categoryMapper.insertCategory(categoryA2);
        categoryMapper.insertCategory(categoryB1);

        ContentDetail detailA1 = detail(categoryA1.getCategoryId(), "A1", 1, null, null, null, null, null, null);
        ContentDetail detailA2 = detail(categoryA2.getCategoryId(), "A2", 1, null, null, null, null, null, null);
        ContentDetail detailB1 = detail(categoryB1.getCategoryId(), "B1", 1, null, null, null, null, null, null);
        contentDetailMapper.insertContentDetail(detailA1);
        contentDetailMapper.insertContentDetail(detailA2);
        contentDetailMapper.insertContentDetail(detailB1);

        assertThat(contentDetailMapper.softDeleteActiveContentDetailsByCategoryId(categoryA1.getCategoryId()))
                .isEqualTo(1);
        assertThat(categoryMapper.softDeleteCategory(categoryA1.getCategoryId(), curriculumA.getCurriculumId()))
                .isEqualTo(1);
        assertThat(contentDetailMapper.selectContentDetailById(detailA1.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(categoryMapper.selectCategoryById(categoryA2.getCategoryId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailA2.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailB1.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);

        categoryA1.setDisplayOrder(2);
        assertThat(categoryMapper.restoreCategory(categoryA1)).isEqualTo(1);
        assertThat(categoryMapper.selectCategoryById(categoryA1.getCategoryId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailA1.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);

        assertThat(contentDetailMapper.softDeleteActiveContentDetailsByCategoryId(categoryA1.getCategoryId()))
                .isEqualTo(0);
        assertThat(contentDetailMapper.softDeleteActiveContentDetailsByCategoryId(categoryA2.getCategoryId()))
                .isEqualTo(1);
        assertThat(categoryMapper.softDeleteActiveCategoriesByCurriculumId(curriculumA.getCurriculumId())).isEqualTo(2);
        assertThat(categoryMapper.selectCategoryById(categoryA1.getCategoryId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(categoryMapper.selectCategoryById(categoryA2.getCategoryId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailA2.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
        assertThat(categoryMapper.selectCategoryById(categoryB1.getCategoryId()).getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailB1.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);

        assertThat(curriculumMapper.softDeleteCurriculum(curriculumA.getCurriculumId(), teacher.getTeacherId()))
                .isEqualTo(1);
        curriculumA.setDisplayOrder(3);
        assertThat(curriculumMapper.restoreCurriculum(curriculumA)).isEqualTo(1);
        assertThat(curriculumMapper.selectCurriculumById(curriculumA.getCurriculumId()).getStatus())
                .isEqualTo(RecordStatus.ACTIVE);
        assertThat(categoryMapper.selectCategoryById(categoryA2.getCategoryId()).getStatus()).isEqualTo(RecordStatus.INACTIVE);
        assertThat(contentDetailMapper.selectContentDetailById(detailA2.getContentDetailId()).getStatus())
                .isEqualTo(RecordStatus.INACTIVE);
    }

    private Curriculum curriculum(Long teacherId, String name, int displayOrder) {
        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacherId);
        curriculum.setName(name);
        curriculum.setDisplayOrder(displayOrder);
        curriculum.setStatus(RecordStatus.ACTIVE);
        return curriculum;
    }

    private Category category(Long curriculumId, String name, int displayOrder) {
        Category category = new Category();
        category.setCurriculumId(curriculumId);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setStatus(RecordStatus.ACTIVE);
        return category;
    }

    private ContentDetail detail(
            Long categoryId,
            String name,
            int displayOrder,
            Integer targetBpm,
            String memo,
            String evaluationMemo,
            String sheetUrl,
            String youtubeUrl,
            String audioUrl) {
        ContentDetail contentDetail = new ContentDetail();
        contentDetail.setCategoryId(categoryId);
        contentDetail.setName(name);
        contentDetail.setDisplayOrder(displayOrder);
        contentDetail.setTargetBpm(targetBpm);
        contentDetail.setMemo(memo);
        contentDetail.setEvaluationMemo(evaluationMemo);
        contentDetail.setSheetUrl(sheetUrl);
        contentDetail.setYoutubeUrl(youtubeUrl);
        contentDetail.setAudioUrl(audioUrl);
        contentDetail.setStatus(RecordStatus.ACTIVE);
        return contentDetail;
    }
}
