import { RouterProvider } from 'react-router-dom'
import router from './routes/AppRouter'

function App() {
  return <RouterProvider router={router} />
}

export default App