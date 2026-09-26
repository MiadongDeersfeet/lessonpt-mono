export function BpmGauge({ current, target }: { current: number | null; target: number | null }) {
  if (current == null && target == null) {
    return null
  }
  if (current != null && target != null && target > 0) {
    const ratio = Math.min(100, Math.round((current / target) * 100))
    const reached = current >= target
    return (
      <div className="bpm-gauge">
        <p className="bpm-figures">{current} → {target}</p>
        <div
          className="bpm-track"
          role="meter"
          aria-label="목표 BPM 대비"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={ratio}
        >
          <span style={{ width: `${ratio}%` }} />
        </div>
        <p className="sr-only">목표 BPM 대비 {ratio}%{reached ? ' · 목표 달성' : ''}</p>
      </div>
    )
  }
  if (current == null && target != null) {
    return <p>목표 {target} BPM</p>
  }
  if (current != null) {
    return <p>현재 {current} BPM</p>
  }
  return null
}
