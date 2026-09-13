import { Link, Outlet } from 'react-router-dom'

function MainLayout() {
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
          <Link to="/truth-social">Truth Social</Link>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout