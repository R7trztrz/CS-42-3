type ThreadsIconName =
  | 'logo'
  | 'home'
  | 'search'
  | 'create'
  | 'notifications'
  | 'profile'
  | 'heart'
  | 'comment'
  | 'repost'
  | 'share'
  | 'more'

type ThreadsIconProps = {
  name: ThreadsIconName
  size?: number
  className?: string
}

function ThreadsIcon({
  name,
  size = 24,
  className = '',
}: ThreadsIconProps) {
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

  if (name === 'logo') {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className}>
        <path
          d="M12.05 3.1c5.28 0 8.65 3.38 8.65 8.55 0 5.37-3.08 9.25-8.46 9.25-4.9 0-8.34-3.13-8.34-7.72 0-3.85 2.56-6.5 6.34-6.5 3.36 0 5.92 1.86 6.45 4.73.54 2.9-1.04 5.43-3.83 6-2.73.56-5.18-.77-5.6-3.08-.39-2.05.95-3.83 3.15-4.18 2.36-.38 4.79.59 6.77 2.08"
          stroke="currentColor"
          strokeWidth="2.35"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  if (name === 'home') {
    return (
      <svg {...common}>
        <path d="M3.5 10.5 12 3.3l8.5 7.2" />
        <path d="M5.5 9.8V20h5.1v-5.3h2.8V20h5.1V9.8" />
      </svg>
    )
  }

  if (name === 'search') {
    return (
      <svg {...common}>
        <circle cx="10.5" cy="10.5" r="6.7" />
        <path d="m19.7 19.7-4.5-4.5" />
      </svg>
    )
  }

  if (name === 'create') {
    return (
      <svg {...common}>
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 8v8" />
        <path d="M8 12h8" />
      </svg>
    )
  }

  if (name === 'notifications') {
    return (
      <svg {...common}>
        <path d="M12 4.1a4.5 4.5 0 0 0-4.5 4.5v2.2c0 .95-.3 1.9-.85 2.67L5.5 15h13l-1.15-1.53a4.5 4.5 0 0 1-.85-2.67V8.6A4.5 4.5 0 0 0 12 4.1Z" />
        <path d="M9.5 18.2a2.7 2.7 0 0 0 5 0" />
      </svg>
    )
  }

  if (name === 'profile') {
    return (
      <svg {...common}>
        <circle cx="12" cy="8" r="3.2" />
        <path d="M5.2 19.2c1.4-3.2 4-4.8 6.8-4.8s5.4 1.6 6.8 4.8" />
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

  if (name === 'comment') {
    return (
      <svg {...common}>
        <path d="M20.3 11.8c0 4.35-3.68 7.72-8.3 7.72-1.22 0-2.4-.22-3.47-.63L4.1 20.1l1.32-3.8a7.18 7.18 0 0 1-1.72-4.5c0-4.35 3.68-7.72 8.3-7.72s8.3 3.37 8.3 7.72Z" />
      </svg>
    )
  }

  if (name === 'repost') {
    return (
      <svg {...common}>
        <path d="M7.2 7.3h9l-2.4-2.4" />
        <path d="m16.2 7.3 2.5-2.5" />
        <path d="M16.8 16.7h-9l2.4 2.4" />
        <path d="m7.8 16.7-2.5 2.5" />
        <path d="M7.2 7.3 5.2 9.5v3" />
        <path d="m16.8 16.7 2-2.2v-3" />
      </svg>
    )
  }

  if (name === 'share') {
    return (
      <svg {...common}>
        <circle cx="18" cy="5" r="2.1" />
        <circle cx="6" cy="12" r="2.1" />
        <circle cx="18" cy="19" r="2.1" />
        <path d="m7.9 10.9 8.2-4.7" />
        <path d="m7.9 13.1 8.2 4.7" />
      </svg>
    )
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="currentColor"
      className={className}
    >
      <circle cx="6.5" cy="12" r="1.4" />
      <circle cx="12" cy="12" r="1.4" />
      <circle cx="17.5" cy="12" r="1.4" />
    </svg>
  )
}

export default ThreadsIcon
