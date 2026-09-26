import { expect, it } from 'vitest'
import { youtubeEmbedUrl } from './youtube.ts'

it('builds a nocookie embed url from allowed youtube forms', () => {
  expect(youtubeEmbedUrl('https://www.youtube.com/watch?v=abcdefghijk')).toBe(
    'https://www.youtube-nocookie.com/embed/abcdefghijk',
  )
  expect(youtubeEmbedUrl('https://youtube.com/embed/abcdefghijk')).toBe(
    'https://www.youtube-nocookie.com/embed/abcdefghijk',
  )
  expect(youtubeEmbedUrl('https://m.youtube.com/shorts/abcdefghijk')).toBe(
    'https://www.youtube-nocookie.com/embed/abcdefghijk',
  )
  expect(youtubeEmbedUrl('https://youtu.be/abcdefghijk')).toBe(
    'https://www.youtube-nocookie.com/embed/abcdefghijk',
  )
})

it('rejects missing, http, and unexpected youtube values', () => {
  expect(youtubeEmbedUrl(null)).toBeNull()
  expect(youtubeEmbedUrl('http://www.youtube.com/watch?v=abcdefghijk')).toBeNull()
  expect(youtubeEmbedUrl('https://example.com/watch?v=abcdefghijk')).toBeNull()
  expect(youtubeEmbedUrl('https://youtu.be/short')).toBeNull()
  expect(youtubeEmbedUrl('not a url')).toBeNull()
})
