import { useNode } from '@craftjs/core'

type LayoutProps = {
  children?: React.ReactNode
}


/*
 * Whole TikTok desktop environment.
 */
export function TikTokDesktopShell({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="h-[820px] min-h-0 w-full overflow-hidden bg-[#0f0f0f] text-white"
    >
      {children}
    </div>
  )
}


TikTokDesktopShell.craft = {
  displayName: 'TikTok Desktop Shell',
}


/*
 * Main area to the right of sidebar.
 */
export function TikTokMainArea({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <main
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex h-full min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-[#0f0f0f]"
    >
      {children}
    </main>
  )
}


TikTokMainArea.craft = {
  displayName: 'TikTok Main Area',
}


/*
 * Vertical scrollable TikTok feed.
 *
 * Important:
 * - flex-col instead of horizontal flex
 * - justify-start instead of justify-center
 * - every new PostWidget is appended below the previous one
 */
export function TikTokFeedStage({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <section
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-6 pb-10"
    >
      <div className="mx-auto flex w-full max-w-[620px] flex-col items-center gap-6 pt-5">
        {children}
      </div>
    </section>
  )
}


TikTokFeedStage.craft = {
  displayName: 'TikTok Feed Stage',
}


/*
 * Kept for compatibility with any older Craft JSON.
 */
export function TikTokPostStage({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex w-full flex-col items-center gap-6"
    >
      {children}
    </div>
  )
}


TikTokPostStage.craft = {
  displayName: 'TikTok Post Stage',
}


/*
 * Legacy video card.
 * Keep registered for older serialized content.
 */
export function TikTokVideoCard({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <article
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="relative h-[630px] w-[355px] shrink-0 overflow-hidden rounded-[18px] bg-[#181818] shadow-2xl"
    >
      {children}
    </article>
  )
}


TikTokVideoCard.craft = {
  displayName: 'TikTok Video Card',
}


/*
 * Legacy action rail.
 */
export function TikTokActionRail({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <aside
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex w-[58px] shrink-0 flex-col items-center justify-end gap-4 pb-2"
    >
      {children}
    </aside>
  )
}


TikTokActionRail.craft = {
  displayName: 'TikTok Action Rail',
}