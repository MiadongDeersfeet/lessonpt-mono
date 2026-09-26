import { useEffect, useState } from 'react'
import { fetchStudentResourceBlob, studentResourceContentUrl } from '../../api/resourceApi.ts'
import { formatFileSize } from '../../resource/fileSize.ts'
import { youtubeEmbedUrl } from '../../resource/youtube.ts'
import type { StudentPortalContent } from '../../types/studentPortal.ts'

export function StudentLessonResources({ content }: { content: StudentPortalContent }) {
  const embedUrl = youtubeEmbedUrl(content.youtubeUrl)
  return (
    <div className="resource-panel">
      {content.sheet ? (
        <section aria-label="악보">
          <p>{content.sheet.originalFileName}</p>
          <p>{formatFileSize(content.sheet.fileSize)}</p>
          <StudentPdfPreview
            key={content.sheet.resourceId}
            resourceId={content.sheet.resourceId}
            fileName={content.sheet.originalFileName}
          />
          <p>
            <a href={studentResourceContentUrl(content.sheet.resourceId, 'attachment')}>악보 다운로드</a>
          </p>
        </section>
      ) : null}
      {content.audio ? (
        <section aria-label="음원">
          <p>{content.audio.originalFileName}</p>
          <audio controls src={studentResourceContentUrl(content.audio.resourceId, 'inline')} />
          <p>
            <a href={studentResourceContentUrl(content.audio.resourceId, 'attachment')}>음원 다운로드</a>
          </p>
        </section>
      ) : null}
      {embedUrl ? (
        <iframe
          className="resource-preview"
          title="수업 영상"
          src={embedUrl}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowFullScreen
          referrerPolicy="strict-origin-when-cross-origin"
        />
      ) : null}
    </div>
  )
}

function StudentPdfPreview({ resourceId, fileName }: { resourceId: number; fileName: string }) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl = ''
    void fetchStudentResourceBlob(resourceId, 'inline')
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        if (active) {
          setPreviewUrl(objectUrl)
        } else {
          URL.revokeObjectURL(objectUrl)
        }
      })
      .catch(() => {
        if (active) {
          setFailed(true)
        }
      })
    return () => {
      active = false
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [resourceId])

  if (failed) {
    return (
      <p className="form-error" role="alert">
        악보를 불러오지 못했습니다.
      </p>
    )
  }
  if (previewUrl == null) {
    return <p role="status">악보를 불러오는 중</p>
  }
  return <iframe className="resource-preview" title={`${fileName} 보기`} src={previewUrl} />
}
