import { useNode } from '@craftjs/core'

type TextWidgetProps = {
  text?: string
  className?: string
}

function TextWidget({
  text = 'Sample text',
  className = '',
}: TextWidgetProps) {
  const {
    connectors: { connect, drag },
  } = useNode()

  return (
    <p
      ref={(ref) => {
        if (ref) connect(drag(ref))
      }}
      className={`cursor-move text-base text-gray-900 ${className}`}
    >
      {text}
    </p>
  )
}

TextWidget.craft = {
  displayName: 'Text Widget',
  props: {
    text: 'Sample text',
    className: '',
  },
}

export default TextWidget