import type { ReactNode } from 'react'
import { useNode } from '@craftjs/core'

type ChildrenProps = {
  children?: ReactNode
}

export function ThreadsDesktopShell({
  children,
}: ChildrenProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="mx-auto h-[860px] w-[1340px] overflow-hidden bg-white text-[#101010]"
    >
      {children}
    </div>
  )
}

ThreadsDesktopShell.craft = {
  displayName: 'Threads Desktop Shell',
}

export function ThreadsMainFeed({
  children,
}: ChildrenProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <main
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="h-full w-[540px] shrink-0 overflow-y-auto bg-white"
    >
      {children}
    </main>
  )
}

ThreadsMainFeed.craft = {
  displayName: 'Threads Main Feed',
}

export function ThreadsPostList({
  children,
}: ChildrenProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="flex w-full flex-col"
    >
      {children}
    </div>
  )
}

ThreadsPostList.craft = {
  displayName: 'Threads Post List',
}
