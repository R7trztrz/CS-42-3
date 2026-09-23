import { createBrowserRouter } from 'react-router-dom'

import MainLayout from '../layouts/MainLayout'
import Home from '../pages/Home'
import Facebook from '../pages/Facebook'
import Instagram from '../pages/Instagram'
import TikTok from '../pages/TikTok'
import X from '../pages/X'
import Threads from '../pages/Threads'
import Bluesky from '../pages/Bluesky'
import TruthSocial from '../pages/TruthSocial'

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

      // M4 研究者端 UI：题库管理（FR-32~35）和问卷编排器（FR-36~39）。
      // 还没有研究列表页，所以 /researcher/questions 和问卷编排器的
      // studyId 会兜底到一个示例 id —— 见 QuestionnaireEditorPage。
      {
        path: 'researcher/questions',
        element: <QuestionBankListPage />,
      },
      {
        path: 'researcher/questions/new',
        element: <QuestionFormPage />,
      },
      {
        path: 'researcher/questions/:questionId/edit',
        element: <QuestionFormPage />,
      },
      {
        path: 'researcher/studies/:studyId/questionnaire',
        element: <QuestionnaireEditorPage />,
      },
    ],
  },

  // M5 参与者端 UI：独立于 MainLayout 的整体界面外壳，
  // 因为参与者不应该看到研究者的导航栏。
  {
    path: 'participate/:studyId',
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
