import { useEffect, useState } from 'react'
import { fetchStudentResourceBlob, studentResourceContentUrl } from '../../api/resourceApi.ts'
import { youtubeEmbedUrl } from '../../resource/youtube.ts'
import type { ContentResource } from '../../types/curriculum.ts'
import type { StudentPortalContent } from '../../types/studentPortal.ts'
import { Overlay } from '../layout/Overlay.tsx'

export function StudentLessonResources({ content }: { content: StudentPortalContent }) {
  const embed = youtubeEmbedUrl(content.youtubeUrl)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [videoOpen, setVideoOpen] = useState(false)

  return (
    <div className="student-resources">
      {content.sheet ? (
        <button type="button" className="resource-button" aria-label="악보 보기" onClick={() => setSheetOpen(true)}>
          악보
        </button>
      ) : null}
      {content.audio ? (
        <section className="student-audio" aria-label="음원">
          <audio
            controls
            src={studentResourceContentUrl(content.audio.resourceId, 'inline')}
            aria-label={content.audio.originalFileName}
          />
          <a className="button button-quiet" href={studentResourceContentUrl(content.audio.resourceId, 'attachment')}>
            음원 다운로드
          </a>
        </section>
      ) : null}
      {embed ? (
        <div className="student-video">
          <button
            type="button"
            className="resource-button"
            aria-expanded={videoOpen}
            aria-label="영상 보기"
            onClick={() => setVideoOpen((open) => !open)}
          >
            영상
          </button>
          {videoOpen ? (
            <iframe
              className="video-player"
              title="수업 영상"
              src={embed}
              allow="accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              referrerPolicy="strict-origin-when-cross-origin"
            />
          ) : null}
        </div>
      ) : null}
      {sheetOpen && content.sheet ? (
        <Overlay
          title={`${content.name} · 악보`}
          className="resource-overlay resource-overlay-sheet student-sheet-overlay"
          onClose={() => setSheetOpen(false)}
        >
          <div className="overlay-body">
            <StudentPdfPreview key={content.sheet.resourceId} resource={content.sheet} />
          </div>
        </Overlay>
      ) : null}
    </div>
  )
}

function StudentPdfPreview({ resource }: { resource: ContentResource }) {
  const [url, setUrl] = useState<string | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl = ''
    fetchStudentResourceBlob(resource.resourceId, 'inline')
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        if (active) {
          setUrl(objectUrl)
        } else {
          URL.revokeObjectURL(objectUrl)
        }
      })
      .catch(() => {
        if (active) {
          setError(true)
        }
      })
    return () => {
      active = false
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [resource.resourceId])

  return (
    <section aria-label="악보">
      <p className="file-name">{resource.originalFileName}</p>
      {error ? <p role="alert" className="form-error">악보를 불러오지 못했습니다.</p> : null}
      {!error && !url ? <p role="status">악보를 불러오는 중</p> : null}
      {url ? <iframe className="pdf-player" title={`${resource.originalFileName} 보기`} src={url} /> : null}
      <p>
        <a className="button button-quiet" href={studentResourceContentUrl(resource.resourceId, 'attachment')}>
          악보 다운로드
        </a>
      </p>
    </section>
  )
}
