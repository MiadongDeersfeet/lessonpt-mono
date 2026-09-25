package com.yunki.lessonpt.resource.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.yunki.lessonpt.common.model.RecordStatus;
import com.yunki.lessonpt.curriculum.domain.Category;
import com.yunki.lessonpt.curriculum.domain.ContentDetail;
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.curriculum.mapper.CategoryMapper;
import com.yunki.lessonpt.curriculum.mapper.ContentDetailMapper;
import com.yunki.lessonpt.curriculum.mapper.CurriculumMapper;
import com.yunki.lessonpt.resource.domain.ContentResource;
import com.yunki.lessonpt.resource.domain.ResourceType;
import com.yunki.lessonpt.resource.service.ResourceCleanup;
import com.yunki.lessonpt.resource.storage.ObjectStorageGateway;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class ContentResourceMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CurriculumMapper curriculumMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ContentDetailMapper contentDetailMapper;
    @Autowired
    private ContentResourceMapper contentResourceMapper;
    @Autowired
    private ResourceCleanup resourceCleanup;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private ObjectStorageGateway objectStorageGateway;

    @Test
    void insertsThenSoftDeletesBeforeAnotherActiveRow() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.resource." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
        curriculum.setName("자료");
        curriculum.setDisplayOrder(1);
        curriculum.setStatus(RecordStatus.ACTIVE);
        curriculumMapper.insertCurriculum(curriculum);

        Category category = new Category();
        category.setCurriculumId(curriculum.getCurriculumId());
        category.setName("분류");
        category.setDisplayOrder(1);
        category.setStatus(RecordStatus.ACTIVE);
        categoryMapper.insertCategory(category);

        ContentDetail detail = new ContentDetail();
        detail.setCategoryId(category.getCategoryId());
        detail.setName("내용");
        detail.setDisplayOrder(1);
        detail.setStatus(RecordStatus.ACTIVE);
        contentDetailMapper.insertContentDetail(detail);

        ContentResource first = resource(detail.getContentDetailId(), "first.pdf");
        assertThat(contentResourceMapper.insertContentResource(first)).isEqualTo(1);
        assertThat(contentResourceMapper.lockStorageAdmission()).isEqualTo(1L);
        assertThat(contentResourceMapper.softDeleteActiveById(first.getResourceId(), detail.getContentDetailId()))
                .isEqualTo(1);
        ContentResource deleted = contentResourceMapper.selectActiveByContentDetailIdAndType(
                detail.getContentDetailId(), ResourceType.SHEET);
        assertThat(deleted).isNull();

        ContentResource second = resource(detail.getContentDetailId(), "second.pdf");
        assertThat(contentResourceMapper.insertContentResource(second)).isEqualTo(1);
        ContentResource active = contentResourceMapper.selectActiveByContentDetailIdAndType(
                detail.getContentDetailId(), ResourceType.SHEET);
        assertThat(active.getResourceId()).isEqualTo(second.getResourceId());
        assertThat(active.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(active.getDeletedAt()).isNull();
        assertThat(active.getObjectKey()).doesNotContain("first.pdf");
    }

    @Test
    void rolledBackCascadeKeepsResourceActiveAndSkipsObjectDelete() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.resource.rollback." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Curriculum curriculum = new Curriculum();
        curriculum.setTeacherId(teacher.getTeacherId());
        curriculum.setName("자료");
        curriculum.setDisplayOrder(1);
        curriculum.setStatus(RecordStatus.ACTIVE);
        curriculumMapper.insertCurriculum(curriculum);

        Category category = new Category();
        category.setCurriculumId(curriculum.getCurriculumId());
        category.setName("분류");
        category.setDisplayOrder(1);
        category.setStatus(RecordStatus.ACTIVE);
        categoryMapper.insertCategory(category);

        ContentDetail detail = new ContentDetail();
        detail.setCategoryId(category.getCategoryId());
        detail.setName("내용");
        detail.setDisplayOrder(1);
        detail.setStatus(RecordStatus.ACTIVE);
        contentDetailMapper.insertContentDetail(detail);

        ContentResource resource = resource(detail.getContentDetailId(), "keep.pdf");
        assertThat(contentResourceMapper.insertContentResource(resource)).isEqualTo(1);

        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(() -> nested.executeWithoutResult(status -> {
            resourceCleanup.discardContentDetail(detail.getContentDetailId());
            resourceCleanup.discardCategory(category.getCategoryId());
            resourceCleanup.discardCurriculum(curriculum.getCurriculumId());
            throw new IllegalStateException("forced");
        })).hasMessage("forced");

        Long contentDetailId = detail.getContentDetailId();

        ContentResource active = contentResourceMapper.selectActiveByContentDetailIdAndType(
                contentDetailId, ResourceType.SHEET);
        assertThat(active).isNotNull();
        assertThat(active.getResourceId()).isEqualTo(resource.getResourceId());
        assertThat(active.getStatus()).isEqualTo(RecordStatus.ACTIVE);
        assertThat(active.getDeletedAt()).isNull();
        verify(objectStorageGateway, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private ContentResource resource(Long contentDetailId, String name) {
        ContentResource resource = new ContentResource();
        resource.setContentDetailId(contentDetailId);
        resource.setResourceType(ResourceType.SHEET);
        resource.setOriginalFileName(name);
        resource.setObjectKey("teachers/it/contents/" + contentDetailId + "/sheet/" + UUID.randomUUID() + ".pdf");
        resource.setContentType("application/pdf");
        resource.setFileSize(12L);
        resource.setStatus(RecordStatus.ACTIVE);
        return resource;
    }
}
