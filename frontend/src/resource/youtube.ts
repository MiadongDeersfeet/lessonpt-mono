const videoIdPattern = /^[A-Za-z0-9_-]{11}$/

export function youtubeEmbedUrl(value: string | null | undefined): string | null {
  if (value == null || value.trim() === '') {
    return null
  }
  let url: URL
  try {
    url = new URL(value.trim())
  } catch {
    return null
  }
  if (url.protocol !== 'https:') {
    return null
  }
  const host = url.hostname.toLowerCase()
  const parts = url.pathname.split('/').filter(Boolean)
  let videoId: string | null = null
  if (host === 'youtu.be') {
    videoId = parts[0] ?? null
  } else if (host === 'youtube.com' || host === 'www.youtube.com' || host === 'm.youtube.com') {
    if (parts[0] === 'watch') {
      videoId = url.searchParams.get('v')
    } else if (parts[0] === 'embed' || parts[0] === 'shorts') {
      videoId = parts[1] ?? null
    }
  }
  if (videoId == null || !videoIdPattern.test(videoId)) {
    return null
  }
  return `https://www.youtube-nocookie.com/embed/${videoId}`
}
