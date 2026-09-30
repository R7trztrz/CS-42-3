import type {
  ReactNode,
} from 'react'

import {
  useNode,
} from '@craftjs/core'


type ChildrenProps = {
  children?: ReactNode
}


/*
 * =========================================================
 * Shared helper
 * =========================================================
 */

function useCraftContainer() {
  return useNode((node) => ({
    selected:
      node.events.selected,
  }))
}


/*
 * =========================================================
 * Facebook Desktop Shell
 * =========================================================
 *
 * Entire simulated Facebook desktop environment.
 */

export function FacebookDesktopShell({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`h-[720px] w-full overflow-hidden bg-[#f0f2f5] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookDesktopShell.craft = {
  displayName:
    'Facebook Desktop Shell',
}


/*
 * =========================================================
 * Facebook Main Feed
 * =========================================================
 *
 * IMPORTANT:
 *
 * Every direct child gets the SAME 12px spacing.
 *
 * Stories
 * ↓ 12px
 * Composer
 * ↓ 12px
 * Post
 * ↓ 12px
 * Post
 * ↓ 12px
 * Newly dragged Post
 *
 * PostWidget itself should NOT have mt-* / mb-*.
 */

export function FacebookMainFeed({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <main
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`min-w-0 flex-1 overflow-y-auto bg-[#f0f2f5] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      <div className="mx-auto flex w-full max-w-[580px] flex-col gap-3 px-3 py-4">
        {children}
      </div>
    </main>
  )
}


FacebookMainFeed.craft = {
  displayName:
    'Facebook Main Feed',
}


/*
 * =========================================================
 * Facebook Composer Card
 * =========================================================
 */

export function FacebookComposerCard({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <section
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`w-full overflow-hidden rounded-xl border border-[#dddfe2] bg-white shadow-sm ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      {children}
    </section>
  )
}


FacebookComposerCard.craft = {
  displayName:
    'Facebook Composer Card',
}


/*
 * =========================================================
 * Facebook Post Card
 * =========================================================
 *
 * Kept for compatibility with old Craft JSON / resolver.
 * New full posts should preferably use PostWidget.
 */

export function FacebookPostCard({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
      drag,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))


  return (
    <article
      ref={(ref) => {
        if (ref) {
          connect(
            drag(ref),
          )
        }
      }}
      className={`w-full cursor-move overflow-hidden rounded-xl border border-[#dddfe2] bg-white text-[#050505] shadow-sm ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      {children}
    </article>
  )
}


FacebookPostCard.craft = {
  displayName:
    'Facebook Post Card',
}


/*
 * =========================================================
 * Facebook Post Header Row
 * =========================================================
 */

export function FacebookHeaderRow({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`flex w-full items-start justify-between px-4 pt-4 ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookHeaderRow.craft = {
  displayName:
    'Facebook Header Row',
}


/*
 * =========================================================
 * Facebook Header Left
 * =========================================================
 */

export function FacebookHeaderLeft({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`flex min-w-0 items-start gap-3 ${
        selected
          ? 'rounded-md ring-1 ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookHeaderLeft.craft = {
  displayName:
    'Facebook Header Left',
}


/*
 * =========================================================
 * Facebook Meta Column
 * =========================================================
 */

export function FacebookMetaColumn({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`min-w-0 flex-1 ${
        selected
          ? 'rounded-md ring-1 ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookMetaColumn.craft = {
  displayName:
    'Facebook Meta Column',
}


/*
 * =========================================================
 * Facebook Body Section
 * =========================================================
 */

export function FacebookBodySection({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`w-full px-4 pb-3 pt-2 ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookBodySection.craft = {
  displayName:
    'Facebook Body Section',
}


/*
 * =========================================================
 * Facebook Image Section
 * =========================================================
 */

export function FacebookImageSection({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`w-full overflow-hidden bg-[#e9edf2] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookImageSection.craft = {
  displayName:
    'Facebook Image Section',
}


/*
 * =========================================================
 * Facebook Footer Section
 * =========================================================
 */

export function FacebookFooterSection({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`w-full bg-white ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookFooterSection.craft = {
  displayName:
    'Facebook Footer Section',
}


/*
 * =========================================================
 * Facebook Action Buttons Row
 * =========================================================
 *
 * Compatibility layout for old widget-composed Facebook posts.
 */

export function FacebookActionButtonsRow({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
    selected,
  } = useCraftContainer()


  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className={`grid min-h-[52px] w-full grid-cols-3 border-t border-[#ced0d4] px-2 ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}


FacebookActionButtonsRow.craft = {
  displayName:
    'Facebook Action Buttons Row',
}