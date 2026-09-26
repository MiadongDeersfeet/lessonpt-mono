import { useEffect, useState } from 'react'
import { fetchStudentResourceBlob, fetchTeacherResourceBlob, studentResourceContentUrl } from '../../api/resourceApi.ts'
import { youtubeEmbedUrl } from '../../resource/youtube.ts'
import type { ContentResource } from '../../types/curriculum.ts'
import { Overlay } from '../layout/Overlay.tsx'

type Kind = 'sheet' | 'youtube' | 'audio'
export type ResourceContent = { name: string; sheet: ContentResource | null; audio: ContentResource | null; youtubeUrl: string | null }
export type TeacherResourceScope = { curriculumId: number; categoryId: number; contentDetailId: number }
const names = { sheet: '악보', youtube: '영상', audio: '음원' }

export function ResourceButtons({ content, teacher, only, blankWhenEmpty = false }: { content: ResourceContent; teacher?: TeacherResourceScope; only?: Kind; blankWhenEmpty?: boolean }) {
  const [selected, setSelected] = useState<Kind | null>(null)
  const embed = youtubeEmbedUrl(content.youtubeUrl)
  const available = { sheet: Boolean(content.sheet), youtube: Boolean(embed), audio: Boolean(content.audio) }
  const resource = selected === 'sheet' ? content.sheet : selected === 'audio' ? content.audio : null
  const kinds = (only ? [only] : ['sheet', 'youtube', 'audio'] as Kind[]).filter((kind) => available[kind])
  if (kinds.length === 0) {
    return only || blankWhenEmpty ? null : <p className="quiet">자료 없음</p>
  }
  return <div className="resource-buttons">
    {kinds.map(kind =>
      <button key={kind} type="button" className="resource-button" aria-label={`${names[kind]} 보기`} title={`${content.name} · ${names[kind]} 보기`} onClick={() => setSelected(kind)}><ResourceIcon kind={kind} /><span>{names[kind]}</span></button>)}
    {selected && available[selected] ? <Overlay title={`${content.name} · ${names[selected]}`} className={`resource-overlay resource-overlay-${selected}`} onClose={() => setSelected(null)}>
      <div className="overlay-body">
        {selected === 'youtube' ? <iframe className="video-player" title="수업 영상" src={embed!} allow="accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" /> : resource ? <FileViewer key={`${selected}-${resource.resourceId}`} resource={resource} kind={selected} teacher={teacher} /> : null}
      </div>
    </Overlay> : null}
  </div>
}

function FileViewer({ resource, kind, teacher }: { resource: ContentResource; kind: 'sheet' | 'audio'; teacher?: TeacherResourceScope }) {
  const directAudio = kind === 'audio' && !teacher
  const [url, setUrl] = useState<string | null>(directAudio ? studentResourceContentUrl(resource.resourceId, 'inline') : null)
  const [error, setError] = useState('')
  const [downloading, setDownloading] = useState(false)
  const curriculumId = teacher?.curriculumId, categoryId = teacher?.categoryId, contentDetailId = teacher?.contentDetailId
  useEffect(() => {
    if (directAudio) return
    let active = true, objectUrl = ''
    const request = curriculumId != null && categoryId != null && contentDetailId != null
      ? fetchTeacherResourceBlob(curriculumId, categoryId, contentDetailId, resource.resourceId, 'inline')
      : fetchStudentResourceBlob(resource.resourceId, 'inline')
    void request.then(blob => { objectUrl = URL.createObjectURL(blob); if (active) setUrl(objectUrl); else URL.revokeObjectURL(objectUrl) }).catch(() => { if (active) setError(`${names[kind]}를 불러오지 못했습니다.`) })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [resource.resourceId, kind, directAudio, curriculumId, categoryId, contentDetailId])
  async function download() {
    if (!teacher) return
    setDownloading(true)
    try {
      const blob = await fetchTeacherResourceBlob(teacher.curriculumId, teacher.categoryId, teacher.contentDetailId, resource.resourceId, 'attachment')
      const address = URL.createObjectURL(blob)
      const link = document.createElement('a'); link.href = address; link.download = resource.originalFileName; link.click()
      setTimeout(() => URL.revokeObjectURL(address), 1000)
    } catch { setError('다운로드하지 못했습니다. 다시 시도해 주세요.') } finally { setDownloading(false) }
  }
  return <section aria-label={names[kind]}>
    <p className="file-name">{resource.originalFileName}</p>
    {error ? <p role="alert" className="form-error">{error}</p> : !url ? <p role="status">{names[kind]}를 불러오는 중</p> : null}
    {url && (kind === 'sheet' ? <iframe className="pdf-player" title={`${resource.originalFileName} 보기`} src={url} /> : <audio controls src={url} aria-label={resource.originalFileName} />)}
    <p>{teacher ? <button type="button" className="button button-quiet" disabled={downloading} onClick={() => void download()}>{downloading ? '다운로드 중' : `${names[kind]} 다운로드`}</button> : <a className="button button-quiet" href={studentResourceContentUrl(resource.resourceId, 'attachment')}>{names[kind]} 다운로드</a>}</p>
  </section>
}

function ResourceIcon({ kind }: { kind: Kind }) {
  return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">{kind === 'sheet' ? <><path d="M6 3h8l4 4v14H6zM14 3v5h4M9 12h6M9 16h6" /></> : kind === 'youtube' ? <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="m10 9 5 3-5 3z" /></> : <><path d="M10 17V5l9-2v12M10 8l9-2" /><ellipse cx="7" cy="18" rx="3" ry="2" /><ellipse cx="16" cy="16" rx="3" ry="2" /></>}</svg>
}
