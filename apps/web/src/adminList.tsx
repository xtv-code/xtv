import { isValidElement, type ReactElement, type ReactNode, useEffect, useRef, useState } from 'react'

export type Dir = 'asc' | 'desc'

export function queryString(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    search.set(key, String(value))
  }
  const text = search.toString()
  return text ? `?${text}` : ''
}

function sameFilters(a: Record<string, string>, b: Record<string, string>) {
  const keys = new Set([...Object.keys(a), ...Object.keys(b)])
  for (const key of keys) {
    if ((a[key] || '') !== (b[key] || '')) return false
  }
  return true
}

export function useAdminQuery(defaults: { sort: string; dir?: Dir }) {
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [filters, setFilters] = useState<Record<string, string>>({})
  const [sort, setSort] = useState(defaults.sort)
  const [dir, setDir] = useState<Dir>(defaults.dir || 'desc')

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((current) => (sameFilters(current, draft) ? current : { ...draft }))
    }, 280)
    return () => window.clearTimeout(timer)
  }, [draft])

  function setFilter(key: string, value: string) {
    setDraft((current) => {
      if ((current[key] || '') === value) return current
      const next = { ...current, [key]: value }
      if (!value) delete next[key]
      return next
    })
  }

  function toggleSort(field: string) {
    if (sort === field) setDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    else {
      setSort(field)
      setDir('asc')
    }
  }

  function query(extra: Record<string, string | number | undefined | null> = {}) {
    return queryString({ ...filters, sort, dir, ...extra })
  }

  const requestKey = queryString({ ...filters, sort, dir })
  return { draft, filters, setFilter, sort, dir, toggleSort, query, requestKey }
}

function filterValueOf(filter: ReactNode) {
  if (!isValidElement(filter)) return ''
  const value = (filter as ReactElement<{ value?: string }>).props.value
  return value == null ? '' : String(value)
}

export function SortTh({ field, sort, dir, onSort, children, filter }: {
  field: string
  sort: string
  dir: Dir
  onSort: (field: string) => void
  children: ReactNode
  filter?: ReactNode
}) {
  const root = useRef<HTMLTableCellElement>(null)
  const box = useRef<HTMLDivElement>(null)
  const active = sort === field
  const filtered = Boolean(filterValueOf(filter))
  const [open, setOpen] = useState(false)
  const showFilter = Boolean(filter) && (open || filtered)

  useEffect(() => {
    if (filtered) setOpen(true)
  }, [filtered])

  useEffect(() => {
    if (!showFilter) return
    const node = box.current?.querySelector<HTMLInputElement | HTMLSelectElement>('input, select')
    node?.focus()
  }, [showFilter])

  useEffect(() => {
    if (!open || filtered) return
    function onPointer(event: MouseEvent) {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open, filtered])

  return (
    <th ref={root} className={active || filtered ? 'col-head active' : 'col-head'}>
      <div className="col-head-row">
        <button
          type="button"
          className={`col-label${filtered ? ' filtered' : ''}`}
          onClick={() => filter && setOpen((current) => !current)}
          title={filter ? '点击筛选' : undefined}
        >
          {children}
          {filtered ? <span className="filter-dot" aria-hidden /> : null}
        </button>
        <button
          type="button"
          className="sort-mark-btn"
          onClick={() => onSort(field)}
          title="排序"
          aria-label="排序"
        >
          {active ? (dir === 'asc' ? '↑' : '↓') : '↕'}
        </button>
      </div>
      {showFilter ? <div className="col-filter" ref={box}>{filter}</div> : null}
    </th>
  )
}

export function FilterInput({ value, onChange, placeholder = '筛选' }: {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}) {
  return (
    <input
      className="filter-input"
      value={value}
      placeholder={placeholder}
      onChange={(event) => onChange(event.target.value)}
      onClick={(event) => event.stopPropagation()}
    />
  )
}

export function FilterSelect({ value, onChange, options }: {
  value: string
  onChange: (value: string) => void
  options: { value: string; label: string }[]
}) {
  return (
    <select
      className="filter-input"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      onClick={(event) => event.stopPropagation()}
    >
      <option value="">全部</option>
      {options.map((option) => (
        <option key={option.value} value={option.value}>{option.label}</option>
      ))}
    </select>
  )
}
