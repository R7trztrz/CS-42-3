import { describe, expect, it } from 'vitest'
import {
  QUESTIONNAIRE_HISTORY_LIMIT,
  apiErrorDetailToView,
  buildSaveQuestionnaireRequest,
  getBranchTargetOptions,
  hydrateQuestionnaireItems,
  questionnaireEditorReducer,
  questionnaireIssueLocation,
} from '../../src/researcher/questionnaire/model/questionnaireEditorModel'
import type { QuestionnaireEditorItem } from '../../src/shared/types/questionnaire'
import { makeQuestion, makeQuestionnaireItem } from './fixtures'

let nextClientId = 1
const createClientId = () => `generated-${nextClientId++}`

function editorItem(
  clientId: string,
  questionId: string,
  itemId: string | null = null,
): QuestionnaireEditorItem {
  return {
    clientId,
    itemId,
    questionId,
    question: makeQuestion(questionId, `Question ${questionId}`),
    missing: false,
    branchRules: [],
  }
}

describe('M4 questionnaire editor model', () => {
  it('writes first-save item IDs back while retaining client identities for the next PUT', () => {
    const previous = [editorItem('client-a', 'question-a'), editorItem('client-b', 'question-b')]
    previous[0].branchRules = [
      { sourceOptionId: null, sourceScaleValue: 1, targetClientId: 'client-b' },
    ]
    const savedItems = [
      makeQuestionnaireItem('item-a', 0, makeQuestion('question-a', 'A', 'SCALE'), [
        {
          id: 'rule-1',
          sourceOptionId: null,
          sourceScaleValue: 1,
          targetItemId: 'item-b',
          targetPosition: 1,
        },
      ]),
      makeQuestionnaireItem('item-b', 1, makeQuestion('question-b', 'B')),
    ]

    const hydrated = hydrateQuestionnaireItems(savedItems, previous, createClientId)
    expect(hydrated.map((item) => [item.clientId, item.itemId])).toEqual([
      ['client-a', 'item-a'],
      ['client-b', 'item-b'],
    ])
    expect(hydrated[0].branchRules[0].targetClientId).toBe('client-b')

    const nextRequest = buildSaveQuestionnaireRequest(hydrated, 1)
    expect(nextRequest.items.map((item) => item.itemId)).toEqual(['item-a', 'item-b'])
    expect(nextRequest.items[0].branchRules[0].targetPosition).toBe(1)
  })

  it('uses absolute positions for targets and excludes self and missing questions', () => {
    const source = editorItem('source', 'question-a')
    const missing = { ...editorItem('missing', 'question-b'), question: null, missing: true }
    const target = editorItem('target', 'question-c')

    expect(getBranchTargetOptions([source, missing, target], 'source')).toEqual([
      { item: target, position: 2 },
    ])
  })

  it('makes every available validation coordinate visible', () => {
    const issue = apiErrorDetailToView({
      field: 'branchRules',
      index: 2,
      itemId: 'item-3',
      ruleIndex: 1,
      code: 'BRANCH_CYCLE',
      message: 'Cycle detected',
    })
    expect(questionnaireIssueLocation(issue)).toEqual([
      'Q3',
      'itemId item-3',
      'rule 2',
      'field branchRules',
      'code BRANCH_CYCLE',
    ])
  })

  it('undoes add, remove, and move operations and caps history at twenty snapshots', () => {
    const a = editorItem('a', 'question-a')
    const b = editorItem('b', 'question-b')
    let state = { items: [a, b], past: [] as QuestionnaireEditorItem[][] }

    state = questionnaireEditorReducer(state, { type: 'move', index: 0, direction: 1 })
    expect(state.items.map((item) => item.clientId)).toEqual(['b', 'a'])
    state = questionnaireEditorReducer(state, { type: 'undo' })
    expect(state.items.map((item) => item.clientId)).toEqual(['a', 'b'])

    state = questionnaireEditorReducer(state, { type: 'remove', clientId: 'a' })
    expect(state.items.map((item) => item.clientId)).toEqual(['b'])
    state = questionnaireEditorReducer(state, { type: 'undo' })
    expect(state.items.map((item) => item.clientId)).toEqual(['a', 'b'])

    state = questionnaireEditorReducer(state, {
      type: 'append',
      item: editorItem('c', 'question-c'),
    })
    expect(state.items.map((item) => item.clientId)).toEqual(['a', 'b', 'c'])
    state = questionnaireEditorReducer(state, { type: 'undo' })
    expect(state.items.map((item) => item.clientId)).toEqual(['a', 'b'])

    for (let index = 0; index < QUESTIONNAIRE_HISTORY_LIMIT + 5; index += 1) {
      state = questionnaireEditorReducer(state, {
        type: 'append',
        item: editorItem(`extra-${index}`, `question-${index}`),
      })
    }
    expect(state.past).toHaveLength(QUESTIONNAIRE_HISTORY_LIMIT)

    state = questionnaireEditorReducer(state, { type: 'reset', items: state.items })
    expect(state.past).toEqual([])
  })
})
