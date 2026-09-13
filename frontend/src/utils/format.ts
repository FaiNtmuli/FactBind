/**
 * Formats an ISO-8601 timestamp coming from the backend for display.
 */
export function formatDateTime(value?: string): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

/**
 * Formats a money value the same way in every page.
 */
export function formatAmount(value?: number): string {
  if (value === undefined || value === null) {
    return '-'
  }
  return value.toFixed(2)
}
