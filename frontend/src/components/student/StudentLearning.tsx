import { useId, useState } from 'react'
import { formatBpm, formatDeadline, progressStatusLabel, textOrDash } from '../../student/display.ts'
import type { StudentPortalCategory, StudentPortalContent, StudentPortalCurriculum, StudentPortalHomework, StudentPortalLearning } from '../../types/studentPortal.ts'
import { ProgressValue } from './ProgressValue.tsx'
import { StudentLessonResources } from './StudentLessonResources.tsx'

export function StudentLearning({ learning }: { learning: StudentPortalLearning }) {
  return (
    <>
      {learning.curriculums.map((curriculum) => (
        <StudentCurriculum key={curriculum.name} curriculum={curriculum} />
      ))}
    </>
  )
}

function StudentCurriculum({ curriculum }: { curriculum: StudentPortalCurriculum }) {
  const published = curriculum.categories.reduce((sum, category) => sum + category.contents.length, 0)
  return (
    <article className="curriculum learning-curriculum">
      <header className="curriculum-header">
        <h2>{curriculum.name}</h2>
        <div className="learning-stats">
          <p>전체 진행률 <ProgressValue progress={curriculum.progress} /></p>
          <p>현재 공개 {published}개</p>
          {curriculum.progress ? <p>완료 {curriculum.progress.completedCount}개</p> : null}
        </div>
      </header>
      {curriculum.categories.map((category) => (
        <StudentCategorySection key={category.name} curriculumName={curriculum.name} category={category} />
      ))}
    </article>
  )
}

function StudentCategorySection({ curriculumName, category }: { curriculumName: string; category: StudentPortalCategory }) {
  const storageId = `${curriculumName}\u0000${category.name}`
  const [open, setOpen] = useState(() => readCategoryOpen(storageId))
  const panelId = useId()
  const published = category.contents.length
  const locked = Math.max(0, category.totalContentCount - published)
  return (
    <section className="learning-category">
      <h3>
        <button
          type="button"
          className="learning-category-toggle"
          aria-expanded={open}
          aria-controls={panelId}
          onClick={() => setOpen((current) => {
            const next = !current
            writeCategoryOpen(storageId, next)
            return next
          })}
        >
          <span className="learning-category-name">
            <span aria-hidden="true">{open ? '▼' : '▶'}</span> {category.name}
          </span>
          <span className="learning-category-count">{published} / {category.totalContentCount} 공개</span>
        </button>
      </h3>
      {open ? (
        <div className="learning-panel" id={panelId}>
          {category.contents.map((content) => (
            <StudentLearningItem key={content.monitoringId} content={content} />
          ))}
          <LockedLessons count={locked} />
        </div>
      ) : null}
    </section>
  )
}

export function StudentLearningItem({ content }: { content: StudentPortalContent }) {
  return (
    <article className="learning-item">
      <h4>{content.name}</h4>
      {content.progressStatus ? (
        <p>
          <span className={`status-label status-${content.progressStatus}`}>{progressStatusLabel(content.progressStatus)}</span>
        </p>
      ) : null}
      <p className="learning-bpm">현재 BPM {formatBpm(content.currentBpm)} / 목표 BPM {formatBpm(content.targetBpm)}</p>
      <StudentLessonResources content={content} />
      <StudentHomeworkList homeworks={content.homeworks} />
    </article>
  )
}

const categoryOpenKey = 'lessonpt.ui.student-categories.v1'

function readCategoryOpen(id: string): boolean {
  try {
    const saved = JSON.parse(localStorage.getItem(categoryOpenKey) ?? '{}') as unknown
    if (saved && typeof saved === 'object' && typeof (saved as Record<string, unknown>)[id] === 'boolean') {
      return (saved as Record<string, boolean>)[id]
    }
  } catch {
    // A broken preference falls back to open.
  }
  return true
}

function writeCategoryOpen(id: string, open: boolean) {
  try {
    const saved = JSON.parse(localStorage.getItem(categoryOpenKey) ?? '{}') as unknown
    const next = saved && typeof saved === 'object' ? { ...(saved as Record<string, boolean>), [id]: open } : { [id]: open }
    localStorage.setItem(categoryOpenKey, JSON.stringify(next))
  } catch {
    // The toggle still works when storage is unavailable.
  }
}

function LockedLessons({ count }: { count: number }) {
  if (count <= 0) {
    return null
  }
  if (count >= 4) {
    return <p className="learning-locked">아직 공개되지 않은 학습 내용 {count}개</p>
  }
  return (
    <>
      {count >= 1 ? <p className="learning-locked">다음 학습 내용</p> : null}
      {count >= 2 ? <p className="learning-locked">다음 학습 내용</p> : null}
      {count >= 3 ? <p className="learning-locked">다음 학습 내용</p> : null}
    </>
  )
}

function StudentHomeworkList({ homeworks }: { homeworks: StudentPortalHomework[] }) {
  if (homeworks.length === 0) {
    return null
  }
  return (
    <section className="learning-homework" aria-label="과제">
      <h5>과제</h5>
      <ul>
        {homeworks.map((homework) => (
          <li key={homework.homeworkId}>
            <p>{homework.content}</p>
            {homework.completed === true ? <p>완료</p> : null}
            {homework.completed === false ? <p>미완료</p> : null}
            <p>기한 {formatDeadline(homework.deadline)}</p>
            {homework.feedback ? <p>피드백 {textOrDash(homework.feedback)}</p> : null}
          </li>
        ))}
      </ul>
    </section>
  )
}
