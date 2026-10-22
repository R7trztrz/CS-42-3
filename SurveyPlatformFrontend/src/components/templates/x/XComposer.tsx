import {
  useNode,
} from '@craftjs/core'

import XIcon from './XIcon'

function XComposer() {
  const {
    id: nodeId,
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
      data-component-id={nodeId}
      data-component-type="XComposer"
      className="border-b border-[#2f3336] bg-black px-5 py-4"
    >
      <div
        data-component-id={`${nodeId}:composer-row`}
        data-parent-component-id={nodeId}
        data-component-type="XComposerRow"
        className="flex gap-3"
      >
        <div
          data-component-id={`${nodeId}:avatar`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAvatar"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#26364a] text-[11px] font-bold text-white"
        >
          You
        </div>

        <div className="min-w-0 flex-1">
          <div
            data-component-id={`${nodeId}:input`}
            data-parent-component-id={nodeId}
            data-component-type="ComposerInput"
            className="pb-4 pt-1 text-[20px] leading-6 text-[#536471]"
          >
            What is happening?
          </div>

          <div
            data-component-id={`${nodeId}:actions`}
            data-parent-component-id={nodeId}
            data-component-type="ComposerActions"
            className="flex items-center justify-between border-t border-[#2f3336] pt-3"
          >
            <div className="flex items-center gap-5 text-[#1d9bf0]">
              {[
                'image',
                'globe',
                'poll',
                'emoji',
              ].map((name) => (
                <div
                  key={name}
                  data-component-id={`${nodeId}:action-${name}`}
                  data-parent-component-id={nodeId}
                  data-component-type="ComposerAction"
                >
                  <XIcon
                    name={name as
                      | 'image'
                      | 'globe'
                      | 'poll'
                      | 'emoji'
                    }
                    size={20}
                  />
                </div>
              ))}
            </div>

            <button
              type="button"
              data-component-id={`${nodeId}:submit`}
              data-parent-component-id={nodeId}
              data-component-type="ComposerSubmit"
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