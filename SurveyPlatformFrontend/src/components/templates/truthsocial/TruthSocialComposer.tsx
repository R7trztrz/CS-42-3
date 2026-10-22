import { useNode } from '@craftjs/core'
import TruthSocialIcon from './TruthSocialIcon'

function TruthSocialComposer() {
  const {
    id: nodeId,
    connectors: { connect },
  } = useNode()

  return (
    <section
      ref={(ref) => {
        if (ref) connect(ref)
      }}
      data-component-id={nodeId}
      data-component-type="TruthSocialComposer"
      className="border-b border-[#e6eaf0] bg-white px-4 py-4"
    >
      <div
        data-component-id={`${nodeId}:composer-row`}
        data-parent-component-id={nodeId}
        data-component-type="TruthSocialComposerRow"
        className="flex gap-3"
      >
        <div
          data-component-id={`${nodeId}:avatar`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAvatar"
          className="flex h-[42px] w-[42px] shrink-0 items-center justify-center rounded-full bg-[#edf0f4] text-[10px] font-bold text-[#687386]"
        >
          You
        </div>

        <div className="min-w-0 flex-1">
          <p
            data-component-id={`${nodeId}:input`}
            data-parent-component-id={nodeId}
            data-component-type="ComposerInput"
            className="pt-1 text-[17px] text-[#9aa4b3]"
          >
            Truth it...
          </p>

          <div
            data-component-id={`${nodeId}:actions`}
            data-parent-component-id={nodeId}
            data-component-type="ComposerActions"
            className="mt-5 flex items-center justify-between"
          >
            <div className="flex items-center gap-4 text-[#ef1f2c]">
              <button
                type="button"
                data-component-id={`${nodeId}:add-image`}
                data-parent-component-id={nodeId}
                data-component-type="ComposerAction"
                className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-[#fff1f2]"
                title="Add image"
              >
                <TruthSocialIcon name="image" size={18} />
              </button>

              <button
                type="button"
                data-component-id={`${nodeId}:add-link`}
                data-parent-component-id={nodeId}
                data-component-type="ComposerAction"
                className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-[#fff1f2]"
                title="Add link"
              >
                <TruthSocialIcon name="link" size={18} />
              </button>

              <button
                type="button"
                data-component-id={`${nodeId}:add-poll`}
                data-parent-component-id={nodeId}
                data-component-type="ComposerAction"
                className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-[#fff1f2]"
                title="Add poll"
              >
                <TruthSocialIcon name="poll" size={18} />
              </button>
            </div>

            <button
              type="button"
              data-component-id={`${nodeId}:submit`}
              data-parent-component-id={nodeId}
              data-component-type="ComposerSubmit"
              className="rounded-full bg-[#ef1f2c] px-5 py-2 text-[14px] font-semibold text-white"
            >
              Truth!
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}

TruthSocialComposer.craft = {
  displayName: 'Truth Social Composer',
}

export default TruthSocialComposer