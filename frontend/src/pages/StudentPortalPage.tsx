import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { clearStudentScope, getStudentLearning, getStudentMe, isStudentScopeRequired, logoutStudent } from '../api/studentPortalApi.ts'
import { useStudentPortal } from '../auth/StudentPortalContext.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { formErrorMessage } from '../components/feedback/describeError.ts'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { formatBpm, formatDeadline, progressStatusLabel, textOrDash } from '../student/display.ts'
import type { StudentMe, StudentPortalLearning } from '../types/studentPortal.ts'
import { StudentFrame } from './StudentAccessPage.tsx'

export function StudentPortalPage() {
  const navigate = useNavigate()
  const { setStatus } = useStudentPortal()
  const [me, setMe] = useState<StudentMe | null>(null)
  const [learning, setLearning] = useState<StudentPortalLearning | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)
  const [loggingOut, setLoggingOut] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    getStudentMe()
      .then(async (nextMe) => {
        const nextLearning = await getStudentLearning()
        if (!active) {
          return
        }
        setMe(nextMe)
        setLearning(nextLearning)
        setStatus('AUTHENTICATED_SCOPED')
      })
      .catch((caught) => {
        if (!active) {
          return
        }
        if (caught instanceof ApiError && caught.status === 401) {
          setStatus('UNAUTHENTICATED')
          navigate('/student/login', { replace: true })
          return
        }
        if (isStudentScopeRequired(caught)) {
          setStatus('AUTHENTICATED_NO_SCOPE')
          navigate('/student/relationships', { replace: true })
          return
        }
        setError(caught)
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [navigate, setStatus])

  async function onLogout() {
    setLoggingOut(true)
    try {
      await logoutStudent()
    } catch {
      // 쿠키 만료 요청이 실패해도 학생 화면에서는 벗어난다.
    }
    setStatus('UNAUTHENTICATED')
    navigate('/student/login', { replace: true })
  }

  async function onOtherClass() {
    setLoggingOut(true)
    try {
      await clearStudentScope()
      setStatus('AUTHENTICATED_NO_SCOPE')
      navigate('/student/relationships', { replace: true })
    } catch (caught) {
      setLoggingOut(false)
      if (caught instanceof ApiError && caught.status === 401) {
        setStatus('UNAUTHENTICATED')
        navigate('/student/login', { replace: true })
        return
      }
      setError(caught)
    }
  }

  return (
    <StudentFrame>
      <header className="page-header-row">
        <h1>{me?.name ?? '학습 현황'}</h1>
        <button type="button" className="button button-quiet" disabled={loggingOut} onClick={() => void onOtherClass()}>
          다른 수업 보기
        </button>
        <button type="button" className="button button-quiet" disabled={loggingOut} onClick={() => void onLogout()}>
          {loggingOut ? '종료 중' : '로그아웃'}
        </button>
      </header>
      {loading ? <LoadingState label="학습 정보를 불러오는 중" /> : null}
      {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
      {learning && learning.curriculums.length === 0 ? <EmptyState message="배정된 커리큘럼이 없습니다." /> : null}
      {learning?.curriculums.map((curriculum) => (
        <article className="curriculum" key={curriculum.name}>
          <header className="curriculum-header">
            <h2>{curriculum.name}</h2>
            <ProgressValue progress={curriculum.progress} />
          </header>
          {curriculum.categories.map((category) => (
            <section key={category.name}>
              <h3>{category.name}</h3>
              {category.contents.length === 0 ? <p className="quiet">내용이 없습니다.</p> : null}
              {category.contents.map((content) => (
                <div className="monitoring" key={content.name}>
                  <h4>{content.name}</h4>
                  <p>{content.progressStatus ? progressStatusLabel(content.progressStatus) : '-'}</p>
                  <p>현재 BPM {formatBpm(content.currentBpm)}</p>
                  <p>목표 BPM {formatBpm(content.targetBpm)}</p>
                  {content.sheetUrl ? <p><a href={content.sheetUrl}>악보</a></p> : null}
                  {content.youtubeUrl ? <p><a href={content.youtubeUrl}>영상</a></p> : null}
                  {content.audioUrl ? <p><a href={content.audioUrl}>음원</a></p> : null}
                  {content.homeworks.length === 0 ? <p className="quiet">과제가 없습니다.</p> : null}
                  {content.homeworks.map((homework, index) => (
                    <div className="homework-item" key={`${content.name}-${index}`}>
                      <p>{homework.content}</p>
                      <p>{homework.completed === true ? '완료' : '미완료'}</p>
                      <p>마감 {formatDeadline(homework.deadline)}</p>
                      <p>피드백 {textOrDash(homework.feedback)}</p>
                    </div>
                  ))}
                </div>
              ))}
            </section>
          ))}
        </article>
      ))}
    </StudentFrame>
  )
}
