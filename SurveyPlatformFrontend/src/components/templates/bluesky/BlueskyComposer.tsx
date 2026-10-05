import { useNode } from '@craftjs/core'
import BlueskyIcon from './BlueskyIcon'

function BlueskyComposer() {
  const { connectors: { connect } } = useNode()

  return (
    <section ref={(ref) => { if (ref) connect(ref) }} className="border-b border-[#e6edf5] bg-white px-4 py-4">
      <div className="flex gap-3">
        <div className="flex h-[42px] w-[42px] shrink-0 items-center justify-center rounded-full bg-[#e6f1ff] text-[10px] font-bold text-[#1185fe]">You</div>
        <div className="min-w-0 flex-1">
          <p className="pt-1 text-[17px] text-[#94a3b8]">What's up?</p>
          <div className="mt-5 flex items-center justify-between">
            <div className="flex items-center gap-4 text-[#1185fe]">
              <button type="button" className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-[#eff6ff]" title="Add image">
                <BlueskyIcon name="image" size={19} />
              </button>
              <button type="button" className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-[#eff6ff]" title="Add link">
                <BlueskyIcon name="link" size={19} />
              </button>
            </div>
            <button type="button" className="rounded-full bg-[#1185fe] px-5 py-2 text-[14px] font-semibold text-white">Post</button>
          </div>
        </div>
      </div>
    </section>
  )
}

BlueskyComposer.craft = { displayName: 'Bluesky Composer' }

export default BlueskyComposer
