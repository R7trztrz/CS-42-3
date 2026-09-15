import { createBrowserRouter } from 'react-router-dom'

import LoginPage from '../auth/pages/LoginPage'
import RegisterPage from '../auth/pages/RegisterPage'

import MainLayout from '../layouts/MainLayout'
import Home from '../pages/Home'
import Facebook from '../pages/Facebook'
import Instagram from '../pages/Instagram'
import TikTok from '../pages/TikTok'
import X from '../pages/X'
import Threads from '../pages/Threads'
import Bluesky from '../pages/Bluesky'
import TruthSocial from '../pages/TruthSocial'

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'facebook',
        element: <Facebook />,
      },
      {
        path: 'instagram',
        element: <Instagram />,
      },
      {
        path: 'tiktok',
        element: <TikTok />,
      },
      {
        path: 'x',
        element: <X />,
      },
      {
        path: 'threads',
        element: <Threads />,
      },
      {
        path: 'bluesky',
        element: <Bluesky />,
      },
      {
        path: 'truth-social',
        element: <TruthSocial />,
      },
    ],
  },
])

export default router