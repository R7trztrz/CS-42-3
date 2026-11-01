// @vitest-environment happy-dom

import { useState } from 'react'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import BranchRuleEditor from '../../src/researcher/questionnaire/components/BranchRuleEditor'
import {
  buildSaveQuestionnaireRequest,
  questionnaireEditorReducer,
} from '../../src/researcher/questionnaire/model/questionnaireEditorModel'
import type {
  QuestionnaireEditorBranchRule,
  QuestionnaireEditorItem,
} from '../../src/shared/types/questionnaire'
import { makeQuestion } from './fixtures'

afterEach(cleanup)

const choice = makeQuestion('choice', 'Route', 'SINGLE_CHOICE')
const scale = { ...makeQuestion('scale', 'Rating', 'SCALE'), scaleMin: -1, scaleMax: 1 }

function item(clientId: string, question = makeQuestion(clientId, clientId)): QuestionnaireEditorItem {
  return { clientId, itemId: `item-${clientId}`, questionId: question.questionId,
    question, missing: false, branchRules: [] }
}

function mount(question = choice, initial: QuestionnaireEditorBranchRule[] = [], disabled = false) {
  const changed = vi.fn()
  function Harness() {
    const [rules, setRules] = useState(initial)
    return <BranchRuleEditor question={question} branchRules={rules}
      items={[item('source', question), item('second'), item('third')]}
      sourceClientId="source" disabled={disabled}
      onChange={(next) => { changed(next); setRules(next) }} />
  }
  render(<Harness />)
  return changed
}

describe('M4 branch editing regression', () => {
  it('adds each choice trigger once, stops at exhaustion and frees a removed trigger', () => {
    const changed = mount()
    const add = screen.getByRole('button', { name: '+ Add jump rule' })
    fireEvent.click(add)
    fireEvent.click(add)
    expect(changed.mock.lastCall?.[0]).toEqual([
      { sourceOptionId: 'choice-option-1', sourceScaleValue: null, targetClientId: 'second' },
      { sourceOptionId: 'choice-option-2', sourceScaleValue: null, targetClientId: 'second' },
    ])
    expect((add as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(screen.getAllByRole('button', { name: 'Remove' })[0])
    fireEvent.click(add)
    expect(changed.mock.lastCall?.[0].map((r: QuestionnaireEditorBranchRule) => r.sourceOptionId))
      .toEqual(['choice-option-2', 'choice-option-1'])
  })

  it('updates a choice trigger and target without losing the other rule', () => {
    const changed = mount(choice, [
      { sourceOptionId: 'choice-option-1', sourceScaleValue: null, targetClientId: 'second' },
    ])
    const selects = screen.getAllByRole('combobox')
    fireEvent.change(selects[0], { target: { value: 'choice-option-2' } })
    fireEvent.change(selects[1], { target: { value: 'third' } })
    expect(changed.mock.lastCall?.[0]).toEqual([
      { sourceOptionId: 'choice-option-2', sourceScaleValue: null, targetClientId: 'third' },
    ])
  })

  it('keeps negative and zero scale triggers numeric and includes both endpoints', () => {
    const changed = mount(scale)
    const add = screen.getByRole('button', { name: '+ Add jump rule' })
    fireEvent.click(add)
    fireEvent.click(add)
    fireEvent.click(add)
    expect(changed.mock.lastCall?.[0].map((r: QuestionnaireEditorBranchRule) =>
      [r.sourceOptionId, r.sourceScaleValue])).toEqual([[null, -1], [null, 0], [null, 1]])
    expect((add as HTMLButtonElement).disabled).toBe(true)
    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: '0' } })
    expect(changed.mock.lastCall?.[0][0].sourceScaleValue).toBe(0)
  })

  it.each(['TEXT', 'MULTI_CHOICE'] as const)('does not offer branching for %s', (type) => {
    const changed = mount(makeQuestion('unsupported', 'Unsupported', type))
    expect(screen.queryByRole('button', { name: '+ Add jump rule' })).toBeNull()
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(changed).not.toHaveBeenCalled()
  })

  it('disables rule creation, removal and selects for read-only content', () => {
    const changed = mount(choice, [
      { sourceOptionId: 'choice-option-1', sourceScaleValue: null, targetClientId: 'third' },
    ], true)
    for (const element of screen.getAllByRole('button')) {
      expect((element as HTMLButtonElement).disabled).toBe(true)
      fireEvent.click(element)
    }
    for (const element of screen.getAllByRole('combobox')) {
      expect((element as HTMLSelectElement).disabled).toBe(true)
    }
    expect(changed).not.toHaveBeenCalled()
  })

  it('does not offer a missing target or the source itself', () => {
    const missing = { ...item('missing'), question: null, missing: true }
    render(<BranchRuleEditor question={choice} branchRules={[]} sourceClientId="source"
      items={[item('source', choice), missing]} onChange={vi.fn()} />)
    expect(screen.getByText('Add another item to configure a jump target.')).toBeTruthy()
    expect(screen.queryByRole('button')).toBeNull()
  })
})

describe('M4 branch identities across editor changes', () => {
  function initial() {
    const source = { ...item('source', choice), branchRules: [
      { sourceOptionId: 'choice-option-1', sourceScaleValue: null, targetClientId: 'third' },
    ] }
    return { items: [source, item('second'), item('third')], past: [] }
  }

  it('reorders the branch destination by identity and serializes its new position', () => {
    const state = questionnaireEditorReducer(initial(), { type: 'move', index: 2, direction: -1 })
    const request = buildSaveQuestionnaireRequest(state.items, 7)
    expect(request.expectedVersion).toBe(7)
    expect(request.items[0].branchRules[0].targetPosition).toBe(1)
    expect(state.items[1].clientId).toBe('third')
    expect(initial().items[0].branchRules[0].targetClientId).toBe('third')
  })

  it('removes incoming rules with their destination and restores both on undo', () => {
    const original = initial()
    const removed = questionnaireEditorReducer(original, { type: 'remove', clientId: 'third' })
    expect(removed.items.map((row) => row.clientId)).toEqual(['source', 'second'])
    expect(buildSaveQuestionnaireRequest(removed.items, 7).items[0].branchRules).toEqual([])
    expect(original.items[0].branchRules).toHaveLength(1)
    const restored = questionnaireEditorReducer(removed, { type: 'undo' })
    expect(restored.items).toEqual(original.items)
    expect(buildSaveQuestionnaireRequest(restored.items, 7).items[0].branchRules[0].targetPosition).toBe(2)
  })

  it('undoes a branch-only edit without changing item identities', () => {
    const original = initial()
    const edited = questionnaireEditorReducer(original, { type: 'replace-branch-rules',
      clientId: 'source', branchRules: [] })
    expect(edited.items[0].branchRules).toEqual([])
    expect(questionnaireEditorReducer(edited, { type: 'undo' }).items).toEqual(original.items)
  })
})
