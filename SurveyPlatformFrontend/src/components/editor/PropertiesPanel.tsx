import { useEditor } from '@craftjs/core'

import { createElement } from 'react'

import type {
  ChangeEvent,
  ElementType,
} from 'react'

import type { PlatformStyle } from '../widgets/types'


const propertyControlClass =
  'w-full rounded-sm border border-[#cbd6e2] bg-white px-3 py-2 text-sm outline-none focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600'


function PropertiesPanel() {
  const {
    selectedNodeId,
    selectedNodeName,
    selectedNodeProps,
    selectedNodeParent,
    selectedNodeIndex,
    siblingCount,
    actions,
    query,
  } = useEditor((state, query) => {
    const selectedNodeId =
      Array.from(
        state.events.selected,
      )[0]

    if (!selectedNodeId) {
      return {
        selectedNodeId: null,
        selectedNodeName: null,
        selectedNodeProps: null,
        selectedNodeParent: null,
        selectedNodeIndex: -1,
        siblingCount: 0,
      }
    }

    const selectedNode =
      state.nodes[selectedNodeId]

    const parentId =
      selectedNode?.data?.parent

    let selectedNodeIndex = -1
    let siblingCount = 0

    if (
      parentId &&
      state.nodes[parentId]
    ) {
      const siblings =
        state.nodes[
          parentId
        ].data.nodes || []

      selectedNodeIndex =
        siblings.indexOf(
          selectedNodeId,
        )

      siblingCount =
        siblings.length
    }

    return {
      selectedNodeId,

      selectedNodeName:
        selectedNode?.data
          ?.displayName ||
        selectedNode?.data
          ?.name ||
        'Widget',

      selectedNodeProps:
        query
          .node(
            selectedNodeId,
          )
          .get()
          .data.props,

      selectedNodeParent:
        parentId || null,

      selectedNodeIndex,

      siblingCount,
    }
  })


  /*
   * Editable widgets
   */
  const isWidget =
    selectedNodeName ===
      'Text Widget' ||
    selectedNodeName ===
      'Avatar Widget' ||
    selectedNodeName ===
      'Image Widget' ||
    selectedNodeName ===
      'Action Bar Widget' ||
    selectedNodeName ===
      'Post Widget'


  /*
   * =====================================
   * Text Widget
   * =====================================
   */

  const handleTextChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.text =
          event.target.value
      },
    )
  }


  /*
   * =====================================
   * Avatar Widget
   * =====================================
   */

  const handleAvatarSizeChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.size =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handleAvatarSrcChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.src =
          event.target.value
      },
    )
  }


  const handleAvatarUsernameChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.username =
          event.target.value
      },
    )
  }


  const handleAvatarTimeChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.time =
          event.target.value
      },
    )
  }


  /*
   * =====================================
   * Image Widget
   * =====================================
   */

  const handleImageSrcChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.src =
          event.target.value
      },
    )
  }


  /*
   * =====================================
   * Action Bar Widget
   * =====================================
   */

  const handleLikesChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.likes =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handleCommentsChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.comments =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handleSharesChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.shares =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handleCaptionChange = (
    event: ChangeEvent<HTMLTextAreaElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.caption =
          event.target.value
      },
    )
  }


  /*
   * =====================================
   * Post Widget
   * =====================================
   */

  const handlePostDisplayNameChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.displayName =
          event.target.value
      },
    )
  }


  const handlePostUsernameChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.username =
          event.target.value
      },
    )
  }


  const handlePostTimeChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.time =
          event.target.value
      },
    )
  }


  const handlePostAvatarSrcChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.avatarSrc =
          event.target.value
      },
    )
  }


  const handlePostImageSrcChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.imageSrc =
          event.target.value
        props.assetId =
          undefined
      },
    )
  }


  const handleRemovePostAsset = () => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.assetId =
          undefined
        props.imageSrc =
          ''
      },
    )
  }


  const handlePostCaptionChange = (
    event: ChangeEvent<HTMLTextAreaElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.caption =
          event.target.value
      },
    )
  }


  const handlePostLikesChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.likes =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handlePostCommentsChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.comments =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handlePostSharesChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.shares =
          Number(
            event.target.value,
          )
      },
    )
  }


  /*
   * TikTok-specific Post properties
   */

  const handlePostSavesChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.saves =
          Number(
            event.target.value,
          )
      },
    )
  }


  const handlePostSoundChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.sound =
          event.target.value
      },
    )
  }


  /*
   * =====================================
   * Platform style
   * =====================================
   */

  const handleStyleChange = (
    event: ChangeEvent<HTMLSelectElement>,
  ) => {
    if (!selectedNodeId) {
      return
    }

    actions.setProp(
      selectedNodeId,
      (props) => {
        props.styleId =
          event.target
            .value as PlatformStyle
      },
    )
  }


  /*
   * =====================================
   * Widget Actions
   * =====================================
   */

  const handleDelete = () => {
    if (
      !selectedNodeId ||
      !isWidget
    ) {
      return
    }

    actions.delete(
      selectedNodeId,
    )
  }


  const handleDuplicate = () => {
    if (
      !selectedNodeId ||
      !selectedNodeParent ||
      !isWidget
    ) {
      return
    }

    const selectedNode =
      query
        .node(
          selectedNodeId,
        )
        .get()

    const NodeComponent =
      selectedNode.data
        .type as ElementType

    const duplicatedElement =
      createElement(
        NodeComponent,
        selectedNode.data.props,
      )

    const duplicatedTree =
      query
        .parseReactElement(
          duplicatedElement,
        )
        .toNodeTree()

    actions.addNodeTree(
      duplicatedTree,
      selectedNodeParent,
      selectedNodeIndex + 1,
    )
  }


  const handleMoveUp = () => {
    if (
      !selectedNodeId ||
      !selectedNodeParent ||
      selectedNodeIndex <= 0 ||
      !isWidget
    ) {
      return
    }

    actions.move(
      selectedNodeId,
      selectedNodeParent,
      selectedNodeIndex - 1,
    )
  }


  const handleMoveDown = () => {
    if (
      !selectedNodeId ||
      !selectedNodeParent ||
      selectedNodeIndex < 0 ||
      selectedNodeIndex >=
        siblingCount - 1 ||
      !isWidget
    ) {
      return
    }

    actions.move(
      selectedNodeId,
      selectedNodeParent,
      selectedNodeIndex + 2,
    )
  }


  const handleUndo = () => {
    actions.history.undo()
  }


  const handleRedo = () => {
    actions.history.redo()
  }


  return (
    <div className="w-full p-4">

      <p className="text-xs font-semibold uppercase text-sky-700">
        Selection
      </p>


      <h2 className="mt-1 text-lg font-semibold text-[#172033]">
        Properties
      </h2>


      {/* Undo / Redo */}

      <div className="mb-4 mt-4 grid grid-cols-2 gap-2">

        <button
          type="button"
          onClick={
            handleUndo
          }
          className="rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
        >
          Undo
        </button>


        <button
          type="button"
          onClick={
            handleRedo
          }
          className="rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
        >
          Redo
        </button>

      </div>


      {!selectedNodeId ? (
        <p className="mt-4 text-sm leading-6 text-[#52667d]">
          Select a widget to edit
          its properties.
        </p>
      ) : (
        <div className="mt-5 space-y-4">

          {/* Selected widget */}

          <div className="rounded-sm border border-sky-200 bg-sky-50 px-3 py-3">

            <p className="text-sm text-gray-500">
              Selected widget
            </p>

            <p className="font-semibold text-sky-950">
              {
                selectedNodeName
              }
            </p>

          </div>


          {isWidget && (
            <>

              {/* Platform Style */}

              <div>

                <label
                  htmlFor="style-property"
                  className="mb-1 block text-sm font-medium text-gray-700"
                >
                  Platform Style
                </label>

                <select
                  id="style-property"
                  value={
                    selectedNodeProps
                      ?.styleId ??
                    'facebook'
                  }
                  onChange={
                    handleStyleChange
                  }
                  className={
                    propertyControlClass
                  }
                >
                  <option value="facebook">
                    Facebook
                  </option>

                  <option value="instagram">
                    Instagram
                  </option>

                  <option value="tiktok">
                    TikTok
                  </option>

                  <option value="x">
                    X
                  </option>

                  <option value="threads">
                    Threads
                  </option>

                  <option value="bluesky">
                    Bluesky
                  </option>

                  <option value="truth-social">
                    Truth Social
                  </option>
                </select>

              </div>


              {/* =====================================
                  Text Widget
                 ===================================== */}

              {selectedNodeName ===
                'Text Widget' && (
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
                    value={
                      selectedNodeProps
                        ?.text ??
                      ''
                    }
                    onChange={
                      handleTextChange
                    }
                    className={
                      propertyControlClass
                    }
                  />

                </div>
              )}


              {/* =====================================
                  Avatar Widget
                 ===================================== */}

              {selectedNodeName ===
                'Avatar Widget' && (
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
                      value={
                        selectedNodeProps
                          ?.size ??
                        40
                      }
                      onChange={
                        handleAvatarSizeChange
                      }
                      className={
                        propertyControlClass
                      }
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
                      value={
                        selectedNodeProps
                          ?.src ??
                        ''
                      }
                      onChange={
                        handleAvatarSrcChange
                      }
                      placeholder="https://example.com/avatar.jpg"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {selectedNodeProps
                    ?.styleId ===
                    'instagram' && (
                    <>
                      <div>

                        <label
                          htmlFor="avatar-username"
                          className="mb-1 block text-sm font-medium text-gray-700"
                        >
                          Username
                        </label>

                        <input
                          id="avatar-username"
                          type="text"
                          value={
                            selectedNodeProps
                              ?.username ??
                            ''
                          }
                          onChange={
                            handleAvatarUsernameChange
                          }
                          placeholder="@username"
                          className={
                            propertyControlClass
                          }
                        />

                      </div>


                      <div>

                        <label
                          htmlFor="avatar-time"
                          className="mb-1 block text-sm font-medium text-gray-700"
                        >
                          Time
                        </label>

                        <input
                          id="avatar-time"
                          type="text"
                          value={
                            selectedNodeProps
                              ?.time ??
                            ''
                          }
                          onChange={
                            handleAvatarTimeChange
                          }
                          placeholder="2h ago"
                          className={
                            propertyControlClass
                          }
                        />

                      </div>
                    </>
                  )}

                </div>
              )}


              {/* =====================================
                  Image Widget
                 ===================================== */}

              {selectedNodeName ===
                'Image Widget' && (
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
                    value={
                      selectedNodeProps
                        ?.src ??
                      ''
                    }
                    onChange={
                      handleImageSrcChange
                    }
                    placeholder="https://example.com/image.jpg"
                    className={
                      propertyControlClass
                    }
                  />

                </div>
              )}


              {/* =====================================
                  Action Bar Widget
                 ===================================== */}

              {selectedNodeName ===
                'Action Bar Widget' && (
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
                      value={
                        selectedNodeProps
                          ?.likes ??
                        0
                      }
                      onChange={
                        handleLikesChange
                      }
                      className={
                        propertyControlClass
                      }
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
                      value={
                        selectedNodeProps
                          ?.comments ??
                        0
                      }
                      onChange={
                        handleCommentsChange
                      }
                      className={
                        propertyControlClass
                      }
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
                      value={
                        selectedNodeProps
                          ?.shares ??
                        0
                      }
                      onChange={
                        handleSharesChange
                      }
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {selectedNodeProps
                    ?.styleId ===
                    'instagram' && (
                    <div>

                      <label
                        htmlFor="caption-property"
                        className="mb-1 block text-sm font-medium text-gray-700"
                      >
                        Caption
                      </label>

                      <textarea
                        id="caption-property"
                        value={
                          selectedNodeProps
                            ?.caption ??
                          ''
                        }
                        onChange={
                          handleCaptionChange
                        }
                        placeholder="@username Write a caption..."
                        rows={4}
                        className={`${propertyControlClass} resize-y`}
                      />

                    </div>
                  )}

                </div>
              )}


              {/* =====================================
                  Post Widget
                 ===================================== */}

              {selectedNodeName ===
                'Post Widget' && (
                <div className="space-y-4">

                  <div className="rounded-sm border border-emerald-200 bg-emerald-50 px-3 py-3">

                    <p className="text-xs font-semibold uppercase text-emerald-700">
                      Complete Post
                    </p>

                    <p className="mt-1 text-xs leading-5 text-emerald-900">
                      Edit all content for this
                      social media post.
                    </p>

                  </div>


                  {/* Display Name */}

                  <div>

                    <label
                      htmlFor="post-display-name"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      Display Name
                    </label>

                    <input
                      id="post-display-name"
                      type="text"
                      value={
                        selectedNodeProps
                          ?.displayName ??
                        ''
                      }
                      onChange={
                        handlePostDisplayNameChange
                      }
                      placeholder="Emma Wilson"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {/* Username */}

                  <div>

                    <label
                      htmlFor="post-username"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      Username
                    </label>

                    <input
                      id="post-username"
                      type="text"
                      value={
                        selectedNodeProps
                          ?.username ??
                        ''
                      }
                      onChange={
                        handlePostUsernameChange
                      }
                      placeholder="@emmaw"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {/* Time */}

                  <div>

                    <label
                      htmlFor="post-time"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      Time
                    </label>

                    <input
                      id="post-time"
                      type="text"
                      value={
                        selectedNodeProps
                          ?.time ??
                        ''
                      }
                      onChange={
                        handlePostTimeChange
                      }
                      placeholder="2h ago"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {/* Avatar URL */}

                  <div>

                    <label
                      htmlFor="post-avatar-src"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      Avatar URL
                    </label>

                    <input
                      id="post-avatar-src"
                      type="text"
                      value={
                        selectedNodeProps
                          ?.avatarSrc ??
                        ''
                      }
                      onChange={
                        handlePostAvatarSrcChange
                      }
                      placeholder="https://example.com/avatar.jpg"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {/* Media URL */}

                  <div>

                    {selectedNodeProps
                      ?.assetId && (
                      <div className="mb-3 rounded-sm border border-emerald-200 bg-emerald-50 px-3 py-3">
                        <p className="text-xs font-semibold uppercase text-emerald-700">
                          Imported image
                        </p>

                        <p className="mt-1 text-xs leading-5 text-emerald-900">
                          This post uses an image stored with the study.
                        </p>

                        <button
                          type="button"
                          onClick={handleRemovePostAsset}
                          className="mt-2 text-xs font-semibold text-red-700 hover:text-red-900"
                        >
                          Remove image
                        </button>
                      </div>
                    )}

                    <label
                      htmlFor="post-image-src"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      {selectedNodeProps
                        ?.styleId ===
                      'tiktok'
                        ? 'Media URL'
                        : 'Post Image URL'}
                    </label>

                    <input
                      id="post-image-src"
                      type="text"
                      value={
                        selectedNodeProps
                          ?.imageSrc ??
                        ''
                      }
                      onChange={
                        handlePostImageSrcChange
                      }
                      placeholder="https://example.com/media.jpg"
                      className={
                        propertyControlClass
                      }
                    />

                  </div>


                  {/* Caption */}

                  <div>

                    <label
                      htmlFor="post-caption"
                      className="mb-1 block text-sm font-medium text-gray-700"
                    >
                      Caption
                    </label>

                    <textarea
                      id="post-caption"
                      value={
                        selectedNodeProps
                          ?.caption ??
                        ''
                      }
                      onChange={
                        handlePostCaptionChange
                      }
                      placeholder="Write the post content..."
                      rows={5}
                      className={`${propertyControlClass} resize-y`}
                    />

                  </div>


                  {/* TikTok Sound */}

                  {selectedNodeProps
                    ?.styleId ===
                    'tiktok' && (
                    <div>

                      <label
                        htmlFor="post-sound"
                        className="mb-1 block text-sm font-medium text-gray-700"
                      >
                        Sound
                      </label>

                      <input
                        id="post-sound"
                        type="text"
                        value={
                          selectedNodeProps
                            ?.sound ??
                          ''
                        }
                        onChange={
                          handlePostSoundChange
                        }
                        placeholder="Original Sound · Emma Wilson"
                        className={
                          propertyControlClass
                        }
                      />

                    </div>
                  )}


                  {/* Engagement */}

                  <div>

                    <p className="mb-3 text-sm font-medium text-gray-700">
                      Engagement
                    </p>


                    <div className="space-y-3">

                      {/* Likes */}

                      <div>

                        <label
                          htmlFor="post-likes"
                          className="mb-1 block text-xs font-medium text-gray-600"
                        >
                          Likes
                        </label>

                        <input
                          id="post-likes"
                          type="number"
                          min="0"
                          value={
                            selectedNodeProps
                              ?.likes ??
                            0
                          }
                          onChange={
                            handlePostLikesChange
                          }
                          className={
                            propertyControlClass
                          }
                        />

                      </div>


                      {/* Comments */}

                      <div>

                        <label
                          htmlFor="post-comments"
                          className="mb-1 block text-xs font-medium text-gray-600"
                        >
                          Comments
                        </label>

                        <input
                          id="post-comments"
                          type="number"
                          min="0"
                          value={
                            selectedNodeProps
                              ?.comments ??
                            0
                          }
                          onChange={
                            handlePostCommentsChange
                          }
                          className={
                            propertyControlClass
                          }
                        />

                      </div>


                      {/* TikTok Saves */}

                      {selectedNodeProps
                        ?.styleId ===
                        'tiktok' && (
                        <div>

                          <label
                            htmlFor="post-saves"
                            className="mb-1 block text-xs font-medium text-gray-600"
                          >
                            Saves
                          </label>

                          <input
                            id="post-saves"
                            type="number"
                            min="0"
                            value={
                              selectedNodeProps
                                ?.saves ??
                              0
                            }
                            onChange={
                              handlePostSavesChange
                            }
                            className={
                              propertyControlClass
                            }
                          />

                        </div>
                      )}


                      {/* Shares */}

                      <div>

                        <label
                          htmlFor="post-shares"
                          className="mb-1 block text-xs font-medium text-gray-600"
                        >
                          Shares
                        </label>

                        <input
                          id="post-shares"
                          type="number"
                          min="0"
                          value={
                            selectedNodeProps
                              ?.shares ??
                            0
                          }
                          onChange={
                            handlePostSharesChange
                          }
                          className={
                            propertyControlClass
                          }
                        />

                      </div>

                    </div>

                  </div>

                </div>
              )}


              {/* =====================================
                  Widget Actions
                 ===================================== */}

              <div className="border-t border-gray-200 pt-4">

                <p className="mb-2 text-sm font-medium text-gray-700">
                  Widget Actions
                </p>


                <div className="grid grid-cols-2 gap-2">

                  <button
                    type="button"
                    onClick={
                      handleMoveUp
                    }
                    disabled={
                      selectedNodeIndex <=
                      0
                    }
                    className="rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Move Up
                  </button>


                  <button
                    type="button"
                    onClick={
                      handleMoveDown
                    }
                    disabled={
                      selectedNodeIndex <
                        0 ||
                      selectedNodeIndex >=
                        siblingCount - 1
                    }
                    className="rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Move Down
                  </button>

                </div>


                <button
                  type="button"
                  onClick={
                    handleDuplicate
                  }
                  className="mt-2 w-full rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
                >
                  Duplicate Widget
                </button>


                <button
                  type="button"
                  onClick={
                    handleDelete
                  }
                  className="mt-2 w-full rounded bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-700"
                >
                  Delete Widget
                </button>

              </div>

            </>
          )}

        </div>
      )}

    </div>
  )
}


export default PropertiesPanel
