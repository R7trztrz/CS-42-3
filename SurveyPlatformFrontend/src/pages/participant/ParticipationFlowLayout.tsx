import type { ReactNode } from 'react'
import { ParticipantTemplate, type DemoStep } from './ParticipantDemoTemplate'

type Props = {
  step: DemoStep
  title?: string
  children: ReactNode
  activityActive?: boolean
}

export default function ParticipationFlowLayout({ step, title, children, activityActive = false }: Props) {
  return (
    <ParticipantTemplate
      currentStep={step}
      title={title}
      className={`participation-flow participation-flow--${step}${activityActive ? ' participation-flow--test' : ''}`}
      keepNavigation
    >
      {children}
    </ParticipantTemplate>
  )
}
