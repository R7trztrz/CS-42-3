import type { ReactNode } from 'react'
import { useNode } from '@craftjs/core'

type ChildrenProps = {
  children?: ReactNode
}

export function TruthSocialDesktopShell({
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
      data-component-type="TruthSocialDesktopShell"
      className="mx-auto h-[860px] w-[1340px] overflow-hidden bg-white text-[#0d2345]"
    >
      {children}
    </div>
  )
}

TruthSocialDesktopShell.craft = {
  displayName: 'Truth Social Desktop Shell',
}

export function TruthSocialMainFeed({
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
      data-component-type="TruthSocialMainFeed"
      className="h-full w-[580px] shrink-0 overflow-y-auto border-x border-[#e6eaf0] bg-white"
    >
      <div
        data-component-id={`${nodeId}:feed-content`}
        data-parent-component-id={nodeId}
        data-component-type="TruthSocialFeedContent"
      >
        {children}
      </div>
    </main>
  )
}

TruthSocialMainFeed.craft = {
  displayName: 'Truth Social Main Feed',
}

export function TruthSocialPostList({
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
      data-component-type="TruthSocialPostList"
      className="flex w-full flex-col"
    >
      {children}
    </div>
  )
}

TruthSocialPostList.craft = {
  displayName: 'Truth Social Post List',
}