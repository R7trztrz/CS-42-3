// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

const { createQuestion, getQuestion, updateQuestion } = vi.hoisted(() => ({
  createQuestion: vi.fn(), getQuestion: vi.fn(), updateQuestion: vi.fn(),
}))
vi.mock('../../src/researcher/questionBank/api/questionBankApi', () => ({ createQuestion, getQuestion, updateQuestion }))

import QuestionFormPage from '../../src/researcher/questionBank/pages/QuestionFormPage'
import { makeQuestion } from './fixtures'

afterEach(() => { cleanup(); vi.resetAllMocks() })

function open(path = '/questions/new') {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/questions/new" element={<QuestionFormPage />} />
    <Route path="/questions/:questionId/edit" element={<QuestionFormPage />} />
    <Route path="/questions" element={<h2>Question bank list</h2>} />
  </Routes></MemoryRouter>)
}

describe('question form workspace', () => {
  it('shows researcher navigation and creates a choice question', async () => {
    createQuestion.mockResolvedValue({})
    open()
    expect(screen.getByRole('navigation', { name: 'Researcher pages' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'New question' })).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Question text'), { target: { value: 'Which feed?' } })
    fireEvent.change(screen.getByLabelText('Option 1'), { target: { value: 'First feed' } })
    fireEvent.change(screen.getByLabelText('Option 2'), { target: { value: 'Second feed' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save question' }))
    await screen.findByRole('heading', { name: 'Question bank list' })
    expect(createQuestion).toHaveBeenCalledWith(expect.objectContaining({ type: 'SINGLE_CHOICE', questionText: 'Which feed?', required: true }))
  })

  it('keeps scale settings and option editing available', () => {
    open()
    fireEvent.click(screen.getByRole('button', { name: '+ Add option' }))
    expect(screen.getByLabelText('Option 3')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Question type'), { target: { value: 'SCALE' } })
    expect(screen.getByLabelText('Minimum')).toBeTruthy()
    expect(screen.getByLabelText('Maximum')).toBeTruthy()
    expect(screen.getByLabelText('Minimum label')).toBeTruthy()
    expect(screen.queryByLabelText('Option 1')).toBeNull()
  })

  it('uses the same workspace for editing and preserves question identity', async () => {
    getQuestion.mockResolvedValue(makeQuestion('existing', 'Original text', 'TEXT'))
    updateQuestion.mockResolvedValue({})
    open('/questions/existing/edit')
    await screen.findByRole('heading', { name: 'Edit question' })
    expect(screen.getByLabelText('Question text')).toHaveProperty('value', 'Original text')
    fireEvent.click(screen.getByRole('button', { name: 'Save question' }))
    await waitFor(() => expect(updateQuestion).toHaveBeenCalledWith('existing', expect.objectContaining({ questionText: 'Original text' })))
  })

  it('returns to the question bank without saving on cancel', async () => {
    open()
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    await screen.findByRole('heading', { name: 'Question bank list' })
    expect(createQuestion).not.toHaveBeenCalled()
  })
})
