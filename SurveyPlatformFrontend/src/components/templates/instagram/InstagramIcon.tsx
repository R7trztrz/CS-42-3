type InstagramIconName =
  | 'instagram'
  | 'home'
  | 'search'
  | 'explore'
  | 'reels'
  | 'message'
  | 'heart'
  | 'create'
  | 'comment'
  | 'send'
  | 'bookmark'
  | 'more'


type InstagramIconProps = {
  name: InstagramIconName
  size?: number
  className?: string
}


function InstagramIcon({
  name,
  size = 24,
  className = '',
}: InstagramIconProps) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    className,
  }

  if (name === 'instagram') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="3"
          width="18"
          height="18"
          rx="5"
        />
        <circle
          cx="12"
          cy="12"
          r="4"
        />
        <circle
          cx="17.5"
          cy="6.5"
          r="1"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }

  if (name === 'home') {
    return (
      <svg {...common}>
        <path d="M3 10.5 12 3l9 7.5" />
        <path d="M5 9.5V21h14V9.5" />
        <path d="M9 21v-7h6v7" />
      </svg>
    )
  }

  if (name === 'search') {
    return (
      <svg {...common}>
        <circle
          cx="11"
          cy="11"
          r="7"
        />
        <path d="m20 20-4-4" />
      </svg>
    )
  }

  if (name === 'explore') {
    return (
      <svg {...common}>
        <circle
          cx="12"
          cy="12"
          r="9"
        />
        <path d="m15.5 8.5-2 5-5 2 2-5 5-2Z" />
      </svg>
    )
  }

  if (name === 'reels') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="4"
          width="18"
          height="16"
          rx="3"
        />
        <path d="m8 4 3 5" />
        <path d="m14 4 3 5" />
        <path d="M3 9h18" />
        <path
          d="m10 12 5 3-5 3Z"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }

  if (name === 'message') {
    return (
      <svg {...common}>
        <path d="M4 5h16v12H8l-4 3V5Z" />
      </svg>
    )
  }

  if (name === 'heart') {
    return (
      <svg {...common}>
        <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />
      </svg>
    )
  }

  if (name === 'create') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="3"
          width="18"
          height="18"
          rx="5"
        />
        <path d="M12 8v8" />
        <path d="M8 12h8" />
      </svg>
    )
  }

  if (name === 'comment') {
    return (
      <svg {...common}>
        <path d="M21 12a8 8 0 0 1-8 8H6l-3 2 1-5a8 8 0 1 1 17-5Z" />
      </svg>
    )
  }

  if (name === 'send') {
    return (
      <svg {...common}>
        <path d="m22 2-7 20-4-9-9-4 20-7Z" />
        <path d="M22 2 11 13" />
      </svg>
    )
  }

  if (name === 'bookmark') {
    return (
      <svg {...common}>
        <path d="M6 3h12v18l-6-4-6 4V3Z" />
      </svg>
    )
  }

  if (name === 'more') {
    return (
      <svg {...common}>
        <circle
          cx="5"
          cy="12"
          r="1"
          fill="currentColor"
        />
        <circle
          cx="12"
          cy="12"
          r="1"
          fill="currentColor"
        />
        <circle
          cx="19"
          cy="12"
          r="1"
          fill="currentColor"
        />
      </svg>
    )
  }

  return null
}


InstagramIcon.craft = {
  displayName: 'Instagram Icon',
}


export default InstagramIcon