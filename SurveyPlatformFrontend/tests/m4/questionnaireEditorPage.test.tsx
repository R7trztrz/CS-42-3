// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'

const { questionnaireApiMock, questionBankApiMock } = vi.hoisted(() => ({
  questionnaireApiMock: {
    getQuestionnaire: vi.fn(),
    saveQuestionnaire: vi.fn(),
  },
  questionBankApiMock: {
    getQuestion: vi.fn(),
    listQuestions: vi.fn(),
  },
}))

vi.mock('../../src/researcher/questionnaire/api/questionnaireApi', () =>
  questionnaireApiMock,
)
vi.mock('../../src/researcher/questionBank/api/questionBankApi', () =>
  questionBankApiMock,
)

import QuestionnaireEditorPage from '../../src/researcher/questionnaire/pages/QuestionnaireEditorPage'
import BranchRuleEditor from '../../src/researcher/questionnaire/components/BranchRuleEditor'
import type { QuestionnaireEditorItem } from '../../src/shared/types/questionnaire'
import {
  makeQuestion,
  makeQuestionnaire,
  makeQuestionnaireItem,
  makeQuestionSummary,
} from './fixtures'

function questionPage(
  content: ReturnType<typeof makeQuestionSummary>[],
  page = 0,
  totalPages = 1,
) {
  return {
    content,
    page,
    size: 20,
    totalElements: content.length,
    totalPages,
  }
}

function renderEditor() {
  const router = createMemoryRouter(
    [
      {
        path: '/studies/:studyId/questionnaire',
        element: <QuestionnaireEditorPage />,
      },
      { path: '/elsewhere', element: <p>Elsewhere</p> },
    ],
    { initialEntries: ['/studies/study-1/questionnaire'] },
  )
  render(<RouterProvider router={router} />)
  return router
}

function addButtonFor(questionText: string) {
  const text = screen.getByText(questionText)
  const row = text.closest('li')
  if (!row) throw new Error(`No picker row for ${questionText}`)
  return within(row).getByRole('button', { name: /add/i })
}

function axiosError(
  code: string,
  message: string,
  details: Array<{
    field: string
    index: number | null
    itemId: string | null
    ruleIndex: number | null
    code: string
    message: string
  }> = [],
) {
  return {
    response: {
      data: {
        code,
        message,
        timestamp: '2026-10-05T00:00:00Z',
        path: '/api/studies/study-1/questionnaire',
        details,
      },
    },
  }
}

describe('M4 questionnaire editor browser behavior', () => {
  beforeEach(() => {
    questionnaireApiMock.getQuestionnaire.mockResolvedValue(null)
    questionnaireApiMock.saveQuestionnaire.mockReset()
    questionBankApiMock.getQuestion.mockImplementation((questionId: string) =>
      Promise.resolve(makeQuestion(questionId, `Question ${questionId}`)),
    )
    questionBankApiMock.listQuestions.mockResolvedValue(
      questionPage([makeQuestionSummary('question-1', 'Question question-1')]),
    )
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('marks edits dirty, blocks in-app navigation, guards beforeunload, and clears both after undo', async () => {
    const router = renderEditor()
    await screen.findByText('Question question-1')
    fireEvent.click(addButtonFor('Question question-1'))
    await screen.findByText('Unsaved changes')

    const unload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(unload)
    expect(unload.defaultPrevented).toBe(true)

    void router.navigate('/elsewhere')
    expect(await screen.findByRole('alertdialog')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Stay' }))
    expect(screen.getByRole('heading', { name: 'Questionnaire editor' })).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Undo' }))
    await waitFor(() => {
      expect(screen.queryByText('Unsaved changes')).toBeNull()
      const cleanUnload = new Event('beforeunload', { cancelable: true })
      window.dispatchEvent(cleanUnload)
      expect(cleanUnload.defaultPrevented).toBe(false)
    })
  })

  it('reuses server-assigned item IDs on a second save without reloading', async () => {
    const first = makeQuestion('question-1', 'First question')
    const second = makeQuestion('question-2', 'Second question')
    questionBankApiMock.listQuestions.mockResolvedValue(
      questionPage([
        makeQuestionSummary('question-1', 'First question'),
        makeQuestionSummary('question-2', 'Second question'),
      ]),
    )
    questionBankApiMock.getQuestion.mockImplementation((questionId: string) =>
      Promise.resolve(questionId === 'question-1' ? first : second),
    )
    questionnaireApiMock.saveQuestionnaire
      .mockResolvedValueOnce(
        makeQuestionnaire([makeQuestionnaireItem('item-1', 0, first)], { version: 0 }),
      )
      .mockResolvedValueOnce(
        makeQuestionnaire(
          [
            makeQuestionnaireItem('item-1', 0, first),
            makeQuestionnaireItem('item-2', 1, second),
          ],
          { version: 1 },
        ),
      )

    renderEditor()
    await screen.findByText('First question')
    fireEvent.click(addButtonFor('First question'))
    await screen.findByText('Unsaved changes')
    fireEvent.click(screen.getByRole('button', { name: 'Save questionnaire' }))
    await screen.findByText('Questionnaire saved.')

    fireEvent.click(addButtonFor('Second question'))
    await screen.findByText('Unsaved changes')
    fireEvent.click(screen.getByRole('button', { name: 'Save questionnaire' }))
    await waitFor(() => expect(questionnaireApiMock.saveQuestionnaire).toHaveBeenCalledTimes(2))

    expect(questionnaireApiMock.saveQuestionnaire.mock.calls[0][1].items[0].itemId).toBeNull()
    expect(questionnaireApiMock.saveQuestionnaire.mock.calls[1][1].items).toMatchObject([
      { itemId: 'item-1', questionId: 'question-1' },
      { itemId: null, questionId: 'question-2' },
    ])
  })

  it('shows structured backend validation coordinates without losing the local editor', async () => {
    const question = makeQuestion('question-1', 'Question one')
    questionnaireApiMock.getQuestionnaire.mockResolvedValue(
      makeQuestionnaire([makeQuestionnaireItem('item-1', 0, question)]),
    )
    questionnaireApiMock.saveQuestionnaire.mockRejectedValue(
      axiosError('INVALID_QUESTIONNAIRE', 'Questionnaire is invalid', [
        {
          field: 'branchRules',
          index: 1,
          itemId: 'item-2',
          ruleIndex: 0,
          code: 'BRANCH_CYCLE',
          message: 'Branch rules contain a cycle',
        },
      ]),
    )

    renderEditor()
    await screen.findByText('Question one')
    fireEvent.click(screen.getByRole('button', { name: 'Save questionnaire' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Questionnaire is invalid')
    expect(screen.getByText('Branch rules contain a cycle')).toBeTruthy()
    expect(
      screen.getByText(
        'Q2 · itemId item-2 · rule 1 · field branchRules · code BRANCH_CYCLE',
      ),
    ).toBeTruthy()
    expect(screen.getByText('Question one')).toBeTruthy()
  })

  it.each([
    [
      'QUESTIONNAIRE_VERSION_CONFLICT',
      'Someone else changed this questionnaire. Reload the page before saving again.',
    ],
    [
      'QUESTIONNAIRE_LOCKED',
      'This study is no longer a draft, so its questionnaire cannot be edited.',
    ],
  ])('renders the %s save state', async (code, expectedMessage) => {
    const question = makeQuestion('question-1', 'Question one')
    questionnaireApiMock.getQuestionnaire.mockResolvedValue(
      makeQuestionnaire([makeQuestionnaireItem('item-1', 0, question)]),
    )
    questionnaireApiMock.saveQuestionnaire.mockRejectedValue(axiosError(code, code))

    renderEditor()
    await screen.findByText('Question one')
    fireEvent.click(screen.getByRole('button', { name: 'Save questionnaire' }))
    expect((await screen.findByRole('alert')).textContent).toContain(expectedMessage)
    if (code === 'QUESTIONNAIRE_LOCKED') {
      expect(screen.getByText('Published - read only')).toBeTruthy()
    }
  })

  it('loads later picker pages and applies keyword and type filters', async () => {
    questionBankApiMock.listQuestions.mockImplementation(
      ({ page = 0, search, type }: { page?: number; search?: string; type?: string }) => {
        if (search === 'later' || type === 'SCALE') {
          return Promise.resolve(
            questionPage([makeQuestionSummary('question-2', 'Later question', 'SCALE')]),
          )
        }
        return Promise.resolve(
          page === 0
            ? questionPage([makeQuestionSummary('question-1', 'First page question')], 0, 2)
            : questionPage([makeQuestionSummary('question-2', 'Later question', 'SCALE')], 1, 2),
        )
      },
    )

    renderEditor()
    await screen.findByText('First page question')
    fireEvent.click(screen.getByRole('button', { name: 'Load more questions' }))
    expect(await screen.findByText('Later question')).toBeTruthy()
    expect(questionBankApiMock.listQuestions).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1, size: 20 }),
    )
    fireEvent.click(addButtonFor('Later question'))
    expect(await screen.findByText('Unsaved changes')).toBeTruthy()

    fireEvent.change(screen.getByLabelText('Search question bank'), {
      target: { value: 'later' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() =>
      expect(questionBankApiMock.listQuestions).toHaveBeenCalledWith(
        expect.objectContaining({ page: 0, search: 'later' }),
      ),
    )

    fireEvent.change(screen.getByLabelText('Filter question bank by type'), {
      target: { value: 'SCALE' },
    })
    await waitFor(() =>
      expect(questionBankApiMock.listQuestions).toHaveBeenCalledWith(
        expect.objectContaining({ page: 0, type: 'SCALE' }),
      ),
    )
  })

  it('reports an add failure without corrupting or dirtying the editor', async () => {
    questionBankApiMock.getQuestion.mockRejectedValueOnce(new Error('Question fetch failed'))

    renderEditor()
    await screen.findByText('Question question-1')
    fireEvent.click(addButtonFor('Question question-1'))

    expect((await screen.findByRole('alert')).textContent).toContain('Question fetch failed')
    expect(screen.queryByText('Unsaved changes')).toBeNull()
    expect(screen.getByText('Question question-1')).toBeTruthy()
  })

  it('renders published questionnaires as read-only snapshots', async () => {
    const question = makeQuestion('question-1', 'Published question')
    questionnaireApiMock.getQuestionnaire.mockResolvedValue(
      makeQuestionnaire([makeQuestionnaireItem('item-1', 0, question)], {
        contentSource: 'PUBLISHED_SNAPSHOT',
        snapshotId: 'snapshot-1',
        publishedAt: '2026-10-05T02:00:00Z',
      }),
    )

    renderEditor()
    expect(await screen.findByText('Published - read only')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Save questionnaire' })).toBeNull()
    expect(screen.queryByText('Add from question bank')).toBeNull()
  })
})

describe('M4 branch target labels', () => {
  afterEach(cleanup)

  it('keeps absolute numbering after missing and source rows are filtered', () => {
    const sourceQuestion = makeQuestion('source-question', 'Source', 'SINGLE_CHOICE')
    const targetQuestion = makeQuestion('target-question', 'Target')
    const items: QuestionnaireEditorItem[] = [
      {
        clientId: 'source',
        itemId: 'item-1',
        questionId: sourceQuestion.questionId,
        question: sourceQuestion,
        missing: false,
        branchRules: [],
      },
      {
        clientId: 'missing',
        itemId: 'item-2',
        questionId: '',
        question: null,
        missing: true,
        branchRules: [],
      },
      {
        clientId: 'target',
        itemId: 'item-3',
        questionId: targetQuestion.questionId,
        question: targetQuestion,
        missing: false,
        branchRules: [],
      },
    ]

    render(
      <BranchRuleEditor
        question={sourceQuestion}
        branchRules={[
          {
            sourceOptionId: sourceQuestion.options[0].optionId,
            sourceScaleValue: null,
            targetClientId: 'target',
          },
        ]}
        items={items}
        sourceClientId="source"
        onChange={() => undefined}
      />,
    )

    expect(screen.getByRole('option', { name: 'Q3: Target' })).toBeTruthy()
    expect(screen.queryByRole('option', { name: /Q2:/ })).toBeNull()
  })
})
