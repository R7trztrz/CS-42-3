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
import ParticipationPage from '../pages/participant/ParticipationPage'

import QuestionBankListPage from '../researcher/questionBank/pages/QuestionBankListPage'
import QuestionFormPage from '../researcher/questionBank/pages/QuestionFormPage'
import QuestionnaireEditorPage from '../researcher/questionnaire/pages/QuestionnaireEditorPage'

import ParticipantSessionLayout from '../participant/session/pages/ParticipantSessionLayout'
import SessionEntryPage from '../participant/session/pages/SessionEntryPage'
import ConsentPage from '../participant/session/pages/ConsentPage'
import FeedPage from '../participant/session/pages/FeedPage'
import QuestionnairePage from '../participant/session/pages/QuestionnairePage'
import SessionCompletePage from '../participant/session/pages/SessionCompletePage'

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
    path: '/participate/:token',
    element: <ParticipationPage />,
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

      // M4 researcher UI: question bank (FR-32~35), owned by the
      // researcher rather than any single study, and the per-study
      // questionnaire editor (FR-36~39), linked to from ResearcherEdit.
      {
        path: '/questions',
        element: <QuestionBankListPage />,
      },
      {
        path: '/questions/new',
        element: <QuestionFormPage />,
      },
      {
        path: '/questions/:questionId/edit',
        element: <QuestionFormPage />,
      },
      {
        path: '/studies/:studyId/questionnaire',
        element: <QuestionnaireEditorPage />,
      },
    ],
  },

  // M5 participant UI preview (mock-backed, no real backend yet - see
  // participant/session/api/sessionApi.ts). Kept under /preview to avoid
  // colliding with the real tokenized /participate/:token entry route
  // above; this is a researcher-facing dev tool, not something a
  // participant would ever reach.
  {
    path: '/preview/participate/:studyId',
    element: <ParticipantSessionLayout />,
    children: [
      { index: true, element: <SessionEntryPage /> },
      { path: 'consent', element: <ConsentPage /> },
      { path: 'feed', element: <FeedPage /> },
      { path: 'questionnaire', element: <QuestionnairePage /> },
      { path: 'complete', element: <SessionCompletePage /> },
    ],
  },
])

export default router
