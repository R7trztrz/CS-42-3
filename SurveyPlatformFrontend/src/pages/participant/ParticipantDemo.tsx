import { useState } from 'react'

import campusPost from '../../assets/participant-demo-campus.jpg'
import './ParticipantDemo.css'

type DemoStep =
  | 'introduction'
  | 'consent'
  | 'calibration'
  | 'experience'
  | 'questionnaire'
  | 'complete'

type StepDefinition = {
  id: DemoStep
  shortLabel: string
  label: string
}

const steps: StepDefinition[] = [
  { id: 'introduction', shortLabel: 'Welcome', label: 'Study introduction' },
  { id: 'consent', shortLabel: 'Consent', label: 'Informed consent' },
  { id: 'calibration', shortLabel: 'Set up', label: 'Eye-tracking setup' },
  { id: 'experience', shortLabel: 'Activity', label: 'Social media activity' },
  { id: 'questionnaire', shortLabel: 'Survey', label: 'Final questionnaire' },
]

function ArrowIcon({ direction = 'right' }: { direction?: 'left' | 'right' }) {
  return (
    <span aria-hidden="true" className="demo-arrow">
      {direction === 'right' ? '\u2192' : '\u2190'}
    </span>
  )
}

function DemoHeader({
  currentStep,
}: {
  currentStep: DemoStep
}) {
  const currentIndex = steps.findIndex((step) => step.id === currentStep)

  return (
    <header className="demo-header">
      <div className="demo-header__inner">
        <div className="demo-brand" aria-label="Research study">
          <span className="demo-brand__mark">SP</span>
          <span>
            <strong>Research study</strong>
            <small>Participant session</small>
          </span>
        </div>

        {currentStep !== 'complete' && currentStep !== 'experience' && (
          <div className="demo-progress" aria-label="Study progress">
            <span>Step {currentIndex + 1} of {steps.length}</span>
            <div className="demo-progress__track" aria-hidden="true">
              <span style={{ width: `${((currentIndex + 1) / steps.length) * 100}%` }} />
            </div>
          </div>
        )}
      </div>
    </header>
  )
}

function StepRail({ currentStep }: { currentStep: DemoStep }) {
  const currentIndex = steps.findIndex((step) => step.id === currentStep)

  return (
    <ol className="demo-step-rail" aria-label="Study steps">
      {steps.map((step, index) => {
        const isCurrent = step.id === currentStep
        const isComplete = index < currentIndex

        return (
          <li
            className={isCurrent ? 'is-current' : isComplete ? 'is-complete' : ''}
            key={step.id}
          >
            <span className="demo-step-rail__number">
              {isComplete ? '\u2713' : index + 1}
            </span>
            <span>
              <small>{step.shortLabel}</small>
              <strong>{step.label}</strong>
            </span>
          </li>
        )
      })}
    </ol>
  )
}

function IntroductionStep({ onNext }: { onNext: () => void }) {
  return (
    <div className="demo-two-column">
      <section className="demo-copy-panel">
        <p className="demo-eyebrow">University research study</p>
        <h1>How people explore social media content</h1>
        <p className="demo-lead">
          In this study, you will browse a short social media feed and answer a few
          questions about your experience.
        </p>

        <div className="demo-notice">
          <span className="demo-notice__icon" aria-hidden="true">i</span>
          <div>
            <strong>Before you begin</strong>
            <p>You will be asked to allow camera access for eye-tracking calibration.</p>
          </div>
        </div>

        <div className="demo-actions">
          <button className="demo-button demo-button--primary" onClick={onNext} type="button">
            Begin study <ArrowIcon />
          </button>
        </div>
      </section>

      <aside className="demo-study-facts" aria-label="Study details">
        <h2>Study details</h2>
        <dl>
          <div>
            <dt>Estimated time</dt>
            <dd>8-10 minutes</dd>
          </div>
          <div>
            <dt>What you need</dt>
            <dd>A laptop or desktop with a camera</dd>
          </div>
          <div>
            <dt>Participation</dt>
            <dd>Voluntary and confidential</dd>
          </div>
          <div>
            <dt>Reference</dt>
            <dd>DMBS-2026-014</dd>
          </div>
        </dl>
        <p className="demo-study-facts__help">
          Questions? Contact the research team at research@example.edu.
        </p>
      </aside>
    </div>
  )
}

function ConsentStep({ onBack, onNext }: { onBack: () => void; onNext: () => void }) {
  return (
    <section className="demo-reading-panel">
      <p className="demo-eyebrow">Informed consent</p>
      <h1>Please review before continuing</h1>
      <p className="demo-lead">
        This sample summarises the information a participant would review before
        joining the study.
      </p>

      <div className="demo-consent-grid">
        <article>
          <span>01</span>
          <div>
            <h2>What you will do</h2>
            <p>Complete a camera setup, browse a simulated feed, and answer a questionnaire.</p>
          </div>
        </article>
        <article>
          <span>02</span>
          <div>
            <h2>How data is used</h2>
            <p>Interaction and gaze data will be analysed for academic research purposes.</p>
          </div>
        </article>
        <article>
          <span>03</span>
          <div>
            <h2>Your choice</h2>
            <p>Participation is voluntary. You may leave before submitting your responses.</p>
          </div>
        </article>
      </div>

      <label className="demo-consent-check">
        <input defaultChecked type="checkbox" />
        <span>
          <strong>I have read the participant information and agree to take part.</strong>
          <small>This is sample content for the interface demonstration.</small>
        </span>
      </label>

      <div className="demo-actions demo-actions--split">
        <button className="demo-button demo-button--quiet" onClick={onBack} type="button">
          <ArrowIcon direction="left" /> Back
        </button>
        <button className="demo-button demo-button--primary" onClick={onNext} type="button">
          Agree and continue <ArrowIcon />
        </button>
      </div>
    </section>
  )
}

function CalibrationStep({ onBack, onNext }: { onBack: () => void; onNext: () => void }) {
  return (
    <section className="demo-calibration">
      <div className="demo-calibration__copy">
        <p className="demo-eyebrow">Eye-tracking setup</p>
        <h1>Let&apos;s prepare your camera</h1>
        <p className="demo-lead">
          Sit comfortably, face the screen, and make sure your face is evenly lit.
        </p>
        <ul className="demo-check-list">
          <li><span>1</span>Position the screen at eye level</li>
          <li><span>2</span>Keep your face inside the guide</li>
          <li><span>3</span>Follow each calibration point</li>
        </ul>
      </div>

      <div className="demo-camera-preview" aria-label="Camera preview placeholder">
        <div className="demo-camera-preview__top">
          <span><i /> Camera preview</span>
          <small>Ready</small>
        </div>
        <div className="demo-face-guide">
          <span className="demo-face-guide__head" />
          <span className="demo-face-guide__shoulders" />
        </div>
        <p>Your camera preview will appear here</p>
      </div>

      <div className="demo-actions demo-actions--split demo-calibration__actions">
        <button className="demo-button demo-button--quiet" onClick={onBack} type="button">
          <ArrowIcon direction="left" /> Back
        </button>
        <button className="demo-button demo-button--primary" onClick={onNext} type="button">
          Start calibration <ArrowIcon />
        </button>
      </div>
    </section>
  )
}

function ExperienceStep({ onNext }: { onNext: () => void }) {
  return (
    <section className="demo-experience">
      <header className="demo-experience__header">
        <div className="demo-brand demo-brand--dark">
          <span className="demo-brand__mark">SP</span>
          <span>
            <strong>Study activity</strong>
            <small>Browse the feed naturally</small>
          </span>
        </div>
        <div className="demo-experience__time">
          <span>Activity</span>
          <strong>02:00</strong>
        </div>
      </header>

      <div className="demo-social-shell">
        <aside className="demo-social-nav">
          <div className="demo-social-logo">loop</div>
          <nav aria-label="Social media demonstration">
            <strong><span>H</span> Home</strong>
            <span><i>S</i> Search</span>
            <span><i>E</i> Explore</span>
            <span><i>M</i> Messages</span>
          </nav>
          <div className="demo-social-profile">
            <span>Y</span>
            <p><strong>Your profile</strong><small>@participant</small></p>
          </div>
        </aside>

        <main className="demo-feed">
          <div className="demo-feed__tabs"><span>Following</span><strong>For you</strong></div>
          <article className="demo-post">
            <header>
              <span className="demo-avatar">EW</span>
              <div><strong>Emma Wilson</strong><small>@emmaw · 12 min</small></div>
              <button aria-label="More post options" type="button">...</button>
            </header>
            <p>Autumn arrived on campus today. A perfect afternoon for a walk between classes.</p>
            <img alt="University courtyard in autumn" src={campusPost} />
            <footer>
              <span><b>24</b> comments</span>
              <span><b>148</b> likes</span>
              <span><b>12</b> shares</span>
            </footer>
          </article>
        </main>

        <aside className="demo-activity-panel">
          <p className="demo-eyebrow">Study activity</p>
          <h2>Browse as you normally would</h2>
          <p>Take a moment to explore the post. Continue when you are ready.</p>
          <button className="demo-button demo-button--coral" onClick={onNext} type="button">
            Finish browsing <ArrowIcon />
          </button>
        </aside>
      </div>
    </section>
  )
}

function QuestionnaireStep({ onBack, onNext }: { onBack: () => void; onNext: () => void }) {
  return (
    <section className="demo-questionnaire">
      <div className="demo-questionnaire__heading">
        <p className="demo-eyebrow">Final questionnaire</p>
        <h1>Tell us about your experience</h1>
        <p className="demo-lead">There are three sample questions on this page.</p>
      </div>

      <div className="demo-question-list">
        <fieldset>
          <legend><span>1</span> How engaging did you find the social media post?</legend>
          <div className="demo-scale">
            {['Not at all', 'Slightly', 'Moderately', 'Very', 'Extremely'].map((label, index) => (
              <label key={label}>
                <input defaultChecked={index === 2} name="engagement" type="radio" />
                <span>{index + 1}</span>
                <small>{label}</small>
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend><span>2</span> How familiar did the feed interface feel?</legend>
          <div className="demo-scale">
            {['Unfamiliar', 'A little', 'Somewhat', 'Very', 'Completely'].map((label, index) => (
              <label key={label}>
                <input defaultChecked={index === 3} name="familiarity" type="radio" />
                <span>{index + 1}</span>
                <small>{label}</small>
              </label>
            ))}
          </div>
        </fieldset>

        <label className="demo-text-question">
          <strong><span>3</span> Is there anything else you would like to share?</strong>
          <textarea placeholder="Optional response" rows={4} />
        </label>
      </div>

      <div className="demo-actions demo-actions--split">
        <button className="demo-button demo-button--quiet" onClick={onBack} type="button">
          <ArrowIcon direction="left" /> Back
        </button>
        <button className="demo-button demo-button--primary" onClick={onNext} type="button">
          Submit responses <ArrowIcon />
        </button>
      </div>
    </section>
  )
}

function CompleteStep({ onRestart }: { onRestart: () => void }) {
  return (
    <section className="demo-complete">
      <div className="demo-complete__mark" aria-hidden="true" />
      <p className="demo-eyebrow">Study complete</p>
      <h1>Thank you for participating</h1>
      <p>
        Your sample responses have been submitted. You may now close this window.
      </p>
      <div className="demo-reference">
        <span>Completion reference</span>
        <strong>DMBS-7K4P9</strong>
      </div>
      <button className="demo-button demo-button--quiet" onClick={onRestart} type="button">
        Restart interface demo
      </button>
    </section>
  )
}

function ParticipantDemo() {
  const [currentStep, setCurrentStep] = useState<DemoStep>('introduction')
  const currentIndex = steps.findIndex((step) => step.id === currentStep)

  const goNext = () => {
    if (currentStep === 'questionnaire') {
      setCurrentStep('complete')
      return
    }

    const next = steps[currentIndex + 1]
    if (next) setCurrentStep(next.id)
  }

  const goBack = () => {
    const previous = steps[currentIndex - 1]
    if (previous) setCurrentStep(previous.id)
  }

  if (currentStep === 'experience') {
    return <ExperienceStep onNext={goNext} />
  }

  return (
    <div className="participant-demo">
      <DemoHeader currentStep={currentStep} />

      <div className="participant-demo__body">
        {currentStep !== 'complete' && <StepRail currentStep={currentStep} />}

        <main className="participant-demo__main">
          {currentStep === 'introduction' && <IntroductionStep onNext={goNext} />}
          {currentStep === 'consent' && <ConsentStep onBack={goBack} onNext={goNext} />}
          {currentStep === 'calibration' && <CalibrationStep onBack={goBack} onNext={goNext} />}
          {currentStep === 'questionnaire' && <QuestionnaireStep onBack={goBack} onNext={goNext} />}
          {currentStep === 'complete' && (
            <CompleteStep onRestart={() => setCurrentStep('introduction')} />
          )}
        </main>
      </div>

      {currentStep !== 'complete' && (
        <footer className="demo-footer">
          <span>Participant information</span>
          <span>Privacy</span>
          <span>Contact the research team</span>
        </footer>
      )}
    </div>
  )
}

export default ParticipantDemo
