import WidgetEditor from '../pages/prototypes/WidgetEditor'

import { createBrowserRouter, Navigate } from 'react-router-dom'

import LoginPage from '../auth/pages/LoginPage'
import RegisterPage from '../auth/pages/RegisterPage'
import ProtectedRoute from '../auth/components/ProtectedRoute'
import ChangePasswordPage from '../auth/pages/ChangePasswordPage'

import MainLayout from '../layouts/MainLayout'
import Facebook from '../pages/prototypes/Facebook'
import Instagram from '../pages/prototypes/Instagram'
import TikTok from '../pages/prototypes/TikTok'
import X from '../pages/prototypes/X'
import Threads from '../pages/prototypes/Threads'
import Bluesky from '../pages/prototypes/Bluesky'
import TruthSocial from '../pages/prototypes/TruthSocial'
import NewInterface from '../pages/prototypes/NewInterface'
import ResearcherDashboard from '../pages/researcher/ResearcherDashboard'
import ResearcherEdit from '../pages/researcher/ResearcherEdit'
import StudyList from '../pages/researcher/StudyList'

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
    element: <ProtectedRoute />,
    children: [
      {
        path: '/researcher-dashboard',
        element: <ResearcherDashboard />,
      },
      {
        path: '/studies/:studyId/edit',
        element: <ResearcherEdit />,
      },
      {
        path: '/studies',
        element: <StudyList />,
      },
      {
        path: '/new-interface',
        element: <NewInterface />,
      },
      {
        path: '/',
        element: <MainLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/researcher-dashboard" replace />,
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
          {
            path: 'widget-editor',
            element: <WidgetEditor />,
          },
          {
            path: 'account/change-password',
            element: <ChangePasswordPage />,
          },
        ],
      },
    ],
  },
])

export default router
