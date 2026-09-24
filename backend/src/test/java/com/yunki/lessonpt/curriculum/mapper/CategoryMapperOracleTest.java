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
import com.yunki.lessonpt.curriculum.domain.Curriculum;
import com.yunki.lessonpt.teacher.domain.Teacher;
import com.yunki.lessonpt.teacher.mapper.TeacherMapper;

/**
 * 임시 강사, 커리큘럼, 카테고리를 만든 뒤 롤백한다.
 * sequence 숫자는 확인하지 않는다.
 */
@SpringBootTest(properties = "lessonpt.jwt.secret=01234567890123456789012345678901")
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LESSONPT_DB_URL", matches = ".+")
class CategoryMapperOracleTest {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CurriculumMapper curriculumMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void insertsUpdatesDeletesAndRestoresInsideOneCurriculum() {
        Teacher teacher = new Teacher();
        teacher.setEmail("it.category." + UUID.randomUUID() + "@lessonpt.local");
        teacher.setPasswordHash("hash");
        teacher.setName("임시강사");
        teacher.setRole("TEACHER");
        teacher.setStatus(RecordStatus.ACTIVE);
        teacherMapper.insertTeacher(teacher);

        Curriculum curriculum = curriculum(teacher.getTeacherId(), "기초", 1);
        Curriculum other = curriculum(teacher.getTeacherId(), "다른과정", 2);
        curriculumMapper.insertCurriculum(curriculum);
        curriculumMapper.insertCurriculum(other);

        Category first = category(curriculum.getCurriculumId(), "준비", 1);
        Category second = category(curriculum.getCurriculumId(), "연주", 2);
        Category untouched = category(other.getCurriculumId(), "유지", 1);
        categoryMapper.insertCategory(first);
        categoryMapper.insertCategory(second);
        categoryMapper.insertCategory(untouched);

        assertThat(first.getCategoryId()).isNotNull().isPositive();
        assertThat(categoryMapper.selectCategoryById(first.getCategoryId()).getName()).isEqualTo("준비");
        assertThat(categoryMapper.selectActiveCategoriesByCurriculumId(curriculum.getCurriculumId()))
                .extracting(Category::getDisplayOrder)
                .containsExactly(1, 2);
        assertThat(categoryMapper.selectMaxDisplayOrderByCurriculumId(curriculum.getCurriculumId())).isEqualTo(2);

        first.setName("준비운동");
        assertThat(categoryMapper.updateCategory(first)).isEqualTo(1);
        assertThat(categoryMapper.selectActiveCategoryByIdAndCurriculumId(first.getCategoryId(), curriculum.getCurriculumId())
                .getName()).isEqualTo("준비운동");

        assertThat(categoryMapper.softDeleteCategory(first.getCategoryId(), curriculum.getCurriculumId())).isEqualTo(1);
        assertThat(categoryMapper.shiftActiveDisplayOrdersDown(curriculum.getCurriculumId(), 1)).isEqualTo(1);
        assertThat(categoryMapper.selectActiveCategoryById(first.getCategoryId())).isNull();
        assertThat(categoryMapper.selectActiveCategoryById(second.getCategoryId()).getDisplayOrder()).isEqualTo(1);
        assertThat(categoryMapper.selectActiveCategoryById(untouched.getCategoryId()).getDisplayOrder()).isEqualTo(1);

        first.setDisplayOrder(2);
        assertThat(categoryMapper.restoreCategory(first)).isEqualTo(1);
        assertThat(categoryMapper.selectActiveCategoriesByCurriculumId(curriculum.getCurriculumId()))
                .extracting(Category::getName, Category::getDisplayOrder)
                .containsExactly(tuple("연주", 1), tuple("준비운동", 2));
        assertThat(categoryMapper.selectActiveCategoriesByCurriculumId(other.getCurriculumId()))
                .extracting(Category::getName, Category::getDisplayOrder)
                .containsExactly(tuple("유지", 1));
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
}
