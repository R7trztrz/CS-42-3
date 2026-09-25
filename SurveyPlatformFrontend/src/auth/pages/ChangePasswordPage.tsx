import { useState, type FormEvent } from 'react'
import { changePassword } from '../api/authApi'

export default function ChangePasswordPage() {
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [confirmNewPassword, setConfirmNewPassword] = useState('')

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (newPassword.length < 8) {
            setError('New password must be at least 8 characters long.')
            return
        }

        if (newPassword !== confirmNewPassword) {
            setError('New passwords do not match.')
            return
        }

        try {
            setIsSubmitting(true)

            await changePassword({
                currentPassword,
                newPassword,
                confirmNewPassword,
            })

            setSuccess('Password changed successfully.')

            setCurrentPassword('')
            setNewPassword('')
            setConfirmNewPassword('')
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message)
            } else {
                setError('Password change failed.')
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
            <div className="w-full max-w-md rounded-xl bg-white p-8 shadow">
                <h1 className="mb-2 text-2xl font-semibold text-gray-900">
                    Change password
                </h1>

                <p className="mb-6 text-sm text-gray-600">
                    Update the password for your researcher account.
                </p>

                <form
                    onSubmit={handleSubmit}
                    noValidate
                    className="space-y-5"
                >
                    <div>
                        <label
                            htmlFor="currentPassword"
                            className="mb-1 block text-sm font-medium text-gray-700"
                        >
                            Current password
                        </label>

                        <input
                            id="currentPassword"
                            type="password"
                            value={currentPassword}
                            onChange={(event) =>
                                setCurrentPassword(event.target.value)
                            }
                            autoComplete="current-password"
                            required
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="newPassword"
                            className="mb-1 block text-sm font-medium text-gray-700"
                        >
                            New password
                        </label>

                        <input
                            id="newPassword"
                            type="password"
                            value={newPassword}
                            onChange={(event) =>
                                setNewPassword(event.target.value)
                            }
                            autoComplete="new-password"
                            required
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
                        />

                        <p className="mt-1 text-xs text-gray-500">
                            Password must contain at least 8 characters.
                        </p>
                    </div>

                    <div>
                        <label
                            htmlFor="confirmNewPassword"
                            className="mb-1 block text-sm font-medium text-gray-700"
                        >
                            Confirm new password
                        </label>

                        <input
                            id="confirmNewPassword"
                            type="password"
                            value={confirmNewPassword}
                            onChange={(event) =>
                                setConfirmNewPassword(event.target.value)
                            }
                            autoComplete="new-password"
                            required
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-blue-500"
                        />
                    </div>

                    {error && (
                        <div
                            role="alert"
                            className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
                        >
                            {error}
                        </div>
                    )}

                    {success && (
                        <div
                            role="status"
                            className="rounded-lg bg-green-50 px-4 py-3 text-sm text-green-700"
                        >
                            {success}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {isSubmitting
                            ? 'Changing password...'
                            : 'Change password'}
                    </button>
                </form>
            </div>
        </main>
    )
}