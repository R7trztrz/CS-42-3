/**
 * Test tools on the participant page, switched on when the dev server or build runs with
 * VITE_EYE_TRACKING_TEST_MODE=true. They show the live gaze dot while browsing, with an on/off
 * switch above the feed. The dot is always shown during calibration, test mode or not.
 *
 * Never enable this for a real study: a moving dot draws the participant's eye.
 */
export const EYE_TRACKING_TEST_MODE: boolean = import.meta.env.VITE_EYE_TRACKING_TEST_MODE === 'true'
