import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { type ContentNames as ContentNameMap } from './api'
import { useI18n } from './i18n'

export const CONTENT_LOCALES = [
  { id: 'zh', label: '中文' },
  { id: 'en', label: 'English' },
  { id: 'ja', label: '日本語' },
  { id: 'ko', label: '한국어' },
] as const

export type ContentLocaleId = (typeof CONTENT_LOCALES)[number]['id']
export type ContentNames = Record<ContentLocaleId, string>

export function contentNames(names?: ContentNameMap | null, primary = ''): ContentNames {
  return {
    zh: names?.zh ?? primary,
    en: names?.en ?? '',
    ja: names?.ja ?? '',
    ko: names?.ko ?? '',
  }
}

export function primaryName(names: ContentNames) {
  return names.zh.trim() || names.en.trim() || names.ja.trim() || names.ko.trim()
}

export function LocaleFields({ label, value, onChange, onFocus, active, maxLength = 300, rows = 0, hint, required = false }: {
  label: string
  value: ContentNames
  onChange: (next: ContentNames) => void
  onFocus?: (locale: ContentLocaleId) => void
  active?: ContentLocaleId
  maxLength?: number
  rows?: number
  hint?: string
  required?: boolean
}) {
  return (
    <fieldset className="locale-fields">
      <legend>{label}{required ? <span className="req">必填</span> : null}</legend>
      {CONTENT_LOCALES.map((item) => (
        <label key={item.id} className={`${active === item.id ? 'active' : ''} ${rows > 1 ? 'multiline' : ''}`.trim()}>
          <span>{item.label}</span>
          {rows > 1 ? (
            <textarea
              value={value[item.id]}
              maxLength={maxLength}
              rows={rows}
              onFocus={() => onFocus?.(item.id)}
              onChange={(event) => onChange({ ...value, [item.id]: event.target.value })}
            />
          ) : (
            <input
              value={value[item.id]}
              maxLength={maxLength}
              onFocus={() => onFocus?.(item.id)}
              onChange={(event) => onChange({ ...value, [item.id]: event.target.value })}
            />
          )}
        </label>
      ))}
      {hint === '' ? null : <p className="hint">{hint || '至少填写一种语言。前台按访问语言显示，没填的语言沿用已填写的名称。'}</p>}
    </fieldset>
  )
}

export function PageHeader({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) {
  return (
    <div className="page-header video-title-row">
      <div>
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {actions}
    </div>
  )
}

export function Section({ title, more, moreLabel, children }: { title: string; more?: string; moreLabel?: string; children: ReactNode }) {
  const { t } = useI18n()
  return (
    <section className="section">
      <div className="section-head">
        <h2>{title}</h2>
        {more ? <Link to={more}>{moreLabel || t('more')}</Link> : null}
      </div>
      {children}
    </section>
  )
}

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      {description ? <p>{description}</p> : null}
    </div>
  )
}

export function Spinner() {
  const { t } = useI18n()
  return <div className="spinner" role="status" aria-label={t('loading')} />
}

export function SkeletonGrid({ count = 8 }: { count?: number }) {
  return (
    <div className="skeleton-grid" aria-hidden>
      {Array.from({ length: count }, (_, index) => (
        <div className="skeleton-card" key={index}>
          <div className="skeleton-thumb" />
          <div className="skeleton-body">
            <div className="skeleton-line" />
            <div className="skeleton-line short" />
          </div>
        </div>
      ))}
    </div>
  )
}

export function Badge({ children, tone = 'default' }: { children: ReactNode; tone?: 'default' | 'ok' | 'warn' | 'bad' | 'info' }) {
  return <span className={`status ${tone === 'default' ? '' : tone}`.trim()}>{children}</span>
}

export function Panel({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`panel ${className}`.trim()}>{children}</div>
}
