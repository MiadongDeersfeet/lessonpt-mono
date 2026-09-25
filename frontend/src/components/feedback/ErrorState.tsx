import { describeError } from './describeError.ts'

export function ErrorState({ error }: { error: unknown }) {
  const view = describeError(error)
  return (
    <div className="state state-error" role="alert">
      <p className="state-title">{view.title}</p>
      <p>{view.detail}</p>
      {view.traceId ? <p className="trace">traceId {view.traceId}</p> : null}
    </div>
  )
}
