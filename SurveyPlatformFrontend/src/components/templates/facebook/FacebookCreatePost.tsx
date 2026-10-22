import { useNode } from '@craftjs/core'
import FacebookIcon from './FacebookIcon'

function FacebookCreatePost() {
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
      data-component-type="FacebookCreatePost"
      className="mx-auto mb-4 w-full max-w-[560px] rounded-xl border border-[#dddfe2] bg-white px-4 py-3 shadow-sm"
    >
      <div
        data-component-id={`${nodeId}:composer`}
        data-parent-component-id={nodeId}
        data-component-type="FacebookComposer"
        className="flex items-center gap-3"
      >
        <div
          data-component-id={`${nodeId}:avatar`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAvatar"
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#5b49ff] text-[10px] font-bold text-white"
        >
          You
        </div>

        <div
          data-component-id={`${nodeId}:input`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerInput"
          className="flex h-10 flex-1 items-center rounded-full bg-[#f0f2f5] px-4 text-[14px] text-[#65676b]"
        >
          What&apos;s on your mind?
        </div>
      </div>

      <div className="my-3 h-px bg-[#e4e6eb]" />

      <div
        data-component-id={`${nodeId}:actions`}
        data-parent-component-id={nodeId}
        data-component-type="ComposerActions"
        className="grid grid-cols-3"
      >
        <div
          data-component-id={`${nodeId}:live-video`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAction"
          className="flex items-center justify-center gap-2 rounded-md py-2 text-[#65676b] hover:bg-[#f0f2f5]"
        >
          <FacebookIcon name="camera" size={20} className="text-[#f3425f]" />
          <span className="text-[13px] font-medium">Live video</span>
        </div>

        <div
          data-component-id={`${nodeId}:photo-video`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAction"
          className="flex items-center justify-center gap-2 rounded-md py-2 text-[#65676b] hover:bg-[#f0f2f5]"
        >
          <FacebookIcon name="photo" size={20} className="text-[#45bd62]" />
          <span className="text-[13px] font-medium">Photo/video</span>
        </div>

        <div
          data-component-id={`${nodeId}:feeling`}
          data-parent-component-id={nodeId}
          data-component-type="ComposerAction"
          className="flex items-center justify-center gap-2 rounded-md py-2 text-[#65676b] hover:bg-[#f0f2f5]"
        >
          <FacebookIcon name="smile" size={20} className="text-[#f7b928]" />
          <span className="text-[13px] font-medium">Feeling</span>
        </div>
      </div>
    </section>
  )
}

FacebookCreatePost.craft = {
  displayName: 'Facebook Create Post',
}

export default FacebookCreatePost