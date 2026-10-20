import { useEffect, useRef } from 'react'

import './eyeTracking.css'

// M6: the participant's own camera view inside the set-up screen's preview panel, with the face
// guide drawn over it. It is mirrored, as people expect from a camera that faces them. The video
// is only displayed here; nothing from it is uploaded.

function CameraPreview({ stream }: { stream: MediaStream }) {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return
    video.srcObject = stream
    try {
      // Muted inline video may play without a gesture; if play() is refused the frame stays still.
      void Promise.resolve(video.play()).catch(() => {})
    } catch {
      /* Environments without media playback keep the element without a picture. */
    }
    return () => {
      if (video.srcObject === stream) video.srcObject = null
    }
  }, [stream])

  return (
    <div className="et-camera-preview">
      <video
        ref={videoRef}
        className="et-camera-preview__video"
        aria-label="Live camera preview"
        autoPlay
        muted
        playsInline
      />
      <span className="et-camera-preview__guide" aria-hidden="true" />
    </div>
  )
}

export default CameraPreview
