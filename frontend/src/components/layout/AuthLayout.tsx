import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Brand } from './Brand.tsx'

export function AuthLayout({children}: {children: ReactNode}) {
  return <main className="auth-shell">
    <section className="auth-story" aria-label="LessonPT 소개">
      <Link className="auth-brand" to="/" aria-label="LessonPT 홈"><Brand /></Link>
      <div className="auth-story-content"><p className="eyebrow">LESS TEACHING ADMIN. MORE MUSIC.</p><h2>수업의 순간을,<br/>성장의 기록으로.</h2><p className="auth-description">학생의 작은 변화부터 다음 수업의 과제까지.<br/>선생님의 수업 흐름을 한곳에서 이어가세요.</p>
        <div className="rhythm-art" aria-hidden="true"><span className="rhythm-caption">EVERY LESSON, A LITTLE FURTHER.</span><div className="rhythm-lines">{[0,1,2,3,4].map(i=><i key={i}/>)}</div><div className="rhythm-notes">{[0,1,2,3].map(i=><b key={i}/>)}</div><span className="rhythm-time">4/4</span></div>
      </div>
      <p className="auth-footnote">학생 · 수업 기록 · 커리큘럼 · 과제</p>
    </section>
    <section className="auth-form-side" aria-label="교사 계정"><div className="auth-mobile-brand"><Brand/></div>{children}<p className="auth-fineprint">수업에 집중할 수 있도록, LessonPT</p></section>
  </main>
}
