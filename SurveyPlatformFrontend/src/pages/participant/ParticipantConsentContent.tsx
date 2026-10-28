export const consentAcknowledgement = 'I have read and understood the information above, including what data is collected, how it is collected, and the withdrawal arrangements, and I voluntarily agree to participate in this study.'

export default function ParticipantConsentContent() {
  return (
    <div className="demo-consent-grid" lang="en">
      <article>
        <span>01</span>
        <div>
          <h2>Data collection and methods</h2>
          <p>Depending on the study settings, the system will collect the following information when you participate:</p>
          <p><strong>Participation records:</strong> The system automatically generates an anonymous participant identifier and records your consent or refusal, progress through each stage, completion or withdrawal status, and associated times.</p>
          <p><strong>Device information:</strong> Your browser provides information about the browser, operating system or platform, screen dimensions and time zone, as well as the page viewport dimensions, scaling ratio and scroll position.</p>
          <p><strong>Browsing behaviour:</strong> Event listeners within the study test page record mouse click positions, times and associated content components, as well as keyboard key codes and times.</p>
          <p><strong>Eye-tracking information:</strong> When eye tracking is enabled, your camera is used with your permission to analyse facial and eye images within your browser and estimate your gaze position. The system records gaze coordinates, times, associated content areas, calibration results and quality information.</p>
          <p><strong>Questionnaire responses:</strong> The system collects the choices, ratings and written answers you provide. Responses are submitted and saved as you answer, and completion is recorded after final submission.</p>
        </div>
      </article>
      <article>
        <span>02</span>
        <div>
          <h2>Privacy and data use</h2>
          <p>The information collected is used for research analysis and linked through an anonymous participant identifier. You do not need to sign in to a personal social media account. Please do not enter passwords or sensitive personal information unrelated to the study.</p>
          <p>Camera images are used only for gaze estimation within your browser. <strong>Camera photographs, raw video and audio are not uploaded to or stored on the research server.</strong> Research data such as gaze coordinates and calibration results is uploaded instead.</p>
        </div>
      </article>
      <article>
        <span>03</span>
        <div>
          <h2>Voluntary participation and withdrawal</h2>
          <p>Participation is entirely voluntary. You may decline to participate, skip eye-tracking calibration, or stop during the study. Please stop if you experience eye strain or any other discomfort.</p>
          <p><strong>Stopping participation does not automatically delete data that has already been saved.</strong></p>
        </div>
      </article>
    </div>
  )
}
