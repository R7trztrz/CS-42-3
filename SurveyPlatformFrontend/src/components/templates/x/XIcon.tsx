type XIconName =
  | 'logo'
  | 'home'
  | 'search'
  | 'notifications'
  | 'messages'
  | 'bookmarks'
  | 'profile'
  | 'image'
  | 'globe'
  | 'poll'
  | 'emoji'
  | 'more'
  | 'verified'
  | 'comment'
  | 'repost'
  | 'like'
  | 'views'
  | 'bookmark'
  | 'share'

type XIconProps = {
  name: XIconName
  size?: number
  className?: string
}

function XIcon({
  name,
  size = 24,
  className = '',
}: XIconProps) {
  const strokeProps = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.9,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    className,
  }

  if (name === 'logo') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
        <path d="M18.901 1.153h3.68l-8.04 9.19 9.459 12.504h-7.406l-5.8-7.584-6.638 7.584H.474l8.6-9.83L0 1.153h7.594l5.243 6.932 6.064-6.932Zm-1.291 19.492h2.039L6.486 3.24H4.298l13.312 17.405Z" />
      </svg>
    )
  }

  if (name === 'verified') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
        <path d="M12 2.7 14.2 4l2.55-.05.9 2.4 2.2 1.3-.7 2.45.7 2.45-2.2 1.3-.9 2.4-2.55-.05L12 17.3l-2.2-1.3-2.55.05-.9-2.4-2.2-1.3.7-2.45-.7-2.45 2.2-1.3.9-2.4L9.8 4 12 2.7Z" fill="#1d9bf0" />
        <path d="m8.9 10.1 2 2 4.2-4.5" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  }

  if (name === 'home') {
    return (
      <svg {...strokeProps}>
        <path d="M3 10.8 12 3l9 7.8" />
        <path d="M5.5 9.8V20h5.6v-5.8h1.8V20h5.6V9.8" />
      </svg>
    )
  }

  if (name === 'search') {
    return (
      <svg {...strokeProps}>
        <circle cx="11" cy="11" r="6.7" />
        <path d="m20 20-4.2-4.2" />
      </svg>
    )
  }

  if (name === 'notifications') {
    return (
      <svg {...strokeProps}>
        <path d="M12 3.8a4.6 4.6 0 0 0-4.6 4.6v2.1c0 1.1-.3 2.2-.9 3.1L5.3 15h13.4l-1.2-1.4c-.6-.9-.9-2-.9-3.1V8.4A4.6 4.6 0 0 0 12 3.8Z" />
        <path d="M9.5 18a2.7 2.7 0 0 0 5 0" />
      </svg>
    )
  }

  if (name === 'messages') {
    return (
      <svg {...strokeProps}>
        <path d="M3.5 5.5h17v13h-17z" />
        <path d="m4.5 6.5 7.5 6 7.5-6" />
      </svg>
    )
  }

  if (name === 'bookmarks' || name === 'bookmark') {
    return (
      <svg {...strokeProps}>
        <path d="M7 3.5h10v17l-5-3.4-5 3.4z" />
      </svg>
    )
  }

  if (name === 'profile') {
    return (
      <svg {...strokeProps}>
        <circle cx="12" cy="8" r="3.3" />
        <path d="M5 19.2c1.5-3.2 4.1-4.8 7-4.8s5.5 1.6 7 4.8" />
      </svg>
    )
  }

  if (name === 'image') {
    return (
      <svg {...strokeProps}>
        <rect x="3.5" y="5.5" width="17" height="13" rx="2.2" />
        <circle cx="9" cy="10" r="1.4" />
        <path d="m5.7 16 4.1-4.1 2.8 2.8 2-2 3.7 3.3" />
      </svg>
    )
  }

  if (name === 'globe') {
    return (
      <svg {...strokeProps}>
        <circle cx="12" cy="12" r="8" />
        <path d="M4.4 12h15.2" />
        <path d="M12 4a13 13 0 0 1 0 16" />
        <path d="M12 4a13 13 0 0 0 0 16" />
      </svg>
    )
  }

  if (name === 'poll') {
    return (
      <svg {...strokeProps}>
        <path d="M6 18V10" />
        <path d="M12 18V6" />
        <path d="M18 18V13" />
      </svg>
    )
  }

  if (name === 'emoji') {
    return (
      <svg {...strokeProps}>
        <circle cx="12" cy="12" r="8" />
        <path d="M9 10h.01" />
        <path d="M15 10h.01" />
        <path d="M8.8 14.2c.8 1.1 1.9 1.7 3.2 1.7s2.4-.6 3.2-1.7" />
      </svg>
    )
  }

  if (name === 'more') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
        <circle cx="6.5" cy="12" r="1.5" />
        <circle cx="12" cy="12" r="1.5" />
        <circle cx="17.5" cy="12" r="1.5" />
      </svg>
    )
  }

  if (name === 'comment') {
    return (
      <svg {...strokeProps}>
        <path d="M20.1 11.7c0 4.25-3.6 7.55-8.1 7.55-1.2 0-2.35-.22-3.4-.62L4.3 19.8l1.28-3.7a7.05 7.05 0 0 1-1.68-4.4c0-4.25 3.6-7.55 8.1-7.55s8.1 3.3 8.1 7.55Z" />
      </svg>
    )
  }

  if (name === 'repost') {
    return (
      <svg {...strokeProps}>
        <path d="M7.2 7.2h9.2l-2.5-2.5" />
        <path d="m16.4 7.2 2.5-2.5" />
        <path d="M16.8 16.8H7.6l2.5 2.5" />
        <path d="m7.6 16.8-2.5 2.5" />
        <path d="M7.2 7.2 5.2 9.4v3.1" />
        <path d="m16.8 16.8 2-2.2v-3.1" />
      </svg>
    )
  }

  if (name === 'like') {
    return (
      <svg {...strokeProps}>
        <path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" />
      </svg>
    )
  }

  if (name === 'views') {
    return (
      <svg {...strokeProps}>
        <path d="M2.8 12s3.4-5.5 9.2-5.5S21.2 12 21.2 12s-3.4 5.5-9.2 5.5S2.8 12 2.8 12Z" />
        <circle cx="12" cy="12" r="2.55" />
      </svg>
    )
  }

  if (name === 'share') {
    return (
      <svg {...strokeProps}>
        <circle cx="18" cy="5" r="2.15" />
        <circle cx="6" cy="12" r="2.15" />
        <circle cx="18" cy="19" r="2.15" />
        <path d="m7.9 10.9 8.1-4.7" />
        <path d="m7.9 13.1 8.1 4.7" />
      </svg>
    )
  }

  return null
}

export default XIcon
