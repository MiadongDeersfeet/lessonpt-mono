import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import {
  createCategory,
  createContentDetail,
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
import { DeactivateDialog } from '../components/curriculum/DeactivateDialog.tsx'
import { NameFormDialog } from '../components/curriculum/NameFormDialog.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { textOrDash } from '../student/display.ts'
import type { Category, ContentDetail, ContentDetailWriteBody, Curriculum } from '../types/curriculum.ts'

const invalidCurriculum = new ApiError(404, 'COMMON_NOT_FOUND', '', null, [])

const categoryDeactivateMessage =
  '이 카테고리를 비활성화합니다. 활성 내용도 함께 비활성화됩니다. 카테고리를 다시 활성화해도 내용은 자동으로 돌아오지 않습니다.'

const contentDeactivateMessage =
  '이 내용을 비활성화합니다. 모니터링과 과제 기록은 남습니다.'

type NameForm =
  | { kind: 'curriculum' }
  | { kind: 'category-create' }
  | { kind: 'category-edit'; category: Category }

type ContentForm = { categoryId: number; content: ContentDetail | null }

export function CurriculumBuilderPage() {
  const params = useParams()
  const curriculumId = Number(params.curriculumId)
  const invalid = !Number.isInteger(curriculumId) || curriculumId <= 0
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null)
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [contentsByCategoryId, setContentsByCategoryId] = useState<Record<number, ContentDetail[]>>({})
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')
  const [nameForm, setNameForm] = useState<NameForm | null>(null)
  const [contentForm, setContentForm] = useState<ContentForm | null>(null)
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
      if (contentForm.content) {
        const updated = await updateContentDetail(curriculumId, categoryId, contentForm.content.contentDetailId, body)
        setContentsByCategoryId((current) => ({
          ...current,
          [categoryId]: (current[categoryId] ?? []).map((row) =>
            row.contentDetailId === updated.contentDetailId ? updated : row,
          ),
        }))
        setNotice('내용을 수정했습니다.')
      } else {
        const created = await createContentDetail(curriculumId, categoryId, body)
        setContentsByCategoryId((current) => ({
          ...current,
          [categoryId]: insertByOrder(current[categoryId] ?? [], created, (row) => row.contentDetailId, (row) => row.displayOrder),
        }))
        setNotice('내용을 추가했습니다.')
      }
      setContentForm(null)
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  function applyContentResource(updated: ContentDetail) {
    if (!contentForm) {
      return
    }
    const categoryId = contentForm.categoryId
    setContentsByCategoryId((current) => ({
      ...current,
      [categoryId]: (current[categoryId] ?? []).map((row) =>
        row.contentDetailId === updated.contentDetailId ? updated : row,
      ),
    }))
    setContentForm((current) => (current ? { ...current, content: updated } : current))
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
      setNotice('카테고리를 비활성화했습니다.')
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
      setNotice('내용을 비활성화했습니다.')
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
                    비활성화
                  </button>
                  <button
                    type="button"
                    className="button"
                    onClick={() => {
                      setFormError(null)
                      setContentForm({ categoryId: category.categoryId, content: null })
                    }}
                  >
                    내용 추가
                  </button>
                </div>
              </header>
              {(contentsByCategoryId[category.categoryId] ?? []).length === 0 ? (
                <EmptyState message="등록된 내용이 없습니다." />
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>순서</th>
                      <th>이름</th>
                      <th>목표 BPM</th>
                      <th>작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(contentsByCategoryId[category.categoryId] ?? []).map((content) => (
                      <tr key={content.contentDetailId}>
                        <td>{content.displayOrder}</td>
                        <td>{content.name}</td>
                        <td>{textOrDash(content.targetBpm == null ? null : String(content.targetBpm))}</td>
                        <td className="row-actions">
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
                            onClick={() => {
                              setDeactivateError(null)
                              setDeactivateContentTarget({ categoryId: category.categoryId, content })
                            }}
                          >
                            비활성화
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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
          curriculumId={curriculumId}
          categoryId={contentForm.categoryId}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setContentForm(null)
            }
          }}
          onSubmit={(body) => void onSaveContent(body)}
          onContentChange={applyContentResource}
        />
      ) : null}
      {deactivateCategoryTarget ? (
        <DeactivateDialog
          title="카테고리 비활성화"
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
          title="내용 비활성화"
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
