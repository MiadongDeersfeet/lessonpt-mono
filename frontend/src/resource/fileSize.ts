export const PDF_MAX_BYTES = 20 * 1024 * 1024
export const AUDIO_MAX_BYTES = 50 * 1024 * 1024

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
