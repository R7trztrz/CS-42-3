import { Link, Outlet, useNavigate } from 'react-router-dom'

function MainLayout() {
    const navigate = useNavigate()

    const handleLogout = () => {
        localStorage.removeItem('researcherToken')
        localStorage.removeItem('researcherTokenType')

        navigate('/login', { replace: true })
    }
  return (
    <div>
      <header>
        <h2>CS-42 Survey Platform</h2>

        <nav>
          <Link to="/">Home</Link>{' | '}
          <Link to="/facebook">Facebook</Link>{' | '}
          <Link to="/instagram">Instagram</Link>{' | '}
          <Link to="/tiktok">TikTok</Link>{' | '}
          <Link to="/x">X</Link>{' | '}
          <Link to="/threads">Threads</Link>{' | '}
          <Link to="/bluesky">Bluesky</Link>{' | '}
          <Link to="/truth-social">Truth Social</Link>{' | '}
          <Link to="/account/change-password">Change password</Link>{' | '}
          <button type="button" onClick={handleLogout}>
              Logout
          </button>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout