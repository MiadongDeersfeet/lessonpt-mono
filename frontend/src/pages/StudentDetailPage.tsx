import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { listLocations } from '../api/locationApi.ts'
import { getStudentLearning } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { StudentLocationSection } from '../components/student/StudentLocationSection.tsx'
import { formatBpm, formatDeadline, progressStatusLabel, textOrDash } from '../student/display.ts'
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
          onRefreshLearning={refreshLearning}
        />
      ) : null}
      {detail && tab === 'learning' ? <LearningTab detail={detail} /> : null}
      {detail && tab === 'access' ? <p className="lead">접근 설정은 다음 단계에서 연결합니다.</p> : null}
    </section>
  )
}

function ProfileTab({
  detail,
  catalog,
  catalogError,
  onRefreshLearning,
}: {
  detail: StudentLearningDetail
  catalog: Location[] | null
  catalogError: unknown
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
        onRefreshLearning={onRefreshLearning}
      />
    </div>
  )
}

function LearningTab({ detail }: { detail: StudentLearningDetail }) {
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
              {curriculum.monitorings.length === 0 ? <EmptyState message="아직 등록된 학습 기록이 없습니다." /> : null}
              {curriculum.monitorings.map((monitoring) => (
                <div className="monitoring" key={monitoring.monitoringId}>
                  <h4>{monitoring.contentDetailName}</h4>
                  <dl className="facts facts-compact">
                    <div>
                      <dt>목표 BPM</dt>
                      <dd>{formatBpm(monitoring.targetBpm)}</dd>
                    </div>
                    <div>
                      <dt>현재 BPM</dt>
                      <dd>{formatBpm(monitoring.currentBpm)}</dd>
                    </div>
                    <div>
                      <dt>상태</dt>
                      <dd>{progressStatusLabel(monitoring.progressStatus)}</dd>
                    </div>
                  </dl>
                  <p className="memo">{monitoring.memo ? monitoring.memo : '메모 없음'}</p>
                  {monitoring.homeworks.length === 0 ? (
                    <p className="quiet">과제가 없습니다.</p>
                  ) : (
                    <ul className="homework-list">
                      {monitoring.homeworks.map((homework) => (
                        <li key={homework.homeworkId}>
                          <p>{homework.homeworkContent}</p>
                          <p className="quiet">
                            마감 {formatDeadline(homework.deadline)} · {homework.completed ? '완료' : '미완료'}
                          </p>
                          <p className="quiet">피드백 {textOrDash(homework.feedback)}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </article>
          ))}
        </section>
      ))}
    </div>
  )
}
