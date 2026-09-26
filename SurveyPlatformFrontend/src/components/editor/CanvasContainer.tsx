import { useNode } from '@craftjs/core'

function CanvasContainer({ children }: { children?: React.ReactNode }) {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <div
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="min-h-[32rem] w-full rounded-md border border-dashed border-[#b8c6d4] bg-[#f8fafc] p-6"
    >
      {children}
    </div>
  )
}

CanvasContainer.craft = {
  displayName: 'Canvas Container',
}

export default CanvasContainer
