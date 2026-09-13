import { ApiError } from '../api/http'

interface ErrorMessageProps {
  error: unknown
  onRetry?: () => void
}

function describe(error: unknown): string {
  if (error instanceof ApiError) {
    return `${error.message} (${error.code})`
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Something went wrong'
}

export function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
  const fields = error instanceof ApiError ? error.fields : undefined

  return (
    <div className="error-message" role="alert">
      <strong>Request failed</strong>
      <p>{describe(error)}</p>
      {fields && (
        <ul>
          {Object.entries(fields).map(([field, message]) => (
            <li key={field}>
              <code>{field}</code>: {message}
            </li>
          ))}
        </ul>
      )}
      {onRetry && (
        <button type="button" className="button button--secondary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}
