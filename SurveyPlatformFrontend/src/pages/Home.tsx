import { Link } from 'react-router-dom'

const templates = [
  {
    name: 'Facebook',
    description: 'Start with a Facebook-style social media feed.',
    path: '/facebook',
  },
  {
    name: 'Instagram',
    description: 'Start with an Instagram-style visual feed.',
    path: '/instagram',
  },
  {
    name: 'TikTok',
    description: 'Start with a TikTok-style short video feed.',
    path: '/tiktok',
  },
  {
    name: 'X',
    description: 'Start with an X-style text and post feed.',
    path: '/x',
  },
  {
    name: 'Threads',
    description: 'Start with a Threads-style conversation feed.',
    path: '/threads',
  },
  {
    name: 'Bluesky',
    description: 'Start with a Bluesky-style open social feed.',
    path: '/bluesky',
  },
  {
    name: 'Truth Social',
    description: 'Start with a Truth Social-style feed.',
    path: '/truth-social',
  },
]

function Home() {
  return (
    <section className="min-h-screen bg-gray-50 px-6 py-10">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            Choose a starting template
          </h1>

          <p className="mt-2 max-w-2xl text-gray-600">
            Select a social media platform template to start building your study
            feed.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {templates.map((template) => (
            <Link
              key={template.name}
              to={template.path}
              className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:border-blue-400 hover:shadow-md"
            >
              <h2 className="text-xl font-semibold text-gray-900">
                {template.name}
              </h2>

              <p className="mt-2 text-sm leading-6 text-gray-600">
                {template.description}
              </p>

              <span className="mt-4 inline-block text-sm font-medium text-blue-600">
                Use this template
              </span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  )
}

export default Home