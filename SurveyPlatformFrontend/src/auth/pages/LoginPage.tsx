import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { loginResearcher } from '../api/authApi'

export default function LoginPage() {
    const navigate = useNavigate()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')

    const [error, setError] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        setError('')

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

        if (!emailPattern.test(email)) {
            setError('Please enter a valid email address.')
            return
        }

        if (!password) {
            setError('Please enter your password.')
            return
        }

        try {
            setIsSubmitting(true)

            const response = await loginResearcher({
                email,
                password,
            })

            localStorage.setItem('researcherToken', response.token)
            localStorage.setItem('researcherTokenType', response.tokenType)

            navigate('/researcher-dashboard', { replace: true })
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message)
            } else {
                setError('Login failed.')
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
            <div className="w-full max-w-md rounded-xl bg-white p-8 shadow">
                <h1 className="mb-2 text-2xl font-semibold text-gray-900">
                    Researcher login
                </h1>

                <p className="mb-6 text-sm text-gray-600">
                    Log in to manage your research studies.
                </p>

                <form onSubmit={handleSubmit} noValidate className="space-y-5">
                    <div>
                        <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700">
                            Email
                        </label>

                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            autoComplete="email"
                            required
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
                        />
                    </div>

                    <div>
                        <label htmlFor="password" className="mb-1 block text-sm font-medium text-gray-700">
                            Password
                        </label>

                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            autoComplete="current-password"
                            required
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
                        />
                    </div>

                    {error && (
                        <div role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {isSubmitting ? 'Logging in...' : 'Log in'}
                    </button>
                    <div className="text-center">
                        <p className="mb-2 text-sm text-gray-600">
                            Don't have an account?
                        </p>

                        <Link
                            to="/register"
                            className="block w-full rounded-lg border border-blue-600 px-4 py-2 font-medium text-blue-600 hover:bg-blue-50"
                        >
                            Create account
                        </Link>
                    </div>
                </form>
            </div>
        </main>
    )
}
