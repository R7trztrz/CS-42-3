import { useNode } from '@craftjs/core'

function ThreadsComposer() {
  const {
    connectors: { connect },
  } = useNode()

  return (
    <section
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      className="border-b border-[#eeeeee] bg-white px-4 pb-5 pt-4"
    >
      <div className="flex gap-3">
        <div className="relative flex w-[44px] shrink-0 flex-col items-center">
          <div className="flex h-[40px] w-[40px] items-center justify-center rounded-full bg-[#eef0f3] text-[10px] font-bold text-[#5d6570]">
            You
          </div>
          <div className="mt-2 h-[18px] w-px bg-[#d9dce1]" />
          <div className="mt-2 h-[18px] w-[18px] rounded-full bg-[#e3e5e8]" />
        </div>

        <div className="min-w-0 flex-1 pt-[1px]">
          <p className="text-[14px] font-semibold text-[#111111]">
            yourname
          </p>
          <p className="mt-2 text-[15px] text-[#a4a8b0]">
            Start a thread...
          </p>
          <p className="mt-7 text-[14px] text-[#a4a8b0]">
            Add to thread
          </p>
        </div>
      </div>
    </section>
  )
}

ThreadsComposer.craft = {
  displayName: 'Threads Composer',
}

export default ThreadsComposer
