export class ApiError extends Error {
    code: string
    status: number

    constructor(code: string, message: string, status: number) {
        super(message)

        this.name = 'ApiError'
        this.code = code
        this.status = status
    }
}