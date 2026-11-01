import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { type CategoryRow, type PageResult, type PersonCard, type SeriesCard, type TagCard, type VideoCard, api } from './api'
import { CategoryTiles, Pager, VideoGrid } from './Cards'
import { useI18n } from './i18n'
import { EmptyState, PageHeader, Section, SkeletonGrid } from './ui'

export function HomePage() {
  const [data, setData] = useState<{ featured: VideoCard[]; latest: VideoCard[]; hot: VideoCard[]; popularTags: TagCard[]; categoryRows: CategoryRow[] } | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { api<NonNullable<typeof data>>('/api/v1/home').then(setData).catch((e) => setError(e.message)) }, [])
  const { t } = useI18n()
  if (error) return <p className="error">{error}</p>
  if (!data) return <SkeletonGrid count={12} />
  return (
    <>
      {data.categoryRows.length > 0 && (
        <Section title={t('home.categories')} more="/categories" moreLabel={t('home.all')}>
          <CategoryTiles items={data.categoryRows.map((row) => ({
            name: row.name,
            slug: row.slug,
            coverUrl: row.videos.find((v) => v.coverUrl)?.coverUrl || null,
            subtitle: latinSubtitle(row.slug, row.name),
          }))} />
        </Section>
      )}
      {data.featured.length > 0 && (
        <Section title={t('home.featured')} more="/latest" moreLabel={t('more')}>
          <VideoGrid items={data.featured} />
        </Section>
      )}
      <Section title={t('home.latest')} more="/latest" moreLabel={t('more')}>
        <VideoGrid items={data.latest} />
      </Section>
      <Section title={t('home.hot')} more="/ranking" moreLabel={t('more')}>
        <VideoGrid items={data.hot} />
      </Section>
      {data.popularTags.length > 0 && (
        <Section title={t('home.tags')} more="/tags" moreLabel={t('more')}>
          <div className="chips tags-cloud">
            {data.popularTags.map((tag) => (
              <Link className="chip" key={tag.id} to={`/tags/${tag.slug}`}>#{tag.name}</Link>
            ))}
          </div>
        </Section>
      )}
      {data.categoryRows.map((row) => (
        <Section key={row.slug} title={row.name} more={`/categories/${row.slug}`} moreLabel={t('more')}>
          <VideoGrid items={row.videos} />
        </Section>
      ))}
    </>
  )
}

export function LatestPage() {
  const { t } = useI18n()
  return <Paged title={t('latest.title')} path="/api/v1/videos" />
}

export function CategoryVideosPage() {
  const { slug = '' } = useParams()
  const [name, setName] = useState(slug)
  useEffect(() => {
    api<{ items: { name: string; slug: string }[] }>('/api/v1/categories?page=1')
      .then((res) => {
        const hit = res.items.find((item) => item.slug === slug)
        if (hit) setName(hit.name)
      })
      .catch(() => setName(slug))
  }, [slug])
  return <Paged key={slug} title={name} path={`/api/v1/categories/${slug}/videos`} />
}

export function TagVideosPage() {
  const { slug = '' } = useParams()
  return <Paged key={slug} title={`#${slug}`} path={`/api/v1/tags/${slug}/videos`} />
}

export function PersonVideosPage() {
  const { slug = '' } = useParams()
  return <Paged key={slug} title={slug} path={`/api/v1/people/${slug}`} />
}

export function SearchPage() {
  const [params] = useSearchParams()
  const q = params.get('q') || ''
  const { t } = useI18n()
  return <Paged key={q} title={q ? t('search.result', { q }) : t('search.title')} path={`/api/v1/search?q=${encodeURIComponent(q)}`} />
}

export function RankingPage() {
  const [range, setRange] = useState('all')
  const { t } = useI18n()
  const ranges = [
    ['day', t('ranking.day')],
    ['week', t('ranking.week')],
    ['month', t('ranking.month')],
    ['all', t('ranking.all')],
  ] as const
  return (
    <>
      <PageHeader title={t('ranking.title')} />
      <div className="chips range-tabs">
        {ranges.map(([id, label]) => (
          <button key={id} type="button" className={`chip${range === id ? ' active' : ''}`} onClick={() => setRange(id)}>{label}</button>
        ))}
      </div>
      <Paged key={range} title="" path={`/api/v1/ranking?range=${range}`} ranked />
    </>
  )
}

function Paged({ title, path, ranked = false }: { title: string; path: string; ranked?: boolean }) {
  const [page, setPage] = useState(1)
  const [data, setData] = useState<PageResult<VideoCard> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    setLoading(true)
    setError('')
    const join = path.includes('?') ? '&' : '?'
    api<PageResult<VideoCard>>(`${path}${join}page=${page}`)
      .then((next) => { setData(next); setError('') })
      .catch((e) => { setError(e.message); setData(null) })
      .finally(() => setLoading(false))
  }, [path, page])
  return (
    <>
      {title ? <PageHeader title={title} /> : null}
      {error ? <p className="error">{error}</p> : null}
      {loading && !data ? <SkeletonGrid /> : error ? null : <VideoGrid items={data?.items || []} ranked={ranked} rankOffset={data ? (data.page - 1) * data.pageSize : 0} />}
      {data && !error ? <Pager page={data.page} total={data.total} pageSize={data.pageSize} onPage={setPage} /> : null}
    </>
  )
}

export function CategoriesPage() {
  const { t } = useI18n()
  const [page, setPage] = useState(1)
  const [data, setData] = useState<PageResult<{ id: number; name: string; slug: string; coverUrl: string | null }> | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setError('')
    setData(null)
    api<NonNullable<typeof data>>(`/api/v1/categories?page=${page}`).then(setData).catch((e) => setError(e.message))
  }, [page])
  return (
    <>
      <PageHeader title={t('categories.title')} />
      {error ? <p className="error">{error}</p> : !data ? <SkeletonGrid count={8} /> : data.items.length ? (
        <CategoryTiles items={data.items.map((item) => ({
          name: item.name,
          slug: item.slug,
          coverUrl: item.coverUrl,
          subtitle: latinSubtitle(item.slug, item.name),
        }))} />
      ) : <EmptyState title={t('categories.empty')} />}
      {data && !error ? <Pager page={data.page} total={data.total} pageSize={data.pageSize} onPage={setPage} /> : null}
    </>
  )
}

export function TagsPage() {
  const { t } = useI18n()
  const [page, setPage] = useState(1)
  const [data, setData] = useState<PageResult<TagCard> | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setError('')
    setData(null)
    api<PageResult<TagCard>>(`/api/v1/tags?page=${page}`).then(setData).catch((e) => setError(e.message))
  }, [page])
  return (
    <>
      <PageHeader title={t('tags.title')} />
      {error ? <p className="error">{error}</p> : (
        <Section title={t('tags.hot')}>
          {!data ? <SkeletonGrid count={4} /> : data.items.length ? (
            <div className="chips tags-cloud">
              {data.items.map((item) => (
                <Link className="chip" key={item.id} to={`/tags/${item.slug}`}>#{item.name}</Link>
              ))}
            </div>
          ) : <EmptyState title={t('tags.empty')} />}
        </Section>
      )}
      {data && !error ? <Pager page={data.page} total={data.total} pageSize={data.pageSize} onPage={setPage} /> : null}
    </>
  )
}

export function PeoplePage() {
  const { t } = useI18n()
  const [kind, setKind] = useState('PERFORMER')
  const [page, setPage] = useState(1)
  const [data, setData] = useState<PageResult<PersonCard> | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setData(null)
    setError('')
    api<PageResult<PersonCard>>(`/api/v1/people?kind=${kind}&page=${page}`).then(setData).catch((e) => setError(e.message))
  }, [kind, page])
  return (
    <>
      <PageHeader title={t('people.title')} />
      <div className="chips range-tabs">
        <button type="button" className={`chip${kind === 'PERFORMER' ? ' active' : ''}`} onClick={() => { setPage(1); setKind('PERFORMER') }}>{t('people.performer')}</button>
        <button type="button" className={`chip${kind === 'CREATOR' ? ' active' : ''}`} onClick={() => { setPage(1); setKind('CREATOR') }}>{t('people.creator')}</button>
      </div>
      {error ? <p className="error">{error}</p> : !data ? <SkeletonGrid /> : data.items.length ? (
        <div className="grid people-grid">
          {data.items.map((person) => (
            <Link className="card person-card" key={person.id} to={`/people/${person.slug}`}>
              <div className="thumb person-thumb" style={{ backgroundImage: person.avatarUrl ? `url(${person.avatarUrl})` : undefined }} />
              <div className="meta"><strong>{person.name}</strong><span className="sub">{t('people.works', { count: person.works })}</span></div>
            </Link>
          ))}
        </div>
      ) : <EmptyState title={t('people.empty')} />}
      {data && !error ? <Pager page={data.page} total={data.total} pageSize={data.pageSize} onPage={setPage} /> : null}
    </>
  )
}

export function SeriesListPage() {
  const { t } = useI18n()
  const [page, setPage] = useState(1)
  const [data, setData] = useState<PageResult<SeriesCard> | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setError('')
    setData(null)
    api<PageResult<SeriesCard>>(`/api/v1/series?page=${page}`).then(setData).catch((e) => setError(e.message))
  }, [page])
  return (
    <>
      <PageHeader title={t('series.title')} />
      {error ? <p className="error">{error}</p> : !data ? <SkeletonGrid /> : data.items.length ? (
        <div className="grid">
          {data.items.map((item) => (
            <Link className="card" key={item.id} to={`/series/${item.slug}`}>
              <div className="thumb" style={{ backgroundImage: item.coverUrl ? `url(${item.coverUrl})` : undefined }} />
              <div className="meta">
                <strong>{item.title}</strong>
                <span className="sub">{t('series.meta', { count: item.episodes, status: item.status === 'COMPLETED' ? t('series.completed') : t('series.ongoing') })}</span>
              </div>
            </Link>
          ))}
        </div>
      ) : <EmptyState title={t('series.empty')} />}
      {data && !error ? <Pager page={data.page} total={data.total} pageSize={data.pageSize} onPage={setPage} /> : null}
    </>
  )
}

function latinSubtitle(slug: string, name: string) {
  const text = slug.replace(/-/g, ' ').trim()
  if (!text || text === name) return undefined
  if (!/^[a-z0-9 ]+$/i.test(text)) return undefined
  return text
}

export function SeriesDetailPage() {
  const { slug = '' } = useParams()
  const { t } = useI18n()
  const [page, setPage] = useState(1)
  const [data, setData] = useState<{ series: SeriesCard; episodes: PageResult<VideoCard> } | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setData(null)
    setError('')
    api<NonNullable<typeof data>>(`/api/v1/series/${slug}?page=${page}`).then(setData).catch((e) => setError(e.message))
  }, [slug, page])
  if (error) return <p className="error">{error}</p>
  if (!data) return <SkeletonGrid />
  return (
    <>
      <PageHeader
        title={data.series.title}
        description={t('series.meta', { count: data.series.episodes, status: data.series.status === 'COMPLETED' ? t('series.completed') : t('series.ongoing') })}
      />
      <VideoGrid items={data.episodes.items} />
      <Pager page={data.episodes.page} total={data.episodes.total} pageSize={data.episodes.pageSize} onPage={setPage} />
    </>
  )
}
