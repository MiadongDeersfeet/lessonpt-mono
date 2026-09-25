export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="page">
      <h1>{title}</h1>
      <p className="lead">이 화면은 다음 단계에서 연결합니다.</p>
    </section>
  )
}
