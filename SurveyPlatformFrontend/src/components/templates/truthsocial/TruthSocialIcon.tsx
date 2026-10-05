type TruthSocialIconName =
  | 'logo' | 'home' | 'search' | 'notifications' | 'messages'
  | 'bookmarks' | 'profile' | 'image' | 'link' | 'poll'
  | 'comment' | 'retruth' | 'heart' | 'bookmark' | 'share' | 'more'

type TruthSocialIconProps = {
  name: TruthSocialIconName
  size?: number
  className?: string
}

function TruthSocialIcon({ name, size = 24, className = '' }: TruthSocialIconProps) {
  const common = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth: 1.8,
    strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, className,
  }

  if (name === 'logo') {
    return (
      <svg width={size} height={size} viewBox="0 0 48 48" fill="none" className={className}>
        <path d="M24 3 41 10v12c0 11.3-6.6 18.5-17 23C13.6 40.5 7 33.3 7 22V10L24 3Z" fill="#ef1f2c" />
        <path d="M15 14h18v6h-6v15h-6V20h-6v-6Z" fill="#fff" />
      </svg>
    )
  }
  if (name === 'home') return <svg {...common}><path d="M3.5 10.5 12 3.3l8.5 7.2" /><path d="M5.5 9.8V20h5.1v-5.3h2.8V20h5.1V9.8" /></svg>
  if (name === 'search') return <svg {...common}><circle cx="10.5" cy="10.5" r="6.7" /><path d="m19.7 19.7-4.5-4.5" /></svg>
  if (name === 'notifications') return <svg {...common}><path d="M12 4.1a4.5 4.5 0 0 0-4.5 4.5v2.2c0 .95-.3 1.9-.85 2.67L5.5 15h13l-1.15-1.53a4.5 4.5 0 0 1-.85-2.67V8.6A4.5 4.5 0 0 0 12 4.1Z" /><path d="M9.5 18.2a2.7 2.7 0 0 0 5 0" /></svg>
  if (name === 'messages') return <svg {...common}><path d="M4 5.5h16v11H9l-5 3v-14Z" /></svg>
  if (name === 'bookmarks' || name === 'bookmark') return <svg {...common}><path d="M7 3.5h10v17l-5-3.4-5 3.4z" /></svg>
  if (name === 'profile') return <svg {...common}><circle cx="12" cy="8" r="3.2" /><path d="M5.2 19.2c1.4-3.2 4-4.8 6.8-4.8s5.4 1.6 6.8 4.8" /></svg>
  if (name === 'image') return <svg {...common}><rect x="3.5" y="5.5" width="17" height="13" rx="2.2" /><circle cx="9" cy="10" r="1.4" /><path d="m5.7 16 4.1-4.1 2.8 2.8 2-2 3.7 3.3" /></svg>
  if (name === 'link') return <svg {...common}><path d="M9.5 14.5 14.5 9.5" /><path d="M7.4 16.6 5.8 18.2a3.4 3.4 0 0 1-4.8-4.8l3.1-3.1a3.4 3.4 0 0 1 4.8 0" /><path d="m16.6 7.4 1.6-1.6a3.4 3.4 0 0 1 4.8 4.8l-3.1 3.1a3.4 3.4 0 0 1-4.8 0" /></svg>
  if (name === 'poll') return <svg {...common}><path d="M6 18V10M12 18V6M18 18v-5" /></svg>
  if (name === 'comment') return <svg {...common}><path d="M20.2 11.8c0 4.3-3.7 7.6-8.2 7.6-1.2 0-2.4-.2-3.5-.6L4.2 20l1.3-3.8a7.1 7.1 0 0 1-1.7-4.4c0-4.3 3.7-7.6 8.2-7.6s8.2 3.3 8.2 7.6Z" /></svg>
  if (name === 'retruth') return <svg {...common}><path d="M7.2 7.3h9l-2.4-2.4" /><path d="m16.2 7.3 2.5-2.5" /><path d="M16.8 16.7h-9l2.4 2.4" /><path d="m7.8 16.7-2.5 2.5" /><path d="M7.2 7.3 5.2 9.5v3" /><path d="m16.8 16.7 2-2.2v-3" /></svg>
  if (name === 'heart') return <svg {...common}><path d="M12 20.2 4.8 13a4.7 4.7 0 0 1 6.6-6.6l.6.6.6-.6a4.7 4.7 0 0 1 6.6 6.6z" /></svg>
  if (name === 'share') return <svg {...common}><circle cx="18" cy="5" r="2.1" /><circle cx="6" cy="12" r="2.1" /><circle cx="18" cy="19" r="2.1" /><path d="m7.9 10.9 8.2-4.7" /><path d="m7.9 13.1 8.2 4.7" /></svg>

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <circle cx="6.5" cy="12" r="1.4" /><circle cx="12" cy="12" r="1.4" /><circle cx="17.5" cy="12" r="1.4" />
    </svg>
  )
}

export default TruthSocialIcon
