import { useNode } from '@craftjs/core'

type LayoutProps = {
  children?: React.ReactNode
}

export function FacebookComposerCard({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="mb-4 w-full max-w-[580px] rounded-2xl border border-[#e4e6eb] bg-white px-5 py-4 shadow-sm"
    >
      {children}
    </div>
  )
}

FacebookComposerCard.craft = {
  displayName: 'Facebook Composer Card',
}


export function FacebookPostCard({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="w-full max-w-[580px] overflow-hidden rounded-2xl border border-[#e4e6eb] bg-white shadow-sm"
    >
      {children}
    </div>
  )
}

FacebookPostCard.craft = {
  displayName: 'Facebook Post Card',
}


export function FacebookHeaderRow({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="flex items-start justify-between gap-4 px-5 pt-5"
    >
      {children}
    </div>
  )
}

FacebookHeaderRow.craft = {
  displayName: 'Facebook Header Row',
}


export function FacebookHeaderLeft({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="flex items-start gap-3"
    >
      {children}
    </div>
  )
}

FacebookHeaderLeft.craft = {
  displayName: 'Facebook Header Left',
}


export function FacebookMetaColumn({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="flex flex-col gap-1"
    >
      {children}
    </div>
  )
}

FacebookMetaColumn.craft = {
  displayName: 'Facebook Meta Column',
}


export function FacebookBodySection({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="px-5 pb-4 pt-3"
    >
      {children}
    </div>
  )
}

FacebookBodySection.craft = {
  displayName: 'Facebook Body Section',
}


export function FacebookImageSection({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="w-full overflow-hidden"
    >
      {children}
    </div>
  )
}

FacebookImageSection.craft = {
  displayName: 'Facebook Image Section',
}


export function FacebookFooterSection({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="px-5 py-3"
    >
      {children}
    </div>
  )
}

FacebookFooterSection.craft = {
  displayName: 'Facebook Footer Section',
}


export function FacebookActionButtonsRow({
  children,
}: LayoutProps) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="grid grid-cols-3 border-t border-[#e4e6eb] px-3 py-2"
    >
      {children}
    </div>
  )
}

FacebookActionButtonsRow.craft = {
  displayName: 'Facebook Action Buttons Row',
}