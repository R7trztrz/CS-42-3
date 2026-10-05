import { useNode } from '@craftjs/core'

import {

  useEffect,

  useRef,

  useState,

} from 'react'

import type { PlatformStyle } from './types'

import { useAssetImage } from './AssetImageContext'

type PostWidgetProps = {

  styleId?: PlatformStyle

  username?: string

  displayName?: string

  time?: string

  avatarSrc?: string

  assetId?: string

  imageSrc?: string

  caption?: string

  likes?: number

  comments?: number

  shares?: number

  views?: number

  saves?: number

  sound?: string

}

type SocialIconName =

  | 'heart'

  | 'comment'

  | 'send'

  | 'bookmark'

  | 'share'

  | 'thumb'

  | 'more'

  | 'music'

  | 'up'

  | 'down'

type SocialIconProps = {

  name: SocialIconName

  size?: number

}

/*

 * =========================================================

 * Facebook icons

 * =========================================================

 */

function FacebookIcon({

  name,

  size = 20,

}: SocialIconProps) {

  if (name === 'thumb') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M7.2 10.2v9.6H4.3v-9.6h2.9Z"

          stroke="currentColor"

          strokeWidth="1.7"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

        <path

          d="M7.2 19.2h8.9a2.2 2.2 0 0 0 2.1-1.55l1.7-5.2A2.15 2.15 0 0 0 17.85 9.6h-4.1l.55-2.8a2.15 2.15 0 0 0-4.05-1.4L7.2 10.2Z"

          stroke="currentColor"

          strokeWidth="1.7"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  if (name === 'comment') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M5 5.5h14A3 3 0 0 1 22 8.5v6A3 3 0 0 1 19 17.5h-7.5L6.4 20.6l.8-3.1H5a3 3 0 0 1-3-3v-6a3 3 0 0 1 3-3Z"

          stroke="currentColor"

          strokeWidth="1.7"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  if (name === 'share') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <circle

          cx="18"

          cy="5"

          r="2.3"

          stroke="currentColor"

          strokeWidth="1.7"

        />

        <circle

          cx="6"

          cy="12"

          r="2.3"

          stroke="currentColor"

          strokeWidth="1.7"

        />

        <circle

          cx="18"

          cy="19"

          r="2.3"

          stroke="currentColor"

          strokeWidth="1.7"

        />

        <path

          d="m8.1 10.9 7.7-4.5"

          stroke="currentColor"

          strokeWidth="1.7"

          strokeLinecap="round"

        />

        <path

          d="m8.1 13.1 7.7 4.5"

          stroke="currentColor"

          strokeWidth="1.7"

          strokeLinecap="round"

        />

      </svg>

    )

  }

  return null

}

/*

 * =========================================================

 * Instagram icons

 * =========================================================

 */

function InstagramIcon({

  name,

  size = 29,

}: SocialIconProps) {

  /*

   * Thin rounded Instagram-style heart.

   */

  if (name === 'heart') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M12 20.45 4.68 13.4C1.53 10.36 3.08 5.08 7.34 5.08c2.03 0 3.58 1.13 4.66 2.54 1.08-1.41 2.63-2.54 4.66-2.54 4.26 0 5.81 5.28 2.66 8.32L12 20.45Z"

          stroke="currentColor"

          strokeWidth="1.55"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  /*

   * Rounded speech bubble close to the prototype.

   */

  if (name === 'comment') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M12 3.65c5 0 9 3.48 9 7.9 0 4.43-4 7.95-9 7.95-1.38 0-2.7-.27-3.87-.77L3.7 20.2l1.28-3.92A7.4 7.4 0 0 1 3 11.55c0-4.42 4-7.9 9-7.9Z"

          stroke="currentColor"

          strokeWidth="1.55"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  /*

   * Instagram paper-plane share.

   */

  if (name === 'send') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M21.4 3.2 3.15 10.42l7.3 3.15 3.18 7.25L21.4 3.2Z"

          stroke="currentColor"

          strokeWidth="1.55"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

        <path

          d="M10.45 13.57 21.4 3.2"

          stroke="currentColor"

          strokeWidth="1.55"

          strokeLinecap="round"

        />

      </svg>

    )

  }

  /*

   * Tall narrow bookmark like the prototype.

   */

  if (name === 'bookmark') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M7 3.3h10a1 1 0 0 1 1 1v16.4L12 16.75 6 20.7V4.3a1 1 0 0 1 1-1Z"

          stroke="currentColor"

          strokeWidth="1.55"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  if (name === 'more') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <circle

          cx="5"

          cy="12"

          r="1.25"

          fill="currentColor"

        />

        <circle

          cx="12"

          cy="12"

          r="1.25"

          fill="currentColor"

        />

        <circle

          cx="19"

          cy="12"

          r="1.25"

          fill="currentColor"

        />

      </svg>

    )

  }

  return null

}

/*

 * =========================================================

 * TikTok icons

 * =========================================================

 */

function TikTokPostIcon({

  name,

  size = 27,

}: SocialIconProps) {

  if (name === 'heart') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M12 20.8 4.55 13.55C1.28 10.37 2.98 4.95 7.33 4.95c2.08 0 3.67 1.2 4.67 2.64 1-1.44 2.59-2.64 4.67-2.64 4.35 0 6.05 5.42 2.78 8.6L12 20.8Z"

          stroke="currentColor"

          strokeWidth="2.15"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  /*

   * TikTok-style rectangular bubble with two lines.

   */

  if (name === 'comment') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M5.3 4.8h13.4A3.3 3.3 0 0 1 22 8.1v5.2a3.3 3.3 0 0 1-3.3 3.3h-6.2L8 19.7v-3.1H5.3A3.3 3.3 0 0 1 2 13.3V8.1a3.3 3.3 0 0 1 3.3-3.3Z"

          stroke="currentColor"

          strokeWidth="2.1"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

        <path

          d="M7.1 9.7h9.8"

          stroke="currentColor"

          strokeWidth="2"

          strokeLinecap="round"

        />

        <path

          d="M7.1 12.9h6.6"

          stroke="currentColor"

          strokeWidth="2"

          strokeLinecap="round"

        />

      </svg>

    )

  }

  /*

   * Narrow TikTok bookmark.

   */

  if (name === 'bookmark') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M7.2 3.2h9.6c.66 0 1.2.54 1.2 1.2v16.4L12 16.65 6 20.8V4.4c0-.66.54-1.2 1.2-1.2Z"

          stroke="currentColor"

          strokeWidth="2.05"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  /*

   * Three-node TikTok share.

   */

  if (name === 'share') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <circle

          cx="18"

          cy="4.8"

          r="2.4"

          stroke="currentColor"

          strokeWidth="2"

        />

        <circle

          cx="5.8"

          cy="12"

          r="2.4"

          stroke="currentColor"

          strokeWidth="2"

        />

        <circle

          cx="18"

          cy="19.2"

          r="2.4"

          stroke="currentColor"

          strokeWidth="2"

        />

        <path

          d="m8 10.7 7.7-4.5"

          stroke="currentColor"

          strokeWidth="2"

          strokeLinecap="round"

        />

        <path

          d="m8 13.3 7.7 4.5"

          stroke="currentColor"

          strokeWidth="2"

          strokeLinecap="round"

        />

      </svg>

    )

  }

  if (name === 'music') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="M9 18V6l10-2v12"

          stroke="currentColor"

          strokeWidth="1.9"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

        <circle

          cx="6.5"

          cy="18"

          r="2.4"

          stroke="currentColor"

          strokeWidth="1.9"

        />

        <circle

          cx="16.5"

          cy="16"

          r="2.4"

          stroke="currentColor"

          strokeWidth="1.9"

        />

      </svg>

    )

  }

  if (name === 'up') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="m5.5 15.3 6.5-6.6 6.5 6.6"

          stroke="currentColor"

          strokeWidth="2.35"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  if (name === 'down') {

    return (

      <svg

        width={size}

        height={size}

        viewBox="0 0 24 24"

        fill="none"

      >

        <path

          d="m5.5 8.7 6.5 6.6 6.5-6.6"

          stroke="currentColor"

          strokeWidth="2.35"

          strokeLinecap="round"

          strokeLinejoin="round"

        />

      </svg>

    )

  }

  return null

}

/*
 * =========================================================
 * X icons
 * =========================================================
 */

type XPostIconName =
  | 'comment'
  | 'repost'
  | 'heart'
  | 'views'
  | 'bookmark'
  | 'share'
  | 'more'
  | 'verified'

function XPostIcon({
  name,
  size = 23,
}: {
  name: XPostIconName
  size?: number
}) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.9,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  }

  if (name === 'comment') {
    return (
      <svg {...common}>
        <path d="M20.1 11.7c0 4.25-3.6 7.55-8.1 7.55-1.2 0-2.35-.22-3.4-.62L4.3 19.8l1.28-3.7a7.05 7.05 0 0 1-1.68-4.4c0-4.25 3.6-7.55 8.1-7.55s8.1 3.3 8.1 7.55Z" />
      </svg>
    )
  }

  if (name === 'repost') {
    return (
      <svg {...common}>
        <path d="M7.2 7.2h9.2l-2.5-2.5" />
        <path d="m16.4 7.2 2.5-2.5" />
        <path d="M16.8 16.8H7.6l2.5 2.5" />
        <path d="m7.6 16.8-2.5 2.5" />
        <path d="M7.2 7.2 5.2 9.4v3.1" />
        <path d="m16.8 16.8 2-2.2v-3.1" />
      </svg>
    )
  }

  if (name === 'heart') {
    return (
      <svg {...common}>
        <path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" />
      </svg>
    )
  }

  if (name === 'views') {
    return (
      <svg {...common}>
        <path d="M2.8 12s3.4-5.5 9.2-5.5S21.2 12 21.2 12s-3.4 5.5-9.2 5.5S2.8 12 2.8 12Z" />
        <circle cx="12" cy="12" r="2.55" />
      </svg>
    )
  }

  if (name === 'bookmark') {
    return (
      <svg {...common}>
        <path d="M7 3.5h10v17l-5-3.4-5 3.4z" />
      </svg>
    )
  }

  if (name === 'share') {
    return (
      <svg {...common}>
        <circle cx="18" cy="5" r="2.15" />
        <circle cx="6" cy="12" r="2.15" />
        <circle cx="18" cy="19" r="2.15" />
        <path d="m7.9 10.9 8.1-4.7" />
        <path d="m7.9 13.1 8.1 4.7" />
      </svg>
    )
  }

  if (name === 'verified') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-label="Verified">
        <path d="M12 2.7 14.2 4l2.55-.05.9 2.4 2.2 1.3-.7 2.45.7 2.45-2.2 1.3-.9 2.4-2.55-.05L12 17.3l-2.2-1.3-2.55.05-.9-2.4-2.2-1.3.7-2.45-.7-2.45 2.2-1.3.9-2.4L9.8 4 12 2.7Z" fill="#1d9bf0" />
        <path d="m8.9 10.1 2 2 4.2-4.5" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  }

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
      <circle cx="6.5" cy="12" r="1.5" />
      <circle cx="12" cy="12" r="1.5" />
      <circle cx="17.5" cy="12" r="1.5" />
    </svg>
  )
}

/*
 * =========================================================
 * Truth Social icons
 * =========================================================
 */
type TruthPostIconName = 'comment' | 'retruth' | 'heart' | 'bookmark' | 'share' | 'more'

function TruthPostIcon({ name, size = 20 }: { name: TruthPostIconName; size?: number }) {
  const common = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth: 1.8,
    strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const,
  }
  if (name === 'comment') return <svg {...common}><path d="M20.2 11.8c0 4.3-3.7 7.6-8.2 7.6-1.2 0-2.4-.2-3.5-.6L4.2 20l1.3-3.8a7.1 7.1 0 0 1-1.7-4.4c0-4.3 3.7-7.6 8.2-7.6s8.2 3.3 8.2 7.6Z" /></svg>
  if (name === 'retruth') return <svg {...common}><path d="M7.2 7.3h9l-2.4-2.4" /><path d="m16.2 7.3 2.5-2.5" /><path d="M16.8 16.7h-9l2.4 2.4" /><path d="m7.8 16.7-2.5 2.5" /><path d="M7.2 7.3 5.2 9.5v3" /><path d="m16.8 16.7 2-2.2v-3" /></svg>
  if (name === 'heart') return <svg {...common}><path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" /></svg>
  if (name === 'bookmark') return <svg {...common}><path d="M7 3.5h10v17l-5-3.4-5 3.4z" /></svg>
  if (name === 'share') return <svg {...common}><circle cx="18" cy="5" r="2.1" /><circle cx="6" cy="12" r="2.1" /><circle cx="18" cy="19" r="2.1" /><path d="m7.9 10.9 8.2-4.7" /><path d="m7.9 13.1 8.2 4.7" /></svg>
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor"><circle cx="6.5" cy="12" r="1.35" /><circle cx="12" cy="12" r="1.35" /><circle cx="17.5" cy="12" r="1.35" /></svg>
}

/*
 * =========================================================
 * Bluesky icons
 * =========================================================
 */
type BlueskyPostIconName = 'comment' | 'repost' | 'heart' | 'bookmark' | 'share' | 'more'

function BlueskyPostIcon({ name, size = 20 }: { name: BlueskyPostIconName; size?: number }) {
  const common = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth: 1.75,
    strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const,
  }
  if (name === 'comment') return <svg {...common}><path d="M20.2 11.8c0 4.3-3.7 7.6-8.2 7.6-1.2 0-2.4-.2-3.5-.6L4.2 20l1.3-3.8a7.1 7.1 0 0 1-1.7-4.4c0-4.3 3.7-7.6 8.2-7.6s8.2 3.3 8.2 7.6Z" /></svg>
  if (name === 'repost') return <svg {...common}><path d="M7.2 7.3h9l-2.4-2.4" /><path d="m16.2 7.3 2.5-2.5" /><path d="M16.8 16.7h-9l2.4 2.4" /><path d="m7.8 16.7-2.5 2.5" /><path d="M7.2 7.3 5.2 9.5v3" /><path d="m16.8 16.7 2-2.2v-3" /></svg>
  if (name === 'heart') return <svg {...common}><path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" /></svg>
  if (name === 'bookmark') return <svg {...common}><path d="M7 3.5h10v17l-5-3.4-5 3.4z" /></svg>
  if (name === 'share') return <svg {...common}><circle cx="18" cy="5" r="2.1" /><circle cx="6" cy="12" r="2.1" /><circle cx="18" cy="19" r="2.1" /><path d="m7.9 10.9 8.2-4.7" /><path d="m7.9 13.1 8.2 4.7" /></svg>
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor"><circle cx="6.5" cy="12" r="1.35" /><circle cx="12" cy="12" r="1.35" /><circle cx="17.5" cy="12" r="1.35" /></svg>
}

/*
 * =========================================================
 * Threads icons
 * =========================================================
 */
type ThreadsPostIconName = 'heart' | 'comment' | 'repost' | 'share' | 'more'
function ThreadsPostIcon({ name, size = 20 }: { name: ThreadsPostIconName; size?: number }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.75, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const }
  if (name === 'heart') return <svg {...common}><path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" /></svg>
  if (name === 'comment') return <svg {...common}><path d="M20.3 11.8c0 4.35-3.68 7.72-8.3 7.72-1.22 0-2.4-.22-3.47-.63L4.1 20.1l1.32-3.8a7.18 7.18 0 0 1-1.72-4.5c0-4.35 3.68-7.72 8.3-7.72s8.3 3.37 8.3 7.72Z" /></svg>
  if (name === 'repost') return <svg {...common}><path d="M7.2 7.3h9l-2.4-2.4" /><path d="m16.2 7.3 2.5-2.5" /><path d="M16.8 16.7h-9l2.4 2.4" /><path d="m7.8 16.7-2.5 2.5" /><path d="M7.2 7.3 5.2 9.5v3" /><path d="m16.8 16.7 2-2.2v-3" /></svg>
  if (name === 'share') return <svg {...common}><circle cx="18" cy="5" r="2.1" /><circle cx="6" cy="12" r="2.1" /><circle cx="18" cy="19" r="2.1" /><path d="m7.9 10.9 8.2-4.7" /><path d="m7.9 13.1 8.2 4.7" /></svg>
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor"><circle cx="6.5" cy="12" r="1.35" /><circle cx="12" cy="12" r="1.35" /><circle cx="17.5" cy="12" r="1.35" /></svg>
}

/*

 * =========================================================

 * More icon

 * =========================================================

 */

function MoreIcon({

  size = 20,

}: {

  size?: number

}) {

  return (

    <svg

      width={size}

      height={size}

      viewBox="0 0 24 24"

      fill="none"

    >

      <circle

        cx="5"

        cy="12"

        r="1.4"

        fill="currentColor"

      />

      <circle

        cx="12"

        cy="12"

        r="1.4"

        fill="currentColor"

      />

      <circle

        cx="19"

        cy="12"

        r="1.4"

        fill="currentColor"

      />

    </svg>

  )

}

/*

 * =========================================================

 * Helpers

 * =========================================================

 */

function getInitials(

  displayName: string,

) {

  return displayName

    .split(' ')

    .map((part) =>

      part.charAt(0),

    )

    .join('')

    .slice(0, 2)

    .toUpperCase()

}

/*

 * =========================================================

 * Post Widget

 * =========================================================

 */

function PostWidget({

  styleId = 'facebook',

  username = '@emmaw',

  displayName = 'Emma Wilson',

  time = '2h ago',

  avatarSrc = '',

  assetId,

  imageSrc = '',

  caption =

    'Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂',

  likes = 47,

  comments = 12,

  shares = 8,

  views = 1243,

  saves = 5,

  sound =

    'Original Sound · Emma Wilson',

}: PostWidgetProps) {

  const assetImageSrc =

    useAssetImage(assetId)

  const displayedImageSrc =

    imageSrc || assetImageSrc

  const {

    connectors: {

      connect,

      drag,

    },

    selected,

    nodeId,

  } = useNode((node) => ({

    selected:

      node.events.selected,

    nodeId:

      node.id,

  }))

  const postRef =

    useRef<HTMLDivElement | null>(

      null,

    )

  const [

    hasPreviousPost,

    setHasPreviousPost,

  ] = useState(false)

  const [

    hasNextPost,

    setHasNextPost,

  ] = useState(false)

  const initials =

    getInitials(

      displayName,

    )

  /*

   * =========================================================

   * Detect previous / next TikTok post

   * =========================================================

   */

  useEffect(() => {

    if (

      styleId !== 'tiktok'

    ) {

      return

    }

    const updateNavigationState =

      () => {

        const current =

          postRef.current

        if (!current) {

          setHasPreviousPost(

            false,

          )

          setHasNextPost(

            false,

          )

          return

        }

        setHasPreviousPost(

          Boolean(

            current.previousElementSibling,

          ),

        )

        setHasNextPost(

          Boolean(

            current.nextElementSibling,

          ),

        )

      }

    updateNavigationState()

    const parent =

      postRef.current

        ?.parentElement

    if (!parent) {

      return

    }

    const observer =

      new MutationObserver(

        updateNavigationState,

      )

    observer.observe(

      parent,

      {

        childList: true,

      },

    )

    return () => {

      observer.disconnect()

    }

  }, [

    styleId,

  ])

  /*

   * =========================================================

   * TikTok navigation

   * =========================================================

   */

  const handlePreviousVideo =

    () => {

      const current =

        postRef.current

      if (!current) {

        return

      }

      const previousPost =

        current.previousElementSibling as HTMLElement | null

      if (!previousPost) {

        return

      }

      previousPost.scrollIntoView({

        behavior: 'smooth',

        block: 'center',

      })

    }

  const handleNextVideo =

    () => {

      const current =

        postRef.current

      if (!current) {

        return

      }

      const nextPost =

        current.nextElementSibling as HTMLElement | null

      if (!nextPost) {

        return

      }

      nextPost.scrollIntoView({

        behavior: 'smooth',

        block: 'center',

      })

    }

  /*

   * =========================================================

   * TikTok

   * =========================================================

   */

  if (

    styleId === 'tiktok'

  ) {

    return (

      <div

        ref={(ref) => {

          postRef.current =

            ref

          if (ref) {

            connect(

              drag(ref),

            )

          }

        }}

        data-component-id={nodeId}

        data-component-type="PostWidget"

        className={`flex w-full cursor-move items-end justify-center gap-5 ${

          selected

            ? 'rounded-xl ring-2 ring-emerald-400 ring-offset-2 ring-offset-[#0f0f0f]'

            : ''

        }`}

      >

        {/* Video / image card */}

        <article
          data-component-id={`${nodeId}:media`}
          data-parent-component-id={nodeId}
          data-component-type="PostMedia"
          className="relative h-[630px] w-[355px] shrink-0 overflow-hidden rounded-[18px] bg-[#181818] shadow-2xl"
        >

          {displayedImageSrc ? (

            <img

              src={displayedImageSrc}

              alt="TikTok post"

              className="absolute inset-0 h-full w-full object-cover"

            />

          ) : (

            <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-b from-[#d9dbde] via-[#afb2b6] to-[#252525]">

              <div className="text-center text-[#555]">

                <div className="text-4xl">

                  🖼️

                </div>

                <p className="mt-2 text-[12px] font-medium">

                  Media placeholder

                </p>

              </div>

            </div>

          )}

          {/* Dark readability gradient */}

          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-[50%] bg-gradient-to-t from-black/95 via-black/45 to-transparent" />

          {/* TikTok caption */}

          <div
            data-component-id={`${nodeId}:caption`}
            data-parent-component-id={nodeId}
            data-component-type="PostCaption"
            className="absolute inset-x-0 bottom-0 z-10 px-4 pb-5 text-white"
          >

            <p
              data-component-id={`${nodeId}:author`}
              data-parent-component-id={nodeId}
              data-component-type="PostAuthor"
              className="text-[14px] font-bold"
            >
              {username}
            </p>

            <p className="mt-2 line-clamp-2 text-[13px] leading-5">

              {caption}

            </p>

            <div
              data-component-id={`${nodeId}:sound`}
              data-parent-component-id={nodeId}
              data-component-type="PostSound"
              className="mt-3 inline-flex max-w-full items-center gap-2 rounded-full bg-black/45 px-3 py-2"
            >

              <TikTokPostIcon

                name="music"

                size={16}

              />

              <span className="max-w-[245px] truncate text-[11px]">

                {sound}

              </span>

            </div>

            <div className="mt-4 flex justify-center gap-1">

              <span className="h-[3px] w-5 rounded-full bg-white" />

              <span className="h-[3px] w-2 rounded-full bg-white/40" />

              <span className="h-[3px] w-2 rounded-full bg-white/40" />

              <span className="h-[3px] w-2 rounded-full bg-white/40" />

            </div>

          </div>

        </article>

        {/* TikTok right rail */}

        <aside className="flex w-[82px] shrink-0 flex-col items-center justify-end gap-[14px] pb-1 text-white">

          {/* Previous video */}

          <button

            type="button"

            disabled={

              !hasPreviousPost

            }

            onClick={(

              event,

            ) => {

              event.stopPropagation()

              handlePreviousVideo()

            }}

            onMouseDown={(

              event,

            ) => {

              event.stopPropagation()

            }}

            className={`flex h-[52px] w-[52px] items-center justify-center rounded-full transition ${

              hasPreviousPost

                ? 'cursor-pointer bg-[#252525] text-white hover:bg-[#333333]'

                : 'cursor-not-allowed bg-[#202020] text-[#666]'

            }`}

            title={

              hasPreviousPost

                ? 'Previous video'

                : 'No previous video'

            }

          >

            <TikTokPostIcon

              name="up"

              size={27}

            />

          </button>

          {/* Avatar */}

          <div
            data-component-id={`${nodeId}:avatar`}
            data-parent-component-id={nodeId}
            data-component-type="PostAvatar"
            className="relative"
          >
            <div className="h-[54px] w-[54px] overflow-hidden rounded-full border-2 border-white">

              {avatarSrc ? (

                <img

                  src={avatarSrc}

                  alt={displayName}

                  className="h-full w-full object-cover"

                />

              ) : (

                <div className="flex h-full w-full items-center justify-center bg-[#5447e8] text-[13px] font-bold text-white">

                  {initials}

                </div>

              )}

            </div>

            <div className="absolute -bottom-1 left-1/2 flex h-[18px] w-[18px] -translate-x-1/2 items-center justify-center rounded-full bg-[#fe2c55] text-[12px] font-bold text-white">

              +

            </div>

          </div>

          {/* Likes */}

          <div
            data-component-id={`${nodeId}:like`}
            data-parent-component-id={nodeId}
            data-component-type="PostLike"
            className="flex flex-col items-center"
          >

            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-full bg-[#252525]">

              <TikTokPostIcon

                name="heart"

                size={30}

              />

            </div>

            <span className="mt-1 text-[11px] font-bold">

              {likes}

            </span>

          </div>

          {/* Comments */}

          <div
            data-component-id={`${nodeId}:comment`}
            data-parent-component-id={nodeId}
            data-component-type="PostComment"
            className="flex flex-col items-center"
          >

            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-full bg-[#252525]">

              <TikTokPostIcon

                name="comment"

                size={29}

              />

            </div>

            <span className="mt-1 text-[11px] font-bold">

              {comments}

            </span>

          </div>

          {/* Save */}

          <div
            data-component-id={`${nodeId}:bookmark`}
            data-parent-component-id={nodeId}
            data-component-type="PostBookmark"
            className="flex flex-col items-center"
          >

            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-full bg-[#252525]">

              <TikTokPostIcon

                name="bookmark"

                size={28}

              />

            </div>

            <span className="mt-1 text-[11px] font-bold">

              {saves}

            </span>

          </div>

          {/* Share */}

          <div
            data-component-id={`${nodeId}:share`}
            data-parent-component-id={nodeId}
            data-component-type="PostShare"
            className="flex flex-col items-center"
          >

            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-full bg-[#252525]">

              <TikTokPostIcon

                name="share"

                size={28}

              />

            </div>

            <span className="mt-1 text-[11px] font-bold">

              {shares}

            </span>

          </div>

          {/* Record */}

          <div className="flex h-[54px] w-[54px] items-center justify-center rounded-full bg-[#252525]">

            <div className="flex h-[36px] w-[36px] items-center justify-center rounded-full bg-gradient-to-br from-[#171717] via-[#382183] to-[#090909] ring-4 ring-[#151515]">

              <div className="h-[10px] w-[10px] rounded-full bg-[#6a45ff]" />

            </div>

          </div>

          {/* Next video */}

          <button

            type="button"

            disabled={

              !hasNextPost

            }

            onClick={(

              event,

            ) => {

              event.stopPropagation()

              handleNextVideo()

            }}

            onMouseDown={(

              event,

            ) => {

              event.stopPropagation()

            }}

            className={`flex h-[52px] w-[52px] items-center justify-center rounded-full transition ${

              hasNextPost

                ? 'cursor-pointer bg-[#252525] text-white hover:bg-[#333333]'

                : 'cursor-not-allowed bg-[#202020] text-[#666]'

            }`}

            title={

              hasNextPost

                ? 'Next video'

                : 'No next video'

            }

          >

            <TikTokPostIcon

              name="down"

              size={27}

            />

          </button>

        </aside>

      </div>

    )

  }

  /*
 * =========================================================
 * X
 * =========================================================
 */

  if (styleId === 'x') {
    const avatarColor =
      displayName === 'Emma Wilson'
        ? '#5b49ff'
        : displayName === 'Alex Chen'
          ? '#13a8d1'
          : '#00a87f'

    return (
      <article
        ref={(ref) => {
          if (ref) {
            connect(
              drag(ref),
            )
          }
        }}
        data-component-id={nodeId}
        data-component-type="PostWidget"
        className={`w-full cursor-move border-b border-[#2f3336] bg-black px-4 py-3 text-[#e7e9ea] ${
          selected
            ? 'ring-2 ring-[#1d9bf0] ring-inset'
            : ''
        }`}
      >
        <div className="flex gap-3">
          <div
            data-component-id={`${nodeId}:avatar`}
            data-parent-component-id={nodeId}
            data-component-type="PostAvatar"
            className="h-[48px] w-[48px] shrink-0 overflow-hidden rounded-full"
          >
            {avatarSrc ? (
              <img
                src={avatarSrc}
                alt={displayName}
                className="h-full w-full object-cover"
              />
            ) : (
              <div
                className="flex h-full w-full items-center justify-center text-[12px] font-bold text-white"
                style={{ background: avatarColor }}
              >
                {initials}
              </div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <div
                data-component-id={`${nodeId}:author`}
                data-parent-component-id={nodeId}
                data-component-type="PostAuthor"
                className="flex min-w-0 flex-wrap items-center gap-x-1 text-[15px] leading-5"
              >
                <span className="truncate font-bold text-[#e7e9ea]">
                  {displayName}
                </span>
                <span className="shrink-0 text-[#1d9bf0]">
                  <XPostIcon name="verified" size={18} />
                </span>
                <span className="truncate text-[#71767b]">
                  {username}
                </span>
                <span className="text-[#71767b]">·</span>
                <span className="shrink-0 text-[#71767b]">
                  {time}
                </span>
              </div>

              <span
                data-component-id={`${nodeId}:more`}
                data-parent-component-id={nodeId}
                data-component-type="PostMore"
                className="shrink-0 text-[#71767b]"
              >
                <XPostIcon name="more" size={18} />
              </span>
            </div>

            <p
              data-component-id={`${nodeId}:caption`}
              data-parent-component-id={nodeId}
              data-component-type="PostCaption"
              className="mt-1 whitespace-pre-wrap text-[15px] leading-[1.45] text-[#e7e9ea]"
            >
              {caption}
            </p>

            <div
              data-component-id={`${nodeId}:media`}
              data-parent-component-id={nodeId}
              data-component-type="PostMedia"
              className="mt-3 aspect-video w-full overflow-hidden rounded-[18px] border border-[#2f3336] bg-[#16181c]"
            >
              {displayedImageSrc ? (
                <img
                  src={displayedImageSrc}
                  alt="Post"
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full w-full flex-col items-center justify-center text-[#71767b]">
                  <div className="text-[34px]">🖼️</div>
                  <p className="mt-3 text-[14px]">
                    Media placeholder
                  </p>
                </div>
              )}
            </div>

            <div className="mt-[12px] grid grid-cols-[repeat(4,minmax(0,1fr))_48px_48px] items-center text-[#71767b]">
              <div
                data-component-id={`${nodeId}:comment`}
                data-parent-component-id={nodeId}
                data-component-type="PostComment"
                className="flex items-center gap-[7px]"
              >
                <XPostIcon name="comment" size={23} />
                <span className="text-[14px]">{comments}</span>
              </div>

              <div
                data-component-id={`${nodeId}:repost`}
                data-parent-component-id={nodeId}
                data-component-type="PostRepost"
                className="flex items-center gap-[7px]"
              >
                <XPostIcon name="repost" size={23} />
                <span className="text-[14px]">{shares}</span>
              </div>

              <div
                data-component-id={`${nodeId}:like`}
                data-parent-component-id={nodeId}
                data-component-type="PostLike"
                className="flex items-center gap-[7px]"
              >
                <XPostIcon name="heart" size={23} />
                <span className="text-[14px]">{likes}</span>
              </div>

              <div
                data-component-id={`${nodeId}:views`}
                data-parent-component-id={nodeId}
                data-component-type="PostViews"
                className="flex items-center gap-[7px]"
              >
                <XPostIcon name="views" size={23} />
                <span className="text-[14px]">
                  {views.toLocaleString()}
                </span>
              </div>

              <div
                data-component-id={`${nodeId}:bookmark`}
                data-parent-component-id={nodeId}
                data-component-type="PostBookmark"
                className="flex items-center justify-center"
              >
                <XPostIcon name="bookmark" size={23} />
              </div>

              <div
                data-component-id={`${nodeId}:share`}
                data-parent-component-id={nodeId}
                data-component-type="PostShare"
                className="flex items-center justify-end"
              >
                <XPostIcon name="share" size={23} />
              </div>
            </div>
          </div>
        </div>
      </article>
    )
  }

/*
 * =========================================================
 * Truth Social
 * =========================================================
 */
  if (styleId === 'truth-social') {
    const avatarColor =
      displayName === 'Emma Wilson' ? '#5146e5'
        : displayName === 'Alex Chen' ? '#11a4c8'
          : '#00a87f'

    return (
      <article
        ref={(ref) => { if (ref) connect(drag(ref)) }}
        data-component-id={nodeId}
        data-component-type="PostWidget"
        className={`w-full cursor-move border-b border-[#e6eaf0] bg-white px-4 py-4 text-[#0d2345] ${selected ? 'ring-2 ring-[#ef1f2c] ring-inset' : ''}`}
      >
        <div className="flex gap-3">
          <div
            data-component-id={`${nodeId}:avatar`}
            data-parent-component-id={nodeId}
            data-component-type="PostAvatar"
            className="h-[42px] w-[42px] shrink-0 overflow-hidden rounded-full"
          >
            {avatarSrc ? (
              <img src={avatarSrc} alt={displayName} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-[12px] font-bold text-white" style={{ background: avatarColor }}>{initials}</div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-3">
              <div
                data-component-id={`${nodeId}:author`}
                data-parent-component-id={nodeId}
                data-component-type="PostAuthor"
                className="flex min-w-0 flex-wrap items-center gap-x-1.5 text-[14px]"
              >
                <span className="font-semibold text-[#0d2345]">{displayName}</span>
                <span className="truncate text-[#8a95a6]">{username}</span>
                <span className="text-[#8a95a6]">·</span>
                <span className="shrink-0 text-[#8a95a6]">{time}</span>
              </div>
              <span
                data-component-id={`${nodeId}:more`}
                data-parent-component-id={nodeId}
                data-component-type="PostMore"
                className="shrink-0 text-[#8a95a6]"
              >
                <TruthPostIcon name="more" size={17} />
              </span>
            </div>

            <p
              data-component-id={`${nodeId}:caption`}
              data-parent-component-id={nodeId}
              data-component-type="PostCaption"
              className="mt-2 whitespace-pre-wrap text-[15px] leading-[1.55] text-[#253b5b]"
            >
              {caption}
            </p>

            <div
              data-component-id={`${nodeId}:media`}
              data-parent-component-id={nodeId}
              data-component-type="PostMedia"
              className="mt-4 aspect-[16/9] w-full overflow-hidden rounded-[14px] bg-[#f2f4f7]"
            >
              {displayedImageSrc ? (
                <img src={displayedImageSrc} alt="Truth media" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full flex-col items-center justify-center text-[#9aa4b3]">
                  <div className="text-[32px]">🖼️</div>
                  <p className="mt-2 text-[13px]">Media placeholder</p>
                </div>
              )}
            </div>

            <div className="mt-3 flex items-center justify-between pr-1 text-[#7d899b]">
              <div
                data-component-id={`${nodeId}:comment`}
                data-parent-component-id={nodeId}
                data-component-type="PostComment"
                className="flex items-center gap-2"
              >
                <TruthPostIcon name="comment" size={20} />
                <span className="text-[13px]">{comments}</span>
              </div>

              <div
                data-component-id={`${nodeId}:retruth`}
                data-parent-component-id={nodeId}
                data-component-type="PostReTruth"
                className="flex items-center gap-2"
              >
                <TruthPostIcon name="retruth" size={20} />
                <span className="text-[13px]">{shares}</span>
              </div>

              <div
                data-component-id={`${nodeId}:like`}
                data-parent-component-id={nodeId}
                data-component-type="PostLike"
                className="flex items-center gap-2"
              >
                <TruthPostIcon name="heart" size={20} />
                <span className="text-[13px]">{likes}</span>
              </div>

              <div
                data-component-id={`${nodeId}:bookmark`}
                data-parent-component-id={nodeId}
                data-component-type="PostBookmark"
                className="flex items-center"
              >
                <TruthPostIcon name="bookmark" size={20} />
              </div>

              <div
                data-component-id={`${nodeId}:share`}
                data-parent-component-id={nodeId}
                data-component-type="PostShare"
                className="flex items-center"
              >
                <TruthPostIcon name="share" size={20} />
              </div>
            </div>
          </div>
        </div>
      </article>
    )
  }

/*
 * =========================================================
 * Bluesky
 * =========================================================
 */
  if (styleId === 'bluesky') {
    const avatarColor =
      displayName === 'Emma Wilson' ? '#5146e5'
        : displayName === 'Alex Chen' ? '#11a4c8'
          : '#00a87f'

    return (
      <article
        ref={(ref) => { if (ref) connect(drag(ref)) }}
        data-component-id={nodeId}
        data-component-type="PostWidget"
        className={`w-full cursor-move border-b border-[#e6edf5] bg-white px-4 py-4 text-[#0f172a] ${selected ? 'ring-2 ring-[#1185fe] ring-inset' : ''}`}
      >
        <div className="flex gap-3">
          <div
            data-component-id={`${nodeId}:avatar`}
            data-parent-component-id={nodeId}
            data-component-type="PostAvatar"
            className="h-[42px] w-[42px] shrink-0 overflow-hidden rounded-full"
          >
            {avatarSrc ? (
              <img src={avatarSrc} alt={displayName} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-[12px] font-bold text-white" style={{ background: avatarColor }}>{initials}</div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-3">
              <div
                data-component-id={`${nodeId}:author`}
                data-parent-component-id={nodeId}
                data-component-type="PostAuthor"
                className="flex min-w-0 flex-wrap items-center gap-x-1.5 text-[14px]"
              >
                <span className="font-semibold text-[#111827]">{displayName}</span>
                <span className="truncate text-[#94a3b8]">{username}</span>
                <span className="text-[#94a3b8]">·</span>
                <span className="shrink-0 text-[#94a3b8]">{time}</span>
              </div>
              <span
                data-component-id={`${nodeId}:more`}
                data-parent-component-id={nodeId}
                data-component-type="PostMore"
                className="shrink-0 text-[#94a3b8]"
              >
                <BlueskyPostIcon name="more" size={17} />
              </span>
            </div>

            <p
              data-component-id={`${nodeId}:caption`}
              data-parent-component-id={nodeId}
              data-component-type="PostCaption"
              className="mt-2 whitespace-pre-wrap text-[15px] leading-[1.55] text-[#1f2937]"
            >
              {caption}
            </p>

            <div
              data-component-id={`${nodeId}:media`}
              data-parent-component-id={nodeId}
              data-component-type="PostMedia"
              className="mt-4 aspect-[16/9] w-full overflow-hidden rounded-[14px] bg-[#f1f5f9]"
            >
              {displayedImageSrc ? (
                <img src={displayedImageSrc} alt="Post" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full flex-col items-center justify-center text-[#94a3b8]">
                  <div className="text-[32px]">🖼️</div>
                  <p className="mt-2 text-[13px]">Media placeholder</p>
                </div>
              )}
            </div>

            <div className="mt-3 flex items-center justify-between pr-1 text-[#7c8a9e]">
              <div
                data-component-id={`${nodeId}:comment`}
                data-parent-component-id={nodeId}
                data-component-type="PostComment"
                className="flex items-center gap-2"
              >
                <BlueskyPostIcon name="comment" size={20} />
                <span className="text-[13px]">{comments}</span>
              </div>
              <div
                data-component-id={`${nodeId}:repost`}
                data-parent-component-id={nodeId}
                data-component-type="PostRepost"
                className="flex items-center gap-2"
              >
                <BlueskyPostIcon name="repost" size={20} />
                <span className="text-[13px]">{shares}</span>
              </div>
              <div
                data-component-id={`${nodeId}:like`}
                data-parent-component-id={nodeId}
                data-component-type="PostLike"
                className="flex items-center gap-2"
              >
                <BlueskyPostIcon name="heart" size={20} />
                <span className="text-[13px]">{likes}</span>
              </div>
              <div
                data-component-id={`${nodeId}:bookmark`}
                data-parent-component-id={nodeId}
                data-component-type="PostBookmark"
                className="flex items-center"
              >
                <BlueskyPostIcon name="bookmark" size={20} />
              </div>
              <div
                data-component-id={`${nodeId}:share`}
                data-parent-component-id={nodeId}
                data-component-type="PostShare"
                className="flex items-center"
              >
                <BlueskyPostIcon name="share" size={20} />
              </div>
            </div>
          </div>
        </div>
      </article>
    )
  }

/*
 * =========================================================
 * Threads
 * =========================================================
 */
  if (styleId === 'threads') {
    const avatarColor =
      displayName === 'Emma Wilson' ? '#5146e5'
        : displayName === 'Alex Chen' ? '#11a4c8'
          : '#00a87f'
    const replyDots =
      displayName === 'Emma Wilson'
        ? ['#00a58e', '#13a8d1']
        : displayName === 'Alex Chen'
          ? ['#5146e5', '#ef4444']
          : ['#ef4444', '#f59e0b']

    return (
      <article
        ref={(ref) => { if (ref) connect(drag(ref)) }}
        data-component-id={nodeId}
        data-component-type="PostWidget"
        className={`w-full cursor-move border-b border-[#eeeeee] bg-white px-4 py-5 text-[#111111] ${selected ? 'ring-2 ring-emerald-400 ring-inset' : ''}`}
      >
        <div className="flex gap-3">
          <div className="relative flex w-[44px] shrink-0 flex-col items-center">
            <div
              data-component-id={`${nodeId}:avatar`}
              data-parent-component-id={nodeId}
              data-component-type="PostAvatar"
              className="h-[42px] w-[42px] overflow-hidden rounded-full"
            >
              {avatarSrc ? (
                <img src={avatarSrc} alt={displayName} className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center text-[12px] font-bold text-white" style={{ background: avatarColor }}>
                  {initials}
                </div>
              )}
            </div>
            <div className="mt-2 min-h-[310px] w-px flex-1 bg-[#dedfe2]" />
            <div className="mt-2 flex -space-x-[5px]">
              <span className="h-[11px] w-[11px] rounded-full border border-white" style={{ background: replyDots[0] }} />
              <span className="h-[11px] w-[11px] rounded-full border border-white" style={{ background: replyDots[1] }} />
            </div>
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-3">
              <div
                data-component-id={`${nodeId}:author`}
                data-parent-component-id={nodeId}
                data-component-type="PostAuthor"
                className="flex min-w-0 items-center gap-2"
              >
                <span className="truncate text-[15px] font-semibold text-[#111111]">{displayName}</span>
                <span className="shrink-0 text-[13px] text-[#a0a4ab]">{time}</span>
              </div>
              <span
                data-component-id={`${nodeId}:more`}
                data-parent-component-id={nodeId}
                data-component-type="PostMore"
                className="shrink-0 text-[#a4a7ad]"
              >
                <ThreadsPostIcon name="more" size={17} />
              </span>
            </div>

            <p
              data-component-id={`${nodeId}:caption`}
              data-parent-component-id={nodeId}
              data-component-type="PostCaption"
              className="mt-2 whitespace-pre-wrap text-[15px] leading-[1.5] text-[#222222]"
            >
              {caption}
            </p>

            <div
              data-component-id={`${nodeId}:media`}
              data-parent-component-id={nodeId}
              data-component-type="PostMedia"
              className="mt-4 aspect-[16/10] w-full overflow-hidden rounded-[14px] bg-[#f3f4f6]"
            >
              {displayedImageSrc ? (
                <img src={displayedImageSrc} alt="Post" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full flex-col items-center justify-center text-[#9da2aa]">
                  <div className="text-[32px]">🖼️</div>
                  <p className="mt-2 text-[13px]">Media placeholder</p>
                </div>
              )}
            </div>

            <div className="mt-4 flex items-center gap-5 text-[#7e8797]">
              <div
                data-component-id={`${nodeId}:like`}
                data-parent-component-id={nodeId}
                data-component-type="PostLike"
                className="flex items-center gap-2"
              >
                <ThreadsPostIcon name="heart" size={21} />
                <span className="text-[14px]">{likes}</span>
              </div>
              <div
                data-component-id={`${nodeId}:comment`}
                data-parent-component-id={nodeId}
                data-component-type="PostComment"
                className="flex items-center gap-2"
              >
                <ThreadsPostIcon name="comment" size={21} />
                <span className="text-[14px]">{comments}</span>
              </div>
              <div
                data-component-id={`${nodeId}:repost`}
                data-parent-component-id={nodeId}
                data-component-type="PostRepost"
                className="flex items-center gap-2"
              >
                <ThreadsPostIcon name="repost" size={21} />
                <span className="text-[14px]">{shares}</span>
              </div>
              <div
                data-component-id={`${nodeId}:share`}
                data-parent-component-id={nodeId}
                data-component-type="PostShare"
                className="flex items-center"
              >
                <ThreadsPostIcon name="share" size={21} />
              </div>
            </div>

            <p className="mt-4 text-[13px] text-[#9ca3af]">2 replies · {likes} likes</p>
          </div>
        </div>
      </article>
    )
  }

/*

   * =========================================================

   * Instagram

   * =========================================================

   */

  if (

    styleId === 'instagram'

  ) {

    return (

      <article

        ref={(ref) => {

          if (ref) {

            connect(

              drag(ref),

            )

          }

        }}

        /*

         * IMPORTANT:

         * No mb-* here.

         * InstagramTemplate's Craft post-list controls all spacing.

         */

        data-component-id={nodeId}

        data-component-type="PostWidget"

        className={`w-full cursor-move bg-white text-[#262626] ${

          selected

            ? 'ring-2 ring-emerald-400 ring-offset-2'

            : ''

        }`}

      >

        {/* Header */}

        <div className="flex items-center justify-between px-[18px] py-[14px]">

          <div className="flex items-center gap-3">

            <div
              data-component-id={`${nodeId}:avatar`}
              data-parent-component-id={nodeId}
              data-component-type="PostAvatar"
              className="rounded-full bg-gradient-to-br from-[#c32aa3] via-[#f46f30] to-[#ffdc7d] p-[2px]"
            >

              <div className="rounded-full bg-white p-[2px]">

                <div className="h-[44px] w-[44px] overflow-hidden rounded-full">

                  {avatarSrc ? (

                    <img

                      src={avatarSrc}

                      alt={displayName}

                      className="h-full w-full object-cover"

                    />

                  ) : (

                    <div className="flex h-full w-full items-center justify-center bg-[#efefef] text-[12px] font-semibold text-[#555]">

                      {initials}

                    </div>

                  )}

                </div>

              </div>

            </div>

            <div
              data-component-id={`${nodeId}:author`}
              data-parent-component-id={nodeId}
              data-component-type="PostAuthor"
            >
              <p className="text-[14px] font-semibold leading-5">

                {username}

              </p>

              <p className="text-[12px] leading-4 text-[#8e8e8e]">

                {time}

              </p>

            </div>

          </div>

          <span
            data-component-id={`${nodeId}:more`}
            data-parent-component-id={nodeId}
            data-component-type="PostMore"
          >
            <InstagramIcon name="more" size={20} />
          </span>

        </div>

        {/* Image */}

        <div
          data-component-id={`${nodeId}:media`}
          data-parent-component-id={nodeId}
          data-component-type="PostMedia"
          className="aspect-square w-full overflow-hidden bg-[#f4f5f7]"
        >

          {displayedImageSrc ? (

            <img

              src={displayedImageSrc}

              alt="Post"

              className="h-full w-full object-cover"

            />

          ) : (

            <div className="flex h-full w-full flex-col items-center justify-center text-[#8e8e8e]">

              <div className="text-3xl">

                🖼️

              </div>

              <p className="mt-2 text-[13px] font-medium">

                Image placeholder

              </p>

            </div>

          )}

        </div>

        {/* Instagram action/content area */}

        <div className="px-[18px] pb-5 pt-[14px]">

          <div className="flex items-center justify-between">

            <div className="flex items-center gap-[18px]">

              <span
                data-component-id={`${nodeId}:like`}
                data-parent-component-id={nodeId}
                data-component-type="PostLike"
              >
                <InstagramIcon name="heart" size={31} />
              </span>

              <span
                data-component-id={`${nodeId}:comment`}
                data-parent-component-id={nodeId}
                data-component-type="PostComment"
              >
                <InstagramIcon name="comment" size={30} />
              </span>

              <span
                data-component-id={`${nodeId}:share`}
                data-parent-component-id={nodeId}
                data-component-type="PostShare"
              >
                <InstagramIcon name="send" size={30} />
              </span>

            </div>

            <span
              data-component-id={`${nodeId}:bookmark`}
              data-parent-component-id={nodeId}
              data-component-type="PostBookmark"
            >
              <InstagramIcon name="bookmark" size={30} />
            </span>

          </div>

          <p
            data-component-id={`${nodeId}:like-count`}
            data-parent-component-id={nodeId}
            data-component-type="PostLikeCount"
            className="mt-[10px] text-[14px] font-semibold"
          >
            {likes} likes
          </p>

          <p
            data-component-id={`${nodeId}:caption`}
            data-parent-component-id={nodeId}
            data-component-type="PostCaption"
            className="mt-[8px] text-[14px] leading-[1.45]"
          >

            <span className="font-semibold">

              {username}{' '}

            </span>

            {caption}

          </p>

          <p
            data-component-id={`${nodeId}:comments-link`}
            data-parent-component-id={nodeId}
            data-component-type="PostCommentsLink"
            className="mt-[8px] text-[13px] text-[#8e8e8e]"
          >
            View all {comments} comments
          </p>

          <p className="mt-[8px] text-[11px] uppercase tracking-wide text-[#8e8e8e]">

            {time}

          </p>

        </div>

      </article>

    )

  }

  /*

   * =========================================================

   * Facebook

   * =========================================================

   */

  if (

    styleId === 'facebook'

  ) {

    return (

      <article

        ref={(ref) => {

          if (ref) {

            connect(

              drag(ref),

            )

          }

        }}

        /*

         * IMPORTANT:

         * No mb-* here.

         * FacebookTemplate's Craft post-list controls all spacing.

         */

        data-component-id={nodeId}

        data-component-type="PostWidget"

        className={`w-full cursor-move overflow-hidden rounded-xl border border-[#dddfe2] bg-white text-[#050505] shadow-sm ${

          selected

            ? 'ring-2 ring-emerald-400 ring-offset-2'

            : ''

        }`}

      >

        {/* Header */}

        <div className="flex items-start justify-between px-4 pt-4">

          <div className="flex items-start gap-3">

            <div
              data-component-id={`${nodeId}:avatar`}
              data-parent-component-id={nodeId}
              data-component-type="PostAvatar"
              className="h-12 w-12 shrink-0 overflow-hidden rounded-full"
            >

              {avatarSrc ? (

                <img

                  src={avatarSrc}

                  alt={displayName}

                  className="h-full w-full object-cover"

                />

              ) : (

                <div className="flex h-full w-full items-center justify-center bg-[#5b49ff] text-[13px] font-bold text-white">

                  {initials}

                </div>

              )}

            </div>

            <div
              data-component-id={`${nodeId}:author`}
              data-parent-component-id={nodeId}
              data-component-type="PostAuthor"
              className="pt-0.5"
            >
              <p className="text-[15px] font-semibold leading-5">

                {displayName}

              </p>

              <p className="mt-0.5 text-[13px] leading-4 text-[#65676b]">

                {time} · 🌐

              </p>

            </div>

          </div>

          <div
            data-component-id={`${nodeId}:more`}
            data-parent-component-id={nodeId}
            data-component-type="PostMore"
            className="pt-1 text-[#65676b]"
          >
            <MoreIcon size={20} />
          </div>

        </div>

        {/* Caption */}

        <div
          data-component-id={`${nodeId}:caption`}
          data-parent-component-id={nodeId}
          data-component-type="PostCaption"
          className="px-4 pb-3 pt-2"
        >
          <p className="text-[15px] leading-[1.45] text-[#050505]">

            {caption}

          </p>

        </div>

        {/* Image */}

        <div
          data-component-id={`${nodeId}:media`}
          data-parent-component-id={nodeId}
          data-component-type="PostMedia"
          className="w-full overflow-hidden bg-[#e9edf2]"
        >

          {displayedImageSrc ? (

            <img

              src={displayedImageSrc}

              alt="Post"

              className="max-h-[420px] w-full object-cover"

            />

          ) : (

            <div className="flex h-[300px] w-full flex-col items-center justify-center text-[#65676b]">

              <div className="text-3xl">

                🖼️

              </div>

              <p className="mt-2 text-[13px] font-medium">

                Image placeholder

              </p>

            </div>

          )}

        </div>

        {/* Reaction summary */}

        <div className="flex min-h-[42px] items-center justify-between px-4 py-2">

          <div className="flex items-center gap-2">

            <div className="flex -space-x-1">

              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#1877f2] text-[10px] text-white ring-2 ring-white">

                👍

              </div>

              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#f55368] text-[10px] text-white ring-2 ring-white">

                ♥

              </div>

            </div>

            <span
              data-component-id={`${nodeId}:like-count`}
              data-parent-component-id={nodeId}
              data-component-type="PostLikeCount"
              className="text-[13px] text-[#65676b]"
            >
              {likes}
            </span>

          </div>

          <div className="flex items-center gap-3 text-[13px] text-[#65676b]">

            <span
              data-component-id={`${nodeId}:comment-count`}
              data-parent-component-id={nodeId}
              data-component-type="PostCommentCount"
            >
              {comments} comments
            </span>

            <span
              data-component-id={`${nodeId}:share-count`}
              data-parent-component-id={nodeId}
              data-component-type="PostShareCount"
            >
              {shares} shares
            </span>

          </div>

        </div>

        {/* Facebook action row */}

        <div className="grid h-[54px] grid-cols-3 border-t border-[#ced0d4] px-2">

          <div
            data-component-id={`${nodeId}:like`}
            data-parent-component-id={nodeId}
            data-component-type="PostLike"
            className="flex items-center justify-center gap-[9px] rounded-md text-[#65676b] hover:bg-[#f0f2f5]"
          >

            <FacebookIcon

              name="thumb"

              size={21}

            />

            <span className="text-[15px] font-semibold">

              Like

            </span>

          </div>

          <div
            data-component-id={`${nodeId}:comment`}
            data-parent-component-id={nodeId}
            data-component-type="PostComment"
            className="flex items-center justify-center gap-[9px] rounded-md text-[#65676b] hover:bg-[#f0f2f5]"
          >

            <FacebookIcon

              name="comment"

              size={22}

            />

            <span className="text-[15px] font-semibold">

              Comment

            </span>

          </div>

          <div
            data-component-id={`${nodeId}:share`}
            data-parent-component-id={nodeId}
            data-component-type="PostShare"
            className="flex items-center justify-center gap-[9px] rounded-md text-[#65676b] hover:bg-[#f0f2f5]"
          >

            <FacebookIcon

              name="share"

              size={21}

            />

            <span className="text-[15px] font-semibold">

              Share

            </span>

          </div>

        </div>

      </article>

    )

  }

  /*

   * =========================================================

   * Other platforms

   * =========================================================

   */

  return (

    <article

      ref={(ref) => {

        if (ref) {

          connect(

            drag(ref),

          )

        }

      }}

      data-component-id={nodeId}

      data-component-type="PostWidget"

      className={`w-full cursor-move rounded-lg border border-gray-200 bg-white p-4 ${

        selected

          ? 'ring-2 ring-emerald-400 ring-offset-2'

          : ''

      }`}

    >

      <p className="font-semibold">

        {displayName}

      </p>

      <p className="mt-1 text-sm text-gray-500">

        {time}

      </p>

      <p className="mt-3 text-sm">

        {caption}

      </p>

      {displayedImageSrc && (

        <img

          src={displayedImageSrc}

          alt="Post"

          className="mt-4 max-h-[420px] w-full rounded object-cover"

        />

      )}

    </article>

  )

}

PostWidget.craft = {

  displayName:

    'Post Widget',

  props: {

    styleId:

      'facebook',

    username:

      '@emmaw',

    displayName:

      'Emma Wilson',

    time:

      '2h ago',

    avatarSrc:

      '',

    assetId:

      undefined,

    imageSrc:

      '',

    caption:

      'Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂',

    likes:

      47,

    comments:

      12,

    shares:

      8,

    views:

      1243,

    saves:

      5,

    sound:

      'Original Sound · Emma Wilson',

  },

}

export default PostWidget
