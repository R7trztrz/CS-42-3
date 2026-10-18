// M6: the camera check on the set-up screen, before calibration. It opens a plain camera stream
// so the participant can see themselves in the face guide and adjust their position. The video
// stays in the page. Calibration opens its own stream through WebGazer, so this one is closed
// before calibration starts, and whenever the screen that holds it goes away.

import { useCallback, useEffect, useRef, useState } from 'react'

export type CameraPreviewFailure = 'BLOCKED' | 'NO_CAMERA' | 'IN_USE' | 'FAILED'

export type CameraPreviewState =
  | { readonly status: 'off' }
  | { readonly status: 'starting' }
  | { readonly status: 'on'; readonly stream: MediaStream }
  | { readonly status: 'failed'; readonly failure: CameraPreviewFailure }

export type CameraAccess = Pick<MediaDevices, 'getUserMedia'>

function classify(error: unknown): CameraPreviewFailure {
  const name = (error as { name?: unknown } | null)?.name
  if (name === 'NotAllowedError' || name === 'SecurityError') return 'BLOCKED'
  if (name === 'NotFoundError' || name === 'OverconstrainedError') return 'NO_CAMERA'
  if (name === 'NotReadableError' || name === 'AbortError') return 'IN_USE'
  return 'FAILED'
}

function close(stream: MediaStream | null) {
  for (const track of stream?.getTracks() ?? []) track.stop()
}

export function useCameraPreview(camera: CameraAccess | undefined = globalThis.navigator?.mediaDevices) {
  const [state, setState] = useState<CameraPreviewState>({ status: 'off' })
  const streamRef = useRef<MediaStream | null>(null)
  // Every start and stop takes a new number, so a camera that answers after the participant
  // moved on is closed instead of shown.
  const requestRef = useRef(0)

  const stop = useCallback(() => {
    requestRef.current += 1
    close(streamRef.current)
    streamRef.current = null
    setState({ status: 'off' })
  }, [])

  const start = useCallback(async () => {
    requestRef.current += 1
    const request = requestRef.current
    close(streamRef.current)
    streamRef.current = null
    if (!camera?.getUserMedia) {
      setState({ status: 'failed', failure: 'FAILED' })
      return
    }
    setState({ status: 'starting' })
    try {
      const stream = await camera.getUserMedia({ video: { facingMode: 'user' }, audio: false })
      if (request !== requestRef.current) {
        close(stream)
        return
      }
      streamRef.current = stream
      // The camera can be taken away while the check is open (unplugged, or claimed elsewhere).
      for (const track of stream.getVideoTracks()) {
        track.addEventListener?.('ended', () => {
          if (streamRef.current !== stream) return
          close(stream)
          streamRef.current = null
          setState({ status: 'failed', failure: 'IN_USE' })
        })
      }
      setState({ status: 'on', stream })
    } catch (error) {
      if (request === requestRef.current) setState({ status: 'failed', failure: classify(error) })
    }
  }, [camera])

  useEffect(() => {
    const requests = requestRef
    const streams = streamRef
    return () => {
      requests.current += 1
      close(streams.current)
      streams.current = null
    }
  }, [])

  return { state, start, stop }
}
