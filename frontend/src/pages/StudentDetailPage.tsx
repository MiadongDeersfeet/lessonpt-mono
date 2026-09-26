import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { listCategories, listContentDetails, listCurriculums } from '../api/curriculumApi.ts'
import { listLocations } from '../api/locationApi.ts'
import { getStudentLearning, releaseStudent, updateStudent } from '../api/studentApi.ts'
import { StudentFormDialog } from '../components/student/StudentFormDialog.tsx'
import { ReleaseStudentDialog } from '../components/student/ReleaseStudentDialog.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { MoreMenu } from '../components/layout/MoreMenu.tsx'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { StudentAccessSection } from '../components/student/StudentAccessSection.tsx'
import { StudentLocationSection } from '../components/student/StudentLocationSection.tsx'
import { StudentMonitoringPanel } from '../components/student/StudentMonitoringPanel.tsx'
import type { ContentDetail, Curriculum } from '../types/curriculum.ts'
import type { Location } from '../types/location.ts'
import type { StudentLearningDetail, StudentUpdateBody } from '../types/student.ts'

type Tab = 'profile' | 'learning' | 'access'

const invalidStudent = new ApiError(404, 'COMMON_NOT_FOUND', '', null, [])

export function StudentDetailPage() {
  const navigate = useNavigate()
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
  const [editingProfile, setEditingProfile] = useState(false)
  const [profileError, setProfileError] = useState<unknown>(null)
  const [profileSubmitting, setProfileSubmitting] = useState(false)
  const [deletingStudent, setDeletingStudent] = useState(false)
  const [deleteStudentError, setDeleteStudentError] = useState<unknown>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

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

  async function onDeleteStudent() {
    setDeletingStudent(true)
    setDeleteStudentError(null)
    try {
      await releaseStudent(studentId)
      navigate('/students')
    } catch (caught) {
      setDeleteStudentError(caught)
    } finally {
      setDeletingStudent(false)
    }
  }

  async function onUpdateProfile(id: number, body: StudentUpdateBody) {
    setProfileSubmitting(true)
    setProfileError(null)
    try {
      await updateStudent(id, body)
      await refreshLearning()
      setEditingProfile(false)
    } catch (caught) {
      setProfileError(caught)
    } finally {
      setProfileSubmitting(false)
    }
  }

  return (
    <section className="page detail-workspace">
      <header className="page-header detail-header">
        <div>
          <Link className="back-link" to="/students">학생 목록으로</Link>
          <h1>{detail?.name ?? '학생'}</h1>
          {detail ? <p className="detail-identity">{identityLine(detail)}</p> : null}
        </div>
        {detail ? (
          <div className="row-actions">
            <button
              type="button"
              className="button detail-secondary"
              onClick={() => {
                setProfileError(null)
                setEditingProfile(true)
              }}
            >
              학생 편집
            </button>
            <MoreMenu
              label="학생 작업"
              items={[{
                label: '학생 삭제',
                danger: true,
                onSelect: () => {
                  setDeleteStudentError(null)
                  setConfirmDelete(true)
                },
              }]}
            />
          </div>
        ) : null}
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
          onOpenLearning={() => setTab('learning')}
        />
      ) : null}
      {detail && tab === 'learning' ? <LearningTab detail={detail} onRefreshLearning={refreshLearning} /> : null}
      {detail && tab === 'access' ? <StudentAccessSection studentId={studentId} email={detail.email} /> : null}
      {detail && editingProfile ? (
        <StudentFormDialog
          mode="edit"
          student={{
            studentId: detail.studentId,
            email: detail.email,
            name: detail.name,
            phone: detail.phone,
            teacherStudentId: 0,
          }}
          submitting={profileSubmitting}
          error={profileError}
          onClose={() => {
            if (!profileSubmitting) {
              setEditingProfile(false)
            }
          }}
          onCreate={() => undefined}
          onUpdate={(id, body) => void onUpdateProfile(id, body)}
        />
      ) : null}
      {detail && confirmDelete ? (
        <ReleaseStudentDialog
          name={detail.name}
          submitting={deletingStudent}
          error={deleteStudentError}
          onClose={() => {
            if (!deletingStudent) {
              setConfirmDelete(false)
            }
          }}
          onConfirm={() => void onDeleteStudent()}
        />
      ) : null}
    </section>
  )
}

function identityLine(detail: StudentLearningDetail): string {
  const email = detail.email && detail.email.trim() !== '' ? detail.email : '이메일 미등록'
  const phone = detail.phone && detail.phone.trim() !== '' ? detail.phone : '전화번호 미등록'
  return `${email} · ${phone}`
}

function ProfileTab({
  detail,
  catalog,
  catalogError,
  curriculums,
  curriculumCatalogError,
  onRefreshLearning,
  onOpenLearning,
}: {
  detail: StudentLearningDetail
  catalog: Location[] | null
  catalogError: unknown
  curriculums: Curriculum[] | null
  curriculumCatalogError: unknown
  onRefreshLearning: () => Promise<void>
  onOpenLearning: () => void
}) {
  return (
    <StudentLocationSection
      studentId={detail.studentId}
      assigned={detail.locations}
      catalog={catalog}
      catalogError={catalogError}
      curriculums={curriculums}
      curriculumCatalogError={curriculumCatalogError}
      onRefreshLearning={onRefreshLearning}
      onOpenLearning={onOpenLearning}
    />
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
  const [contentsByCurriculumId, setContentsByCurriculumId] = useState<Record<number, (ContentDetail & { categoryId: number; categoryName: string })[]> | null>(null)
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
          lists.flatMap((items, index) => items.map(item => ({ ...item, categoryId: categories[index].categoryId, categoryName: categories[index].name }))),
        ] as const
      }),
    )
      .then((rows) => {
        if (!active) {
          return
        }
        const next: Record<number, (ContentDetail & { categoryId: number; categoryName: string })[]> = {}
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
    <div className="lesson-workspace">
      {detail.locations.map((location) => (
        <section className="lesson-block" key={location.teacherStudentLocationId}>
          <h2>{location.locationName}</h2>
          {location.studentCurriculums.length === 0 ? <EmptyState message="배정된 커리큘럼이 없습니다." /> : null}
          {location.studentCurriculums.map((curriculum) => (
            <article className="curriculum" key={curriculum.studentCurriculumId}>
              <header className="curriculum-header">
                <h3>{curriculum.curriculumName}</h3>
                <ProgressValue progress={curriculum.progress} meter />
              </header>
              <p className="memo">{curriculum.memo ? curriculum.memo : '메모 없음'}</p>
              <StudentMonitoringPanel
                curriculumId={curriculum.curriculumId}
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
