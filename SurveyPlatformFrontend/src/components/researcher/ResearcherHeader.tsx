import { Link, useNavigate } from 'react-router-dom'

type ResearcherHeaderProps = {
  activePage?: 'dashboard' | 'studies'
}

const navigationItems = [
  { label: 'Dashboard', page: 'dashboard', to: '/researcher-dashboard' },
  { label: 'Study List', page: 'studies', to: '/studies' },
] as const

function ResearcherHeader({ activePage }: ResearcherHeaderProps) {
  const navigate = useNavigate()

  const handleLogout = () => {
    localStorage.removeItem('researcherToken')
    localStorage.removeItem('researcherTokenType')

    navigate('/login', { replace: true })
  }

  return (
    <header className="text-white">
      <div className="bg-[#101b2b] px-6 py-5 md:px-10">
        <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-6">
          <div className="flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-emerald-500 text-sm font-bold text-[#101b2b]">
              SP
            </div>

            <div>
              <p className="text-xs font-semibold uppercase text-emerald-300">
                Researcher workspace
              </p>

              <h1 className="mt-1 text-xl font-semibold">
                Survey Platform
              </h1>
            </div>
          </div>

          <div className="flex items-center gap-3 text-sm">
            <Link
              to="/account/change-password"
              className="font-medium text-gray-300 hover:text-white"
            >
              Account
            </Link>

            <button
              type="button"
              onClick={handleLogout}
              className="rounded-sm border border-[#52647b] px-3 py-2 font-semibold text-white hover:border-emerald-400 hover:bg-[#1d2b3f]"
            >
              Logout
            </button>
          </div>
        </div>
      </div>

      <nav
        aria-label="Researcher pages"
        className="border-b border-[#314158] bg-[#1d2b3f] px-6 md:px-10"
      >
        <div className="mx-auto flex w-full max-w-7xl gap-8">
          {navigationItems.map((item) => {
            const isActive = item.page === activePage

            const className = `border-b-2 px-1 py-3 text-sm ${
              isActive
                ? 'border-emerald-400 font-semibold text-white'
                : 'border-transparent font-medium text-gray-300 hover:text-white'
            }`

            return (
              <Link
                key={item.label}
                to={item.to}
                className={className}
              >
                {item.label}
              </Link>
            )
          })}
        </div>
      </nav>
    </header>
  )
}

export default ResearcherHeader