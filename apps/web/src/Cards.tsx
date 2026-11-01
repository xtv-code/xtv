import { Link } from 'react-router-dom'
import { type VideoCard as Card, duration, money } from './api'
import { type Locale, useI18n } from './i18n'
import { EmptyState } from './ui'

export function VideoGrid({ items, featured = false, ranked = false, rankOffset = 0 }: { items: Card[]; featured?: boolean; ranked?: boolean; rankOffset?: number }) {
  const { t, locale } = useI18n()
  if (!items.length) {
    return <EmptyState title={t('empty.videos')} />
  }
  return (
    <div className={`grid${featured ? ' featured' : ''}`}>
      {items.map((item, index) => (
        <Link className="card" key={item.id} to={`/videos/${item.id}`}>
          <div className="thumb" style={{ backgroundImage: item.coverUrl ? `url(${item.coverUrl})` : undefined }}>
            {ranked ? <span className="rank-badge">#{rankOffset + index + 1}</span> : null}
            {item.accessType !== 'FREE' && (
              <span className={`access-badge ${item.accessType === 'MEMBER' ? 'member' : 'ppv'}`}>
                {item.accessType === 'MEMBER' ? t('access.member') : item.accessType === 'PPV' ? money(item.priceUsdt) : t('access.free')}
              </span>
            )}
            {item.durationMs ? <span className="badge duration">{duration(item.durationMs)}</span> : null}
            <span className="thumb-stats">
              <span className="play-mark">▶</span>
              <span>{formatViews(item.viewCount, locale)}</span>
            </span>
          </div>
          <div className="meta">
            <strong>{item.title}</strong>
            <span className="sub">
              {item.publishedAt ? <span>{item.publishedAt.slice(0, 10)}</span> : null}
              {item.episodeNo ? <span>{t('card.episode', { no: item.episodeNo })}</span> : null}
              {item.seriesStatus === 'ONGOING' ? <span>{t('series.ongoing')}</span> : null}
              {item.seriesStatus === 'COMPLETED' ? <span>{t('series.completed')}</span> : null}
            </span>
          </div>
        </Link>
      ))}
    </div>
  )
}

export function CategoryTiles({ items }: { items: { name: string; slug: string; coverUrl?: string | null; subtitle?: string }[] }) {
  if (!items.length) return null
  return (
    <div className="cat-tiles">
      {items.map((item) => (
        <Link className="cat-tile" key={item.slug} to={`/categories/${item.slug}`}>
          <div className="cat-tile-bg" style={{ backgroundImage: item.coverUrl ? `url(${item.coverUrl})` : undefined }} />
          <div className="cat-tile-shade" />
          <div className="cat-tile-copy">
            {item.subtitle ? <span className="cat-tile-en">{item.subtitle}</span> : null}
            <strong>{item.name}</strong>
          </div>
        </Link>
      ))}
    </div>
  )
}

export function Pager({ page, total, pageSize, onPage }: { page: number; total: number; pageSize: number; onPage: (page: number) => void }) {
  const { t } = useI18n()
  const pages = Math.max(1, Math.ceil(total / Math.max(pageSize, 1)))
  const windowStart = Math.max(1, Math.min(page - 2, pages - 4))
  const numbers = Array.from({ length: Math.min(5, pages) }, (_, index) => windowStart + index).filter((n) => n >= 1 && n <= pages)
  return (
    <div className="pager">
      <span className="hint">{t('pager.count', { count: total })}</span>
      <button className="btn ghost sm" disabled={page <= 1} onClick={() => onPage(page - 1)}>{t('pager.prev')}</button>
      {numbers.map((n) => (
        <button key={n} type="button" className={`btn ghost sm${n === page ? ' active' : ''}`} onClick={() => onPage(n)}>{n}</button>
      ))}
      <button className="btn ghost sm" disabled={page >= pages} onClick={() => onPage(page + 1)}>{t('pager.next')}</button>
    </div>
  )
}

function formatViews(count: number, locale: Locale) {
  if (locale === 'en') {
    if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`
    return String(count)
  }
  if (count < 10000) return String(count)
  const value = (count / 10000).toFixed(1)
  if (locale === 'ja') return `${value}万`
  if (locale === 'ko') return `${value}만`
  return `${value} 万`
}
