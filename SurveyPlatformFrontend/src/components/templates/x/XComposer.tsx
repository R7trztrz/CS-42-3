import {
  useNode,
} from '@craftjs/core'

import XIcon from './XIcon'

function XComposer() {
  const {
    connectors: {
      connect,
    },
  } = useNode()

  return (
    <section
      ref={(ref) => {
        if (ref) {
          connect(ref)
        }
      }}
      className="border-b border-[#2f3336] bg-black px-5 py-4"
    >
      <div className="flex gap-3">

        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#26364a] text-[11px] font-bold text-white">
          You
        </div>

        <div className="min-w-0 flex-1">

          <div className="pb-4 pt-1 text-[20px] leading-6 text-[#536471]">
            What is happening?
          </div>

          <div className="flex items-center justify-between border-t border-[#2f3336] pt-3">

            <div className="flex items-center gap-5 text-[#1d9bf0]">
              <XIcon
                name="image"
                size={20}
              />

              <XIcon
                name="globe"
                size={20}
              />

              <XIcon
                name="poll"
                size={20}
              />

              <XIcon
                name="emoji"
                size={20}
              />
            </div>

            <button
              type="button"
              className="rounded-full bg-[#1d9bf0] px-6 py-2 text-[14px] font-bold text-white"
            >
              Post
            </button>

          </div>
        </div>
      </div>
    </section>
  )
}

XComposer.craft = {
  displayName: 'X Composer',
}

export default XComposer