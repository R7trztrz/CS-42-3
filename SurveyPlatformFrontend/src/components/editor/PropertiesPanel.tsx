import { useEditor } from '@craftjs/core'
import type { PlatformStyle } from '../widgets/types'

const propertyControlClass = 'w-full rounded-sm border border-[#cbd6e2] bg-white px-3 py-2 text-sm outline-none focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600'

function PropertiesPanel() {
  const {
    selectedNodeId,
    selectedNodeName,
    selectedNodeProps,
    actions,
  } = useEditor((state, query) => {
    const selectedNodeId = Array.from(state.events.selected)[0]

    if (!selectedNodeId) {
      return {
        selectedNodeId: null,
        selectedNodeName: null,
        selectedNodeProps: null,
      }
    }

    const selectedNode = state.nodes[selectedNodeId]

    return {
      selectedNodeId,
      selectedNodeName:
        selectedNode?.data?.displayName ||
        selectedNode?.data?.name ||
        'Widget',
      selectedNodeProps: query.node(selectedNodeId).get().data.props,
    }
  })

  const handleTextChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.text = event.target.value
    })
  }

  const handleAvatarSizeChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.size = Number(event.target.value)
    })
  }

  const handleAvatarSrcChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.src = event.target.value
    })
  }

  const handleImageSrcChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.src = event.target.value
    })
  }

  const handleLikesChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.likes = Number(event.target.value)
    })
  }

  const handleCommentsChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.comments = Number(event.target.value)
    })
  }

  const handleSharesChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.shares = Number(event.target.value)
    })
  }

  const handleStyleChange = (
    event: React.ChangeEvent<HTMLSelectElement>
  ) => {
    if (!selectedNodeId) return

    actions.setProp(selectedNodeId, (props) => {
      props.styleId = event.target.value as PlatformStyle
    })
  }

  return (
    <div className="w-full p-4">
      <p className="text-xs font-semibold uppercase text-sky-700">Selection</p>
      <h2 className="mt-1 text-lg font-semibold text-[#172033]">Properties</h2>

      {!selectedNodeId ? (
        <p className="mt-4 text-sm leading-6 text-[#52667d]">
          Select a widget to edit its properties.
        </p>
      ) : (
        <div className="mt-5 space-y-4">
          <div className="rounded-sm border border-sky-200 bg-sky-50 px-3 py-3">
            <p className="text-sm text-gray-500">
              Selected widget
            </p>

            <p className="font-semibold text-sky-950">
              {selectedNodeName}
            </p>
          </div>

          <div>
            <label
              htmlFor="style-property"
              className="mb-1 block text-sm font-medium text-gray-700"
            >
              Platform Style
            </label>

            <select
              id="style-property"
              value={selectedNodeProps?.styleId ?? 'facebook'}
              onChange={handleStyleChange}
              className={propertyControlClass}
            >
              <option value="facebook">Facebook</option>
              <option value="instagram">Instagram</option>
              <option value="tiktok">TikTok</option>
              <option value="x">X</option>
              <option value="threads">Threads</option>
              <option value="bluesky">Bluesky</option>
              <option value="truth-social">Truth Social</option>
            </select>
          </div>

          {selectedNodeName === 'Text Widget' && (
            <div>
              <label
                htmlFor="text-property"
                className="mb-1 block text-sm font-medium text-gray-700"
              >
                Text
              </label>

              <input
                id="text-property"
                type="text"
                value={selectedNodeProps?.text ?? ''}
                onChange={handleTextChange}
                className={propertyControlClass}
              />
            </div>
          )}

          {selectedNodeName === 'Avatar Widget' && (
            <div className="space-y-4">
              <div>
                <label
                  htmlFor="avatar-size"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Size
                </label>

                <input
                  id="avatar-size"
                  type="number"
                  min="20"
                  max="200"
                  value={selectedNodeProps?.size ?? 40}
                  onChange={handleAvatarSizeChange}
                  className={propertyControlClass}
                />
              </div>

              <div>
                <label
                  htmlFor="avatar-src"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Image URL
                </label>

                <input
                  id="avatar-src"
                  type="text"
                  value={selectedNodeProps?.src ?? ''}
                  onChange={handleAvatarSrcChange}
                  placeholder="https://example.com/avatar.jpg"
                  className={propertyControlClass}
                />
              </div>
            </div>
          )}

          {selectedNodeName === 'Image Widget' && (
            <div>
              <label
                htmlFor="image-src"
                className="mb-1 block text-sm font-medium text-gray-700"
              >
                Image URL
              </label>

              <input
                id="image-src"
                type="text"
                value={selectedNodeProps?.src ?? ''}
                onChange={handleImageSrcChange}
                placeholder="https://example.com/image.jpg"
                className={propertyControlClass}
              />
            </div>
          )}

          {selectedNodeName === 'Action Bar Widget' && (
            <div className="space-y-4">
              <div>
                <label
                  htmlFor="likes-property"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Likes
                </label>

                <input
                  id="likes-property"
                  type="number"
                  min="0"
                  value={selectedNodeProps?.likes ?? 0}
                  onChange={handleLikesChange}
                  className={propertyControlClass}
                />
              </div>

              <div>
                <label
                  htmlFor="comments-property"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Comments
                </label>

                <input
                  id="comments-property"
                  type="number"
                  min="0"
                  value={selectedNodeProps?.comments ?? 0}
                  onChange={handleCommentsChange}
                  className={propertyControlClass}
                />
              </div>

              <div>
                <label
                  htmlFor="shares-property"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Shares
                </label>

                <input
                  id="shares-property"
                  type="number"
                  min="0"
                  value={selectedNodeProps?.shares ?? 0}
                  onChange={handleSharesChange}
                  className={propertyControlClass}
                />
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default PropertiesPanel
