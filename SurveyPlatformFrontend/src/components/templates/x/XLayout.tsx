import type {
  ReactNode,
} from 'react'

import {
  useNode,
} from '@craftjs/core'

type ChildrenProps = {
  children?: ReactNode
}

export function XDesktopShell({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="mx-auto h-[860px] w-[1340px] overflow-hidden bg-black text-white"
    >
      {children}
    </div>
  )
}

XDesktopShell.craft = {
  displayName: 'X Desktop Shell',
}

export function XMainFeed({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <main
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="h-full w-[700px] shrink-0 overflow-y-auto border-x border-[#2f3336] bg-black"
    >
      {children}
    </main>
  )
}

XMainFeed.craft = {
  displayName: 'X Main Feed',
}

export function XPostList({
  children,
}: ChildrenProps) {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="flex w-full flex-col"
    >
      {children}
    </div>
  )
}

XPostList.craft = {
  displayName: 'X Post List',
}