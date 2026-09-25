import { useEffect, useState } from 'react'
import { createLocation, deactivateLocation, listLocations, updateLocation } from '../api/locationApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { LocationFormDialog } from '../components/location/LocationFormDialog.tsx'
import { ReleaseLocationDialog } from '../components/location/ReleaseLocationDialog.tsx'
import { textOrDash } from '../student/display.ts'
import type { Location, LocationWriteBody } from '../types/location.ts'

type FormState = { mode: 'create'; location: null } | { mode: 'edit'; location: Location }

export function LocationListPage() {
  const [locations, setLocations] = useState<Location[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState<FormState | null>(null)
  const [formError, setFormError] = useState<unknown>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<Location | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releaseSubmitting, setReleaseSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    listLocations()
      .then((rows) => {
        if (active) {
          setLocations(rows)
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
  }, [])

  async function onCreate(body: LocationWriteBody) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const created = await createLocation(body)
      setLocations((current) => insertLocation(current, created))
      setForm(null)
      setNotice('출강처를 추가했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onUpdate(locationId: number, body: LocationWriteBody) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const updated = await updateLocation(locationId, body)
      setLocations((current) => current?.map((row) => (row.locationId === locationId ? updated : row)) ?? [updated])
      setForm(null)
      setNotice('출강처를 수정했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onDeactivate() {
    if (!releaseTarget) {
      return
    }
    setReleaseSubmitting(true)
    setReleaseError(null)
    try {
      await deactivateLocation(releaseTarget.locationId)
      const rows = await listLocations()
      setLocations(rows)
      setReleaseTarget(null)
      setNotice('출강처를 비활성화했습니다.')
    } catch (caught) {
      setReleaseError(caught)
    } finally {
      setReleaseSubmitting(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header page-header-row">
        <h1>출강처</h1>
        <button
          type="button"
          className="button"
          onClick={() => {
            setFormError(null)
            setForm({ mode: 'create', location: null })
          }}
        >
          출강처 추가
        </button>
      </header>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {error ? <ErrorState error={error} /> : null}
      {!error && locations == null ? <LoadingState label="출강처 목록을 불러오는 중" /> : null}
      {locations?.length === 0 ? <EmptyState message="등록된 출강처가 없습니다." /> : null}
      {locations && locations.length > 0 ? (
        <table className="data-table">
          <thead>
            <tr>
              <th>순서</th>
              <th>이름</th>
              <th>주소</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {locations.map((location) => (
              <tr key={location.locationId}>
                <td>{location.displayOrder}</td>
                <td>{location.name}</td>
                <td>{textOrDash(location.address)}</td>
                <td className="row-actions">
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setFormError(null)
                      setForm({ mode: 'edit', location })
                    }}
                  >
                    편집
                  </button>
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setReleaseError(null)
                      setReleaseTarget(location)
                    }}
                  >
                    비활성화
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
      {form ? (
        <LocationFormDialog
          mode={form.mode}
          location={form.location}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setForm(null)
            }
          }}
          onCreate={(body) => void onCreate(body)}
          onUpdate={(locationId, body) => void onUpdate(locationId, body)}
        />
      ) : null}
      {releaseTarget ? (
        <ReleaseLocationDialog
          name={releaseTarget.name}
          submitting={releaseSubmitting}
          error={releaseError}
          onClose={() => {
            if (!releaseSubmitting) {
              setReleaseTarget(null)
            }
          }}
          onConfirm={() => void onDeactivate()}
        />
      ) : null}
    </section>
  )
}

function insertLocation(current: Location[] | null, created: Location): Location[] {
  const rows = (current ?? []).filter((row) => row.locationId !== created.locationId)
  rows.push(created)
  rows.sort((left, right) => left.displayOrder - right.displayOrder || left.locationId - right.locationId)
  return rows
}
