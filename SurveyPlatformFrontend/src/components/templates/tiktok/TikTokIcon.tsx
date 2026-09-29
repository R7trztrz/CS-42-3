type TikTokIconName =
  | 'home'
  | 'friends'
  | 'explore'
  | 'message'
  | 'search'
  | 'heart'
  | 'comment'
  | 'bookmark'
  | 'share'
  | 'up'
  | 'down'
  | 'music'
  | 'plus'


type TikTokIconProps = {
  name: TikTokIconName
  size?: number
  className?: string
}


function TikTokIcon({
  name,
  size = 22,
  className = '',
}: TikTokIconProps) {
  const common = {
    width: size,
    height: size,
    viewBox:
      '0 0 24 24',
    fill: 'none',
    stroke:
      'currentColor',
    strokeWidth: 2,
    strokeLinecap:
      'round' as const,
    strokeLinejoin:
      'round' as const,
    className,
  }


  /*
   * =====================================
   * Sidebar icons
   * =====================================
   */

  if (
    name === 'home'
  ) {
    return (
      <svg {...common}>
        <path d="M3 11.5 12 4l9 7.5" />
        <path d="M5 10.5V21h14V10.5" />
        <path d="M9 21v-6h6v6" />
      </svg>
    )
  }


  if (
    name === 'friends'
  ) {
    return (
      <svg {...common}>
        <circle
          cx="9"
          cy="8"
          r="3"
        />

        <circle
          cx="17"
          cy="9"
          r="2.5"
        />

        <path d="M3 20c.4-4 2.7-6 6-6s5.6 2 6 6" />

        <path d="M14 15c3.2-.4 5.8 1.2 6.5 4" />
      </svg>
    )
  }


  if (
    name === 'explore'
  ) {
    return (
      <svg {...common}>
        <path d="M4 6.5 9 4l6 2.5L20 4v13.5L15 20l-6-2.5L4 20Z" />

        <path d="M9 4v13.5" />

        <path d="M15 6.5V20" />
      </svg>
    )
  }


  if (
    name === 'message'
  ) {
    return (
      <svg {...common}>
        <path d="M4 5h16v12H8l-4 3Z" />
      </svg>
    )
  }


  if (
    name === 'search'
  ) {
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


  /*
   * =====================================
   * TikTok interaction icons
   * =====================================
   */

  if (
    name === 'heart'
  ) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
      >
        <path
          d="M12 20.4 4.55 13.2C1.45 10.2 3.02 5 7.28 5c2.02 0 3.54 1.13 4.72 2.5C13.18 6.13 14.7 5 16.72 5c4.26 0 5.83 5.2 2.73 8.2L12 20.4Z"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }


  if (
    name === 'comment'
  ) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
      >
        <path
          d="M6.2 5.2h11.6A3.2 3.2 0 0 1 21 8.4v5.4a3.2 3.2 0 0 1-3.2 3.2h-5.1l-3.5 2.5c-.55.39-1.3 0-1.3-.68V17H6.2A3.2 3.2 0 0 1 3 13.8V8.4a3.2 3.2 0 0 1 3.2-3.2Z"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        <circle
          cx="8.2"
          cy="11.1"
          r="1"
          fill="currentColor"
        />

        <circle
          cx="12"
          cy="11.1"
          r="1"
          fill="currentColor"
        />

        <circle
          cx="15.8"
          cy="11.1"
          r="1"
          fill="currentColor"
        />
      </svg>
    )
  }


  if (
    name === 'bookmark'
  ) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
      >
        <path
          d="M6.5 4.5c0-.83.67-1.5 1.5-1.5h8c.83 0 1.5.67 1.5 1.5V21L12 17.1 6.5 21V4.5Z"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }


  if (
    name === 'share'
  ) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        className={className}
      >
        <circle
          cx="18"
          cy="5"
          r="2.5"
          stroke="currentColor"
          strokeWidth="2"
        />

        <circle
          cx="6"
          cy="12"
          r="2.5"
          stroke="currentColor"
          strokeWidth="2"
        />

        <circle
          cx="18"
          cy="19"
          r="2.5"
          stroke="currentColor"
          strokeWidth="2"
        />

        <path
          d="m8.2 10.8 7.5-4.3"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />

        <path
          d="m8.2 13.2 7.5 4.3"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </svg>
    )
  }


  /*
   * =====================================
   * Navigation / media controls
   * =====================================
   */

  if (
    name === 'up'
  ) {
    return (
      <svg {...common}>
        <path d="m6 15 6-6 6 6" />
      </svg>
    )
  }


  if (
    name === 'down'
  ) {
    return (
      <svg {...common}>
        <path d="m6 9 6 6 6-6" />
      </svg>
    )
  }


  if (
    name === 'music'
  ) {
    return (
      <svg {...common}>
        <path d="M9 18V6l10-2v12" />

        <circle
          cx="6.5"
          cy="18"
          r="2.5"
        />

        <circle
          cx="16.5"
          cy="16"
          r="2.5"
        />
      </svg>
    )
  }


  /*
   * Plus
   */
  return (
    <svg {...common}>
      <path d="M12 5v14" />
      <path d="M5 12h14" />
    </svg>
  )
}


TikTokIcon.craft = {
  displayName:
    'TikTok Icon',
}


export default TikTokIcon