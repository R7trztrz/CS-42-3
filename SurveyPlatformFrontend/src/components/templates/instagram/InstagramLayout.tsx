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
 * Instagram Desktop Shell
 * =========================================================
 */

export function InstagramDesktopShell({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramDesktopShell"
      className={`h-[720px] w-full overflow-hidden bg-white ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramDesktopShell.craft = {
  displayName:
    'Instagram Desktop Shell',
}

/*
 * =========================================================
 * Instagram Main Feed
 * =========================================================
 */

export function InstagramMainFeed({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <main
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramMainFeed"
      className={`min-w-0 flex-1 overflow-y-auto bg-white ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      <div
        data-component-id={`${nodeId}:feed-content`}
        data-parent-component-id={nodeId}
        data-component-type="InstagramFeedContent"
        className="mx-auto w-full max-w-[680px] px-4 py-4"
      >
        {children}
      </div>
    </main>
  )
}

InstagramMainFeed.craft = {
  displayName:
    'Instagram Main Feed',
}

/*
 * =========================================================
 * Instagram Post List
 * =========================================================
 */

export function InstagramPostList({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramPostList"
      className={`flex w-full flex-col gap-0 ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramPostList.craft = {
  displayName:
    'Instagram Post List',
}

/*
 * =========================================================
 * Instagram Post Card
 * =========================================================
 */

export function InstagramPostCard({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
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
      data-component-id={nodeId}
      data-component-type="InstagramPostCard"
      className={`w-full cursor-move bg-white text-[#262626] ${
        selected
          ? 'ring-2 ring-emerald-400 ring-offset-2'
          : ''
      }`}
    >
      {children}
    </article>
  )
}

InstagramPostCard.craft = {
  displayName:
    'Instagram Post Card',
}

/*
 * =========================================================
 * Instagram Header Row
 * =========================================================
 */

export function InstagramHeaderRow({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramHeaderRow"
      className={`flex w-full items-center justify-between px-[18px] py-[14px] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramHeaderRow.craft = {
  displayName:
    'Instagram Header Row',
}

/*
 * =========================================================
 * Instagram Header Left
 * =========================================================
 */

export function InstagramHeaderLeft({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramHeaderLeft"
      className={`flex min-w-0 items-center gap-3 ${
        selected
          ? 'rounded-md ring-1 ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramHeaderLeft.craft = {
  displayName:
    'Instagram Header Left',
}

/*
 * =========================================================
 * Instagram Meta Column
 * =========================================================
 */

export function InstagramMetaColumn({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramMetaColumn"
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

InstagramMetaColumn.craft = {
  displayName:
    'Instagram Meta Column',
}

/*
 * =========================================================
 * Instagram Image Section
 * =========================================================
 */

export function InstagramImageSection({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramImageSection"
      className={`aspect-square w-full overflow-hidden bg-[#f4f5f7] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramImageSection.craft = {
  displayName:
    'Instagram Image Section',
}

/*
 * =========================================================
 * Instagram Action Row
 * =========================================================
 */

export function InstagramActionRow({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramActionRow"
      className={`flex w-full items-center justify-between ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramActionRow.craft = {
  displayName:
    'Instagram Action Row',
}

/*
 * =========================================================
 * Instagram Footer Section
 * =========================================================
 */

export function InstagramFooterSection({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramFooterSection"
      className={`w-full bg-white px-[18px] pb-5 pt-[14px] ${
        selected
          ? 'ring-1 ring-inset ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramFooterSection.craft = {
  displayName:
    'Instagram Footer Section',
}

/*
 * =========================================================
 * Instagram Caption Section
 * =========================================================
 */

export function InstagramCaptionSection({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: {
      connect,
    },
    selected,
  } = useNode((node) => ({
    selected:
      node.events.selected,
  }))

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      data-component-id={nodeId}
      data-component-type="InstagramCaptionSection"
      className={`w-full ${
        selected
          ? 'rounded-md ring-1 ring-emerald-300'
          : ''
      }`}
    >
      {children}
    </div>
  )
}

InstagramCaptionSection.craft = {
  displayName:
    'Instagram Caption Section',
}