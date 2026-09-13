interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  const lastPage = Math.max(totalPages - 1, 0)

  return (
    <div className="pagination">
      <span className="pagination-info">
        {totalElements} item{totalElements === 1 ? '' : 's'} &middot; page {page + 1} of{' '}
        {Math.max(totalPages, 1)}
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          className="button button--secondary"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="button button--secondary"
          disabled={page >= lastPage}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  )
}
