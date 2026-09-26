import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import {
  createCategory,
  deactivateCategory,
  deactivateContentDetail,
  getCurriculum,
  listCategories,
  listContentDetails,
  updateCategory,
  updateContentDetail,
  updateCurriculum,
} from '../api/curriculumApi.ts'
import { ContentDetailFormDialog } from '../components/curriculum/ContentDetailFormDialog.tsx'
import { ContentResources } from '../components/curriculum/ContentResources.tsx'
import { DeactivateDialog } from '../components/curriculum/DeactivateDialog.tsx'
import { NameFormDialog } from '../components/curriculum/NameFormDialog.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { textOrDash } from '../student/display.ts'
import type { Category, ContentDetail, ContentDetailWriteBody, Curriculum } from '../types/curriculum.ts'

import { Overlay } from '../components/layout/Overlay.tsx'
import { ResizableTable } from '../components/layout/ResizableTable.tsx'
import { ResourceButtons } from '../components/curriculum/ResourceButtons.tsx'
import { QuickContentForm } from '../components/curriculum/QuickContentForm.tsx'
const columns = [
  { key: 'order', label: '순서', width: 80 }, { key: 'name', label: '이름', width: 260, min: 160 },
  { key: 'bpm', label: '목표 BPM', width: 110 }, { key: 'sheet', label: 'PDF', width: 90 },
  { key: 'youtube', label: '영상', width: 220, min: 140 }, { key: 'audio', label: '음원', width: 90 },
  { key: 'actions', label: '작업', width: 260, min: 220 },
]
const invalidCurriculum = new ApiError(404, 'COMMON_NOT_FOUND', '', null, [])

const categoryDeactivateMessage =
  '이 카테고리를 삭제합니다. 활성 내용도 함께 삭제됩니다. 카테고리를 다시 활성화해도 내용은 자동으로 돌아오지 않습니다.'

const contentDeactivateMessage =
  '이 내용을 삭제합니다. 모니터링과 과제 기록은 남습니다.'

type NameForm =
  | { kind: 'curriculum' }
  | { kind: 'category-create' }
  | { kind: 'category-edit'; category: Category }

type ContentForm = { categoryId: number; content: ContentDetail }

export function CurriculumBuilderPage() {
  const params = useParams()
  const curriculumId = Number(params.curriculumId)
  const invalid = !Number.isInteger(curriculumId) || curriculumId <= 0
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null)
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [contentsByCategoryId, setContentsByCategoryId] = useState<Record<number, ContentDetail[]>>({})
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')
  const [quickCategory, setQuickCategory] = useState<number | null>(null)
  const [nameForm, setNameForm] = useState<NameForm | null>(null)
  const [contentForm, setContentForm] = useState<ContentForm | null>(null)
  const [resourceCard, setResourceCard] = useState<ContentForm | null>(null)
  const [formError, setFormError] = useState<unknown>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [deactivateCategoryTarget, setDeactivateCategoryTarget] = useState<Category | null>(null)
  const [deactivateContentTarget, setDeactivateContentTarget] = useState<{ categoryId: number; content: ContentDetail } | null>(null)
  const [deactivateError, setDeactivateError] = useState<unknown>(null)
  const [deactivateSubmitting, setDeactivateSubmitting] = useState(false)

  useEffect(() => {
    if (invalid) {
      return
    }
    let active = true
    setCurriculum(null)
    setCategories(null)
    setContentsByCategoryId({})
    setError(null)
    Promise.all([getCurriculum(curriculumId), listCategories(curriculumId)])
      .then(async ([nextCurriculum, nextCategories]) => {
        const lists = await Promise.all(
          nextCategories.map((category) => listContentDetails(curriculumId, category.categoryId)),
        )
        if (!active) {
          return
        }
        const contents: Record<number, ContentDetail[]> = {}
        nextCategories.forEach((category, index) => {
          contents[category.categoryId] = lists[index] ?? []
        })
        setCurriculum(nextCurriculum)
        setCategories(nextCategories)
        setContentsByCategoryId(contents)
      })
      .catch((caught) => {
        if (active) {
          setError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [curriculumId, invalid])

  async function onSaveName(name: string) {
    if (!nameForm) {
      return
    }
    setFormSubmitting(true)
    setFormError(null)
    try {
      if (nameForm.kind === 'curriculum') {
        setCurriculum(await updateCurriculum(curriculumId, name))
        setNotice('커리큘럼 이름을 수정했습니다.')
      } else if (nameForm.kind === 'category-create') {
        const created = await createCategory(curriculumId, name)
        setCategories((current) => insertByOrder(current, created, (row) => row.categoryId, (row) => row.displayOrder))
        setContentsByCategoryId((current) => ({ ...current, [created.categoryId]: [] }))
        setNotice('카테고리를 추가했습니다.')
      } else {
        const updated = await updateCategory(curriculumId, nameForm.category.categoryId, name)
        setCategories((current) => current?.map((row) => (row.categoryId === updated.categoryId ? updated : row)) ?? [updated])
        setNotice('카테고리 이름을 수정했습니다.')
      }
      setNameForm(null)
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onSaveContent(body: ContentDetailWriteBody) {
    if (!contentForm) {
      return
    }
    setFormSubmitting(true)
    setFormError(null)
    const categoryId = contentForm.categoryId
    try {
      const updated = await updateContentDetail(curriculumId, categoryId, contentForm.content.contentDetailId, body)
      setContentsByCategoryId((current) => ({
        ...current,
        [categoryId]: (current[categoryId] ?? []).map((row) =>
          row.contentDetailId === updated.contentDetailId ? updated : row,
        ),
      }))
      setNotice('내용을 수정했습니다.')
      setContentForm(null)
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  function applyContentResource(updated: ContentDetail) {
    if (!resourceCard) {
      return
    }
    const categoryId = resourceCard.categoryId
    setContentsByCategoryId((current) => ({
      ...current,
      [categoryId]: (current[categoryId] ?? []).map((row) =>
        row.contentDetailId === updated.contentDetailId ? updated : row,
      ),
    }))
    setResourceCard((current) => (current ? { ...current, content: updated } : current))
  }

  async function onDeactivateCategory() {
    if (!deactivateCategoryTarget) {
      return
    }
    const categoryId = deactivateCategoryTarget.categoryId
    setDeactivateSubmitting(true)
    setDeactivateError(null)
    try {
      await deactivateCategory(curriculumId, categoryId)
      setCategories(await listCategories(curriculumId))
      setContentsByCategoryId((current) => {
        const next = { ...current }
        delete next[categoryId]
        return next
      })
      setDeactivateCategoryTarget(null)
      setNotice('카테고리를 삭제했습니다.')
    } catch (caught) {
      setDeactivateError(caught)
    } finally {
      setDeactivateSubmitting(false)
    }
  }

  async function onDeactivateContent() {
    if (!deactivateContentTarget) {
      return
    }
    const { categoryId, content } = deactivateContentTarget
    setDeactivateSubmitting(true)
    setDeactivateError(null)
    try {
      await deactivateContentDetail(curriculumId, categoryId, content.contentDetailId)
      const rows = await listContentDetails(curriculumId, categoryId)
      setContentsByCategoryId((current) => ({ ...current, [categoryId]: rows }))
      setDeactivateContentTarget(null)
      setNotice('내용을 삭제했습니다.')
    } catch (caught) {
      setDeactivateError(caught)
    } finally {
      setDeactivateSubmitting(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header page-header-row">
        <div>
          <Link className="back-link" to="/curriculums">커리큘럼 목록</Link>
          <h1>{curriculum?.name ?? '커리큘럼'}</h1>
          <p className="lead">커리큘럼 구성</p>
        </div>
        {curriculum ? (
          <button
            type="button"
            className="button button-quiet"
            onClick={() => {
              setFormError(null)
              setNameForm({ kind: 'curriculum' })
            }}
          >
            이름 수정
          </button>
        ) : null}
      </header>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {invalid || error ? <ErrorState error={error ?? invalidCurriculum} /> : null}
      {!invalid && !error && (curriculum == null || categories == null) ? (
        <LoadingState label="커리큘럼을 불러오는 중" />
      ) : null}
      {curriculum && categories ? (
        <div className="stack">
          <div className="page-header-row">
            <h2>카테고리</h2>
            <button
              type="button"
              className="button"
              onClick={() => {
                setFormError(null)
                setNameForm({ kind: 'category-create' })
              }}
            >
              카테고리 추가
            </button>
          </div>
          {categories.length === 0 ? <EmptyState message="등록된 카테고리가 없습니다." /> : null}
          {categories.map((category) => (
            <section className="card" key={category.categoryId}>
              <header className="page-header-row">
                <h3>
                  {category.displayOrder}. {category.name}
                </h3>
                <div className="row-actions">
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setFormError(null)
                      setNameForm({ kind: 'category-edit', category })
                    }}
                  >
                    편집
                  </button>
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setDeactivateError(null)
                      setDeactivateCategoryTarget(category)
                    }}
                  >
                    카테고리 삭제
                  </button>
                  <button
                    type="button"
                    className="button"
                    onClick={() => {
                      setFormError(null)
                      setQuickCategory(current => current === category.categoryId ? null : category.categoryId)
                    }}
                  >
                    내용 추가
                  </button>
                </div>
              </header>
              {(contentsByCategoryId[category.categoryId] ?? []).length === 0 && quickCategory !== category.categoryId ? (
                <EmptyState message="등록된 내용이 없습니다." />
              ) : (
                <ResizableTable columns={columns} storageKey="builder" label={`${category.name} 내용`}>
                    {(contentsByCategoryId[category.categoryId] ?? []).map((content) => (
                      <tr key={content.contentDetailId}>
                        <td data-label="순서">{content.displayOrder}</td>
                        <td data-label="이름" className="content-name" title={content.name}>{content.name}</td>
                        <td data-label="목표 BPM" className="numeric">{textOrDash(content.targetBpm == null ? null : String(content.targetBpm))}</td>
                        {(['sheet', 'youtube', 'audio'] as const).map(kind => <td key={kind} data-label={kind === 'sheet' ? 'PDF' : kind === 'youtube' ? '영상' : '음원'}><ResourceButtons only={kind} content={content} teacher={{ curriculumId, categoryId: category.categoryId, contentDetailId: content.contentDetailId }} /></td>)}
                        <td data-label="작업"><div className="row-actions">
                          <button
                            type="button"
                            className="button button-quiet"
                            onClick={() => {
                              setFormError(null)
                              setContentForm({ categoryId: category.categoryId, content })
                            }}
                          >
                            편집
                          </button>
                          <button
                            type="button"
                            className="button button-quiet"
                            onClick={() => setResourceCard({ categoryId: category.categoryId, content })}
                          >
                            자료
                          </button>
                          <button
                            type="button"
                            className="button button-quiet"
                            onClick={() => {
                              setDeactivateError(null)
                              setDeactivateContentTarget({ categoryId: category.categoryId, content })
                            }}
                          >
                            내용 삭제
                          </button>
                        </div></td>
                      </tr>
                    ))}
                    {quickCategory === category.categoryId ? (
                      <QuickContentForm
                        curriculumId={curriculumId}
                        categoryId={category.categoryId}
                        onClose={() => setQuickCategory(null)}
                        onCreated={(created) => {
                          setContentsByCategoryId((current) => ({
                            ...current,
                            [category.categoryId]: insertByOrder(current[category.categoryId] ?? [], created, (row) => row.contentDetailId, (row) => row.displayOrder),
                          }))
                          setNotice('내용을 추가했습니다.')
                        }}
                      />
                    ) : null}
                </ResizableTable>
              )}
            </section>
          ))}
        </div>
      ) : null}
      {nameForm ? (
        <NameFormDialog
          title={nameFormTitle(nameForm)}
          initialName={nameForm.kind === 'category-edit' ? nameForm.category.name : nameForm.kind === 'curriculum' ? curriculum?.name ?? '' : ''}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setNameForm(null)
            }
          }}
          onSubmit={(name) => void onSaveName(name)}
        />
      ) : null}
      {contentForm ? (
        <ContentDetailFormDialog
          content={contentForm.content}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setContentForm(null)
            }
          }}
          onSubmit={(body) => void onSaveContent(body)}
        />
      ) : null}
      {resourceCard ? (
        <Overlay title={`${resourceCard.content.name} 자료`} onClose={() => setResourceCard(null)}>
          <div className="overlay-body">
            <ContentResources
              curriculumId={curriculumId}
              categoryId={resourceCard.categoryId}
              content={resourceCard.content}
              onContentChange={applyContentResource}
            />
          </div>
        </Overlay>
      ) : null}
      {deactivateCategoryTarget ? (
        <DeactivateDialog
          title="카테고리 삭제"
          confirmLabel="삭제"
          message={categoryDeactivateMessage}
          submitting={deactivateSubmitting}
          error={deactivateError}
          onClose={() => {
            if (!deactivateSubmitting) {
              setDeactivateCategoryTarget(null)
            }
          }}
          onConfirm={() => void onDeactivateCategory()}
        />
      ) : null}
      {deactivateContentTarget ? (
        <DeactivateDialog
          title="내용 삭제"
          confirmLabel="삭제"
          message={contentDeactivateMessage}
          submitting={deactivateSubmitting}
          error={deactivateError}
          onClose={() => {
            if (!deactivateSubmitting) {
              setDeactivateContentTarget(null)
            }
          }}
          onConfirm={() => void onDeactivateContent()}
        />
      ) : null}
    </section>
  )
}

function nameFormTitle(form: NameForm): string {
  if (form.kind === 'curriculum') {
    return '커리큘럼 수정'
  }
  if (form.kind === 'category-create') {
    return '카테고리 추가'
  }
  return '카테고리 수정'
}

function insertByOrder<T>(current: T[] | null, created: T, idOf: (row: T) => number, orderOf: (row: T) => number): T[] {
  const rows = (current ?? []).filter((row) => idOf(row) !== idOf(created))
  rows.push(created)
  rows.sort((left, right) => orderOf(left) - orderOf(right) || idOf(left) - idOf(right))
  return rows
}
