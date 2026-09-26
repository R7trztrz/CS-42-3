import { Component, type ReactNode } from 'react'

type InterfaceErrorBoundaryProps = {
  children: ReactNode
}

type InterfaceErrorBoundaryState = {
  hasError: boolean
}

class InterfaceErrorBoundary extends Component<
  InterfaceErrorBoundaryProps,
  InterfaceErrorBoundaryState
> {
  state: InterfaceErrorBoundaryState = {
    hasError: false,
  }

  static getDerivedStateFromError(): InterfaceErrorBoundaryState {
    return { hasError: true }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-6 py-14 text-center">
          <h3 className="text-lg font-semibold text-[#172033]">Interface unavailable</h3>
          <p className="mt-2 text-sm text-amber-800" role="alert">
            This study interface uses content that this version of the participant view cannot display.
          </p>
        </div>
      )
    }

    return this.props.children
  }
}

export default InterfaceErrorBoundary
