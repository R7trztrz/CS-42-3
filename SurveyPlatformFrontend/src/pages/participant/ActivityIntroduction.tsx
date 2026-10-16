type Props = {
  questionnaireEnabled: boolean
  onStart: () => void
}

export default function ActivityIntroduction({ questionnaireEnabled, onStart }: Props) {
  return (
    <section className="demo-reading-panel">
      <p className="demo-eyebrow">Social media activity</p>
      <h1>Ready to explore the feed?</h1>
      <p className="demo-lead">
        You will now enter the study&apos;s social media interface. Browse the content
        naturally, as you would on social media.
      </p>
      <div className="demo-consent-grid">
        <article><span>01</span><div><h2>Enter the activity</h2><p>Select Start activity to open the study interface.</p></div></article>
        <article><span>02</span><div><h2>Browse at your own pace</h2><p>Read the posts and explore the content presented to you.</p></div></article>
        <article><span>03</span><div><h2>Finish browsing</h2><p>{questionnaireEnabled
          ? 'Select Finish browsing when you are ready to continue to the final questionnaire.'
          : 'Select Finish browsing when you are ready to complete the study.'}</p></div></article>
      </div>
      <div className="demo-actions">
        <button type="button" className="demo-button demo-button--primary" onClick={onStart}>Start activity</button>
      </div>
    </section>
  )
}
