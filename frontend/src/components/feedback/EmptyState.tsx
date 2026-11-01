export function EmptyState({ message }: { message: string }) {
  return <div className="state state-empty"><span className="empty-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M8 5h12l5 5v17H8zM20 5v6h5M12 16h9M12 21h6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg></span><p>{message}</p></div>
}
