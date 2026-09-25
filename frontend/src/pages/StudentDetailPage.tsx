import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { listCategories, listContentDetails, listCurriculums } from '../api/curriculumApi.ts'
import { listLocations } from '../api/locationApi.ts'
import { getStudentLearning } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { StudentAccessSection } from '../components/student/StudentAccessSection.tsx'
import { StudentLocationSection } from '../components/student/StudentLocationSection.tsx'
import { StudentMonitoringPanel } from '../components/student/StudentMonitoringPanel.tsx'
import { textOrDash } from '../student/display.ts'
import type { Curriculum } from '../types/curriculum.ts'
import type { Location } from '../types/location.ts'
import type { StudentLearningDetail } from '../types/student.ts'

type Tab = 'profile' | 'learning' | 'access'

const invalidStudent = new ApiError(404, 'COMMON_NOT_FOUND', '', null, [])

export function StudentDetailPage() {
  const params = useParams()
  const studentId = Number(params.studentId)
  const invalid = !Number.isInteger(studentId) || studentId <= 0
  const [detail, setDetail] = useState<StudentLearningDetail | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [catalog, setCatalog] = useState<Location[] | null>(null)
  const [catalogError, setCatalogError] = useState<unknown>(null)
  const [curriculums, setCurriculums] = useState<Curriculum[] | null>(null)
  const [curriculumCatalogError, setCurriculumCatalogError] = useState<unknown>(null)
  const [tab, setTab] = useState<Tab>('profile')

  useEffect(() => {
    if (invalid) {
      return
    }
    let active = true
    setDetail(null)
    setError(null)
    getStudentLearning(studentId)
      .then((next) => {
        if (active) {
          setDetail(next)
        }
      })
      .catch((caught) => {
        if (active) {
          setError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [invalid, studentId])

  useEffect(() => {
    if (invalid) {
      return
    }
    let active = true
    setCatalog(null)
    setCatalogError(null)
    listLocations()
      .then((rows) => {
        if (active) {
          setCatalog(rows)
        }
      })
      .catch((caught) => {
        if (active) {
          setCatalogError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [invalid, studentId])

  useEffect(() => {
    if (invalid) {
      return
    }
    let active = true
    setCurriculums(null)
    setCurriculumCatalogError(null)
    listCurriculums()
      .then((rows) => {
        if (active) {
          setCurriculums(rows)
        }
      })
      .catch((caught) => {
        if (active) {
          setCurriculumCatalogError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [invalid, studentId])

  async function refreshLearning() {
    const next = await getStudentLearning(studentId)
    setDetail(next)
    setError(null)
  }

  return (
    <section className="page">
      <header className="page-header">
        <h1>{detail?.name ?? '학생'}</h1>
        <p className="lead">학습 현황</p>
      </header>
      <div className="tabs" role="tablist" aria-label="학생 상세">
        <button type="button" role="tab" aria-selected={tab === 'profile'} className="tab" onClick={() => setTab('profile')}>
          기본정보
        </button>
        <button type="button" role="tab" aria-selected={tab === 'learning'} className="tab" onClick={() => setTab('learning')}>
          학습관리
        </button>
        <button type="button" role="tab" aria-selected={tab === 'access'} className="tab" onClick={() => setTab('access')}>
          접근설정
        </button>
      </div>
      {invalid || error ? <ErrorState error={error ?? invalidStudent} /> : null}
      {!invalid && !error && detail == null ? <LoadingState label="학습 정보를 불러오는 중" /> : null}
      {detail && tab === 'profile' ? (
        <ProfileTab
          detail={detail}
          catalog={catalog}
          catalogError={catalogError}
          curriculums={curriculums}
          curriculumCatalogError={curriculumCatalogError}
          onRefreshLearning={refreshLearning}
        />
      ) : null}
      {detail && tab === 'learning' ? <LearningTab detail={detail} onRefreshLearning={refreshLearning} /> : null}
      {detail && tab === 'access' ? <StudentAccessSection studentId={studentId} email={detail.email} /> : null}
    </section>
  )
}

function ProfileTab({
  detail,
  catalog,
  catalogError,
  curriculums,
  curriculumCatalogError,
  onRefreshLearning,
}: {
  detail: StudentLearningDetail
  catalog: Location[] | null
  catalogError: unknown
  curriculums: Curriculum[] | null
  curriculumCatalogError: unknown
  onRefreshLearning: () => Promise<void>
}) {
  return (
    <div className="stack">
      <section className="card">
        <h2>기본정보</h2>
        <dl className="facts">
          <div>
            <dt>이름</dt>
            <dd>{detail.name}</dd>
          </div>
          <div>
            <dt>이메일</dt>
            <dd>{textOrDash(detail.email)}</dd>
          </div>
          <div>
            <dt>전화번호</dt>
            <dd>{textOrDash(detail.phone)}</dd>
          </div>
        </dl>
      </section>
      <StudentLocationSection
        studentId={detail.studentId}
        assigned={detail.locations}
        catalog={catalog}
        catalogError={catalogError}
        curriculums={curriculums}
        curriculumCatalogError={curriculumCatalogError}
        onRefreshLearning={onRefreshLearning}
      />
    </div>
  )
}

function LearningTab({
  detail,
  onRefreshLearning,
}: {
  detail: StudentLearningDetail
  onRefreshLearning: () => Promise<void>
}) {
  const curriculumIds = [
    ...new Set(detail.locations.flatMap((location) => location.studentCurriculums.map((item) => item.curriculumId))),
  ]
  const idsKey = curriculumIds.join(',')
  const [contentsByCurriculumId, setContentsByCurriculumId] = useState<Record<number, { contentDetailId: number; name: string }[]> | null>(null)
  const [catalogError, setCatalogError] = useState<unknown>(null)

  useEffect(() => {
    const curriculumIds = idsKey === '' ? [] : idsKey.split(',').map(Number)
    if (curriculumIds.length === 0) {
      setContentsByCurriculumId({})
      return
    }
    let active = true
    setContentsByCurriculumId(null)
    setCatalogError(null)
    Promise.all(
      curriculumIds.map(async (curriculumId) => {
        const categories = await listCategories(curriculumId)
        const lists = await Promise.all(categories.map((category) => listContentDetails(curriculumId, category.categoryId)))
        return [
          curriculumId,
          lists.flat().map((item) => ({ contentDetailId: item.contentDetailId, name: item.name })),
        ] as const
      }),
    )
      .then((rows) => {
        if (!active) {
          return
        }
        const next: Record<number, { contentDetailId: number; name: string }[]> = {}
        rows.forEach(([curriculumId, contents]) => {
          next[curriculumId] = contents
        })
        setContentsByCurriculumId(next)
      })
      .catch((caught) => {
        if (active) {
          setCatalogError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [idsKey])

  if (detail.locations.length === 0) {
    return <EmptyState message="배정된 출강처가 없습니다." />
  }
  return (
    <div className="stack">
      {detail.locations.map((location) => (
        <section className="card" key={location.teacherStudentLocationId}>
          <h2>{location.locationName}</h2>
          {location.studentCurriculums.length === 0 ? <EmptyState message="배정된 커리큘럼이 없습니다." /> : null}
          {location.studentCurriculums.map((curriculum) => (
            <article className="curriculum" key={curriculum.studentCurriculumId}>
              <header className="curriculum-header">
                <h3>{curriculum.curriculumName}</h3>
                <ProgressValue progress={curriculum.progress} />
              </header>
              <p className="memo">{curriculum.memo ? curriculum.memo : '메모 없음'}</p>
              <StudentMonitoringPanel
                studentCurriculumId={curriculum.studentCurriculumId}
                monitorings={curriculum.monitorings}
                contents={contentsByCurriculumId?.[curriculum.curriculumId] ?? null}
                catalogError={catalogError}
                onRefreshLearning={onRefreshLearning}
              />
            </article>
          ))}
        </section>
      ))}
    </div>
  )
}
