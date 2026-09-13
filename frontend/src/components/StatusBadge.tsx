const labels: Record<string, string> = {
  ACTIVE: 'Active',
  DISABLED: 'Disabled',
  ON_SALE: 'On sale',
  OFF_SALE: 'Off sale',
  CREATED: 'Created',
  PAID: 'Paid',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
}

export function StatusBadge({ status }: { status: string }) {
  const modifier = status.toLowerCase().replace(/_/g, '-')
  return <span className={`badge badge--${modifier}`}>{labels[status] ?? status}</span>
}
