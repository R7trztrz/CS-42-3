import type { ReactNode } from 'react'
import { useNode } from '@craftjs/core'

type ChildrenProps = {
  children?: ReactNode
}

export function BlueskyDesktopShell({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      data-component-id={nodeId}
      data-component-type="BlueskyDesktopShell"
      className="mx-auto h-[860px] w-[1340px] overflow-hidden bg-white text-[#0f172a]"
    >
      {children}
    </div>
  )
}

BlueskyDesktopShell.craft = {
  displayName: 'Bluesky Desktop Shell',
}

export function BlueskyMainFeed({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <main
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      data-component-id={nodeId}
      data-component-type="BlueskyMainFeed"
      className="h-full w-[570px] shrink-0 overflow-y-auto border-x border-[#e6edf5] bg-white"
    >
      <div
        data-component-id={`${nodeId}:feed-content`}
        data-parent-component-id={nodeId}
        data-component-type="BlueskyFeedContent"
      >
        {children}
      </div>
    </main>
  )
}

BlueskyMainFeed.craft = {
  displayName: 'Bluesky Main Feed',
}

export function BlueskyPostList({
  children,
}: ChildrenProps) {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      data-component-id={nodeId}
      data-component-type="BlueskyPostList"
      className="flex w-full flex-col"
    >
      {children}
    </div>
  )
}

BlueskyPostList.craft = {
  displayName: 'Bluesky Post List',
}