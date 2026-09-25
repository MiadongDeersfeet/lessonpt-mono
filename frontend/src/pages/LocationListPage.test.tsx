import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { LocationListPage } from './LocationListPage.tsx'

vi.mock('../api/locationApi.ts', () => ({
  listLocations: vi.fn(),
  createLocation: vi.fn(),
  updateLocation: vi.fn(),
  deactivateLocation: vi.fn(),
}))

import { createLocation, deactivateLocation, listLocations, updateLocation } from '../api/locationApi.ts'

const listed = {
  locationId: 30,
  name: '연습실',
  displayOrder: 1,
  address: '서울',
  createdAt: '2026-09-25T00:00:00',
  updatedAt: '2026-09-25T00:00:00',
}

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  vi.mocked(listLocations).mockReset()
  vi.mocked(createLocation).mockReset()
  vi.mocked(updateLocation).mockReset()
  vi.mocked(deactivateLocation).mockReset()
  vi.mocked(listLocations).mockResolvedValue([listed])
})

it('creates a location and shows the returned row without another list request', async () => {
  const user = userEvent.setup()
  vi.mocked(createLocation).mockResolvedValue({
    locationId: 31,
    name: '새연습실',
    displayOrder: 2,
    address: null,
    createdAt: '2026-09-25T00:00:00',
    updatedAt: '2026-09-25T00:00:00',
  })
  render(<LocationListPage />)

  await user.click(await screen.findByRole('button', { name: '출강처 추가' }))
  await user.type(screen.getByLabelText('이름'), '새연습실')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('새연습실')).toBeTruthy()
  expect(createLocation).toHaveBeenCalledWith({ name: '새연습실', address: null })
  expect(listLocations).toHaveBeenCalledTimes(1)
})

it('shows a validation message beside the name field', async () => {
  const user = userEvent.setup()
  vi.mocked(createLocation).mockRejectedValue(
    new ApiError(400, 'COMMON_INVALID_INPUT', '요청 값이 올바르지 않습니다.', 'trace-9', [
      { field: 'name', message: '이름은 150자 이하여야 합니다.' },
    ]),
  )
  render(<LocationListPage />)

  await user.click(await screen.findByRole('button', { name: '출강처 추가' }))
  await user.type(screen.getByLabelText('이름'), '긴이름')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('이름은 150자 이하여야 합니다.')).toBeTruthy()
  expect(screen.getByText('입력값을 확인해 주세요.')).toBeTruthy()
})

it('updates the listed location from the patch response', async () => {
  const user = userEvent.setup()
  vi.mocked(updateLocation).mockResolvedValue({ ...listed, name: '수정연습실', address: null })
  render(<LocationListPage />)

  await user.click(await screen.findByRole('button', { name: '편집' }))
  const dialog = await screen.findByRole('dialog')
  expect(within(dialog).queryByLabelText('순서')).toBeNull()
  expect(within(dialog).queryByRole('spinbutton')).toBeNull()
  const name = within(dialog).getByLabelText('이름')
  await user.clear(name)
  await user.type(name, '수정연습실')
  await user.clear(within(dialog).getByLabelText('주소'))
  await user.click(within(dialog).getByRole('button', { name: '저장' }))

  expect(await screen.findByText('수정연습실')).toBeTruthy()
  expect(updateLocation).toHaveBeenCalledWith(30, { name: '수정연습실', address: null })
  expect(listLocations).toHaveBeenCalledTimes(1)
})

it('asks before deactivating a location and then refetches the list', async () => {
  const user = userEvent.setup()
  vi.mocked(deactivateLocation).mockResolvedValue(undefined)
  vi.mocked(listLocations)
    .mockResolvedValueOnce([listed, { ...listed, locationId: 32, name: '뒤연습실', displayOrder: 2 }])
    .mockResolvedValueOnce([{ ...listed, locationId: 32, name: '뒤연습실', displayOrder: 1 }])
  render(<LocationListPage />)

  const row = (await screen.findByText('연습실')).closest('tr')
  if (!row) {
    throw new Error('location row missing')
  }
  await user.click(within(row).getByRole('button', { name: '비활성화' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('활성 수강')
  expect(dialog.textContent).toContain('자동으로 돌아오지 않습니다')
  expect(deactivateLocation).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '비활성화' }))

  expect(deactivateLocation).toHaveBeenCalledWith(30)
  expect(await screen.findByText('뒤연습실')).toBeTruthy()
  expect(screen.queryByText('연습실')).toBeNull()
  expect(listLocations).toHaveBeenCalledTimes(2)
})

it('shows the order conflict message and does not refetch after a failed deactivate', async () => {
  const user = userEvent.setup()
  const message = '다른 요청이 장소 순서를 변경 중입니다. 잠시 후 다시 시도해 주세요.'
  vi.mocked(deactivateLocation).mockRejectedValue(new ApiError(409, 'ORDER_CONFLICT', message, 'trace-3', []))
  render(<LocationListPage />)

  await user.click(await screen.findByRole('button', { name: '비활성화' }))
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: '비활성화' }))

  expect(await screen.findByText(message)).toBeTruthy()
  expect(listLocations).toHaveBeenCalledTimes(1)
})
