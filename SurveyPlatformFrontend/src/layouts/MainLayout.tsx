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

        {/* M4 研究者入口；还没有研究列表页，问卷编排器暂时链接到
            兜底的示例 study id。 */}
        <nav>
          <Link to="/researcher/questions">题库管理</Link>{' | '}
          <Link to="/researcher/studies/demo-study/questionnaire">问卷编排器</Link>
          {' | '}
          <Link to="/participate/demo-study">预览参与者流程（M5）</Link>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout