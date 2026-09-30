type FacebookIconName =
  | 'facebook'
  | 'home'
  | 'friends'
  | 'groups'
  | 'video'
  | 'saved'
  | 'search'
  | 'message'
  | 'bell'
  | 'more'
  | 'camera'
  | 'photo'
  | 'smile'
  | 'comment'
  | 'share'
  | 'game'


type FacebookIconProps = {
  name: FacebookIconName
  size?: number
  className?: string
}


function FacebookIcon({
  name,
  size = 22,
  className = '',
}: FacebookIconProps) {
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


  if (name === 'facebook') {
    return (
      <svg
        {...common}
        viewBox="0 0 24 24"
      >
        <circle
          cx="12"
          cy="12"
          r="11"
          fill="#1877f2"
          stroke="none"
        />

        <path
          d="M13.5 7.5h2V4.4c-.35-.05-1.55-.15-2.95-.15-2.9 0-4.9 1.77-4.9 5.03V12H4.35v3.5h3.3V24h4.05v-8.5h3.4l.54-3.5H11.7V9.63c0-1.01.27-1.7 1.8-1.7Z"
          fill="white"
          stroke="none"
          transform="scale(.7) translate(5 5)"
        />
      </svg>
    )
  }


  if (name === 'home') {
    return (
      <svg {...common}>
        <path d="M3 11.5 12 4l9 7.5" />
        <path d="M5 10.5V21h14V10.5" />
        <path d="M9 21v-6h6v6" />
      </svg>
    )
  }


  if (name === 'friends') {
    return (
      <svg {...common}>
        <circle cx="9" cy="8" r="3" />
        <circle cx="17" cy="9" r="2.5" />

        <path d="M3 20c.4-4 2.7-6 6-6s5.6 2 6 6" />
        <path d="M14 15c3.2-.4 5.8 1.2 6.5 4" />
      </svg>
    )
  }


  if (name === 'groups') {
    return (
      <svg {...common}>
        <circle cx="8" cy="8" r="2.5" />
        <circle cx="16" cy="8" r="2.5" />
        <circle cx="12" cy="14" r="2.5" />

        <path d="M3 20c.5-3 2-4.5 5-4.5" />
        <path d="M21 20c-.5-3-2-4.5-5-4.5" />
        <path d="M7 21c.5-3 2-4.5 5-4.5s4.5 1.5 5 4.5" />
      </svg>
    )
  }


  if (name === 'video') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="6"
          width="13"
          height="12"
          rx="2"
        />

        <path d="m16 10 5-3v10l-5-3Z" />
      </svg>
    )
  }


  if (name === 'saved') {
    return (
      <svg {...common}>
        <path d="M6 4h12v17l-6-4-6 4Z" />
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


  if (name === 'message') {
    return (
      <svg {...common}>
        <path d="M4 5h16v12H8l-4 3Z" />
      </svg>
    )
  }


  if (name === 'bell') {
    return (
      <svg {...common}>
        <path d="M6 9a6 6 0 1 1 12 0v5l2 3H4l2-3Z" />
        <path d="M9.5 20h5" />
      </svg>
    )
  }


  if (name === 'more') {
    return (
      <svg {...common}>
        <circle
          cx="5"
          cy="12"
          r="1.5"
          fill="currentColor"
          stroke="none"
        />

        <circle
          cx="12"
          cy="12"
          r="1.5"
          fill="currentColor"
          stroke="none"
        />

        <circle
          cx="19"
          cy="12"
          r="1.5"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }


  if (name === 'camera') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="6"
          width="18"
          height="13"
          rx="2"
        />

        <path d="m15 10 4-2v9l-4-2Z" />
      </svg>
    )
  }


  if (name === 'photo') {
    return (
      <svg {...common}>
        <rect
          x="3"
          y="4"
          width="18"
          height="16"
          rx="2"
        />

        <circle
          cx="9"
          cy="9"
          r="2"
        />

        <path d="m5 18 5-5 3 3 2-2 4 4" />
      </svg>
    )
  }


  if (name === 'smile') {
    return (
      <svg {...common}>
        <circle
          cx="12"
          cy="12"
          r="9"
        />

        <circle
          cx="9"
          cy="10"
          r="1"
          fill="currentColor"
          stroke="none"
        />

        <circle
          cx="15"
          cy="10"
          r="1"
          fill="currentColor"
          stroke="none"
        />

        <path d="M8 14c1 2 2.5 3 4 3s3-1 4-3" />
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


  if (name === 'share') {
    return (
      <svg {...common}>
        <circle cx="18" cy="5" r="2.5" />
        <circle cx="6" cy="12" r="2.5" />
        <circle cx="18" cy="19" r="2.5" />

        <path d="m8.2 10.8 7.5-4.3" />
        <path d="m8.2 13.2 7.5 4.3" />
      </svg>
    )
  }


  if (name === 'game') {
    return (
      <svg {...common}>
        <path d="M7 9h10a5 5 0 0 1 4.5 7.2l-1 2a2 2 0 0 1-3.1.6L15 17h-6l-2.4 1.8a2 2 0 0 1-3.1-.6l-1-2A5 5 0 0 1 7 9Z" />
        <path d="M7 12v4" />
        <path d="M5 14h4" />

        <circle
          cx="16"
          cy="13"
          r="1"
          fill="currentColor"
          stroke="none"
        />

        <circle
          cx="18"
          cy="15"
          r="1"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    )
  }


  return null
}


FacebookIcon.craft = {
  displayName: 'Facebook Icon',
}


export default FacebookIcon