import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Hls from 'hls.js'
import { type AdView, type OrderView, type PageResult, type PaymentChannel, type PlayResult, type VideoCard, type VideoDetail, api, authToken, favorites, payChannelKey, toggleFavorite } from './api'
import { Pager, VideoGrid } from './Cards'
import { useI18n } from './i18n'
import { EmptyState, Section, SkeletonGrid } from './ui'

export function VideoPage() {
  const { id = '' } = useParams()
  const [detail, setDetail] = useState<VideoDetail | null>(null)
  const [play, setPlay] = useState<PlayResult | null>(null)
  const [relatedPage, setRelatedPage] = useState(1)
  const [samePage, setSamePage] = useState(1)
  const [related, setRelated] = useState<PageResult<VideoCard> | null>(null)
  const [same, setSame] = useState<PageResult<VideoCard> | null>(null)
  const [ad, setAd] = useState<AdView | null>(null)
  const [saved, setSaved] = useState(favorites().includes(Number(id)))
  const [copied, setCopied] = useState(false)
  const videoRef = useRef<HTMLVideoElement>(null)
  const counted = useRef(false)

  const [loadError, setLoadError] = useState('')
  const { t } = useI18n()
  useEffect(() => {
    counted.current = false
    setLoadError('')
    setDetail(null)
    setPlay(null)
    setSaved(favorites().includes(Number(id)))
    setRelatedPage(1)
    setSamePage(1)
    Promise.all([
      api<VideoDetail>(`/api/v1/videos/${id}`),
      api<PlayResult>(`/api/v1/videos/${id}/play`),
    ]).then(([nextDetail, nextPlay]) => {
      setDetail(nextDetail)
      setPlay(nextPlay)
    }).catch((e) => setLoadError(e instanceof Error ? e.message : t('load.fail')))
    api<AdView | null>('/api/v1/ads?slot=PLAYER_BOTTOM').then(setAd).catch(() => undefined)
    if (authToken()) {
      api<{ saved: boolean }>(`/api/v1/me/favorites/${id}`).then((state) => setSaved(state.saved)).catch(() => undefined)
    }
  }, [id, t])

  useEffect(() => {
    api<PageResult<VideoCard>>(`/api/v1/videos/${id}/related?page=${relatedPage}`).then(setRelated).catch(() => setRelated(null))
  }, [id, relatedPage])

  useEffect(() => {
    api<PageResult<VideoCard>>(`/api/v1/videos/${id}/same-people?page=${samePage}`).then(setSame).catch(() => setSame(null))
  }, [id, samePage])

  const showPlayer = Boolean(detail && play?.allowed && play.playUrl)
  useEffect(() => {
    const video = videoRef.current
    if (!showPlayer || !video || !play?.playUrl) return
    const nativeHls = video.canPlayType('application/vnd.apple.mpegurl') === 'probably'
    if (nativeHls || !Hls.isSupported()) {
      video.src = play.playUrl
      return () => {
        video.removeAttribute('src')
        video.load()
      }
    }
    const hls = new Hls()
    hls.loadSource(play.playUrl)
    hls.attachMedia(video)
    return () => hls.destroy()
  }, [showPlayer, play?.playUrl])

  function onPlay() {
    if (counted.current) return
    counted.current = true
    api(`/api/v1/videos/${id}/views`, { method: 'POST' }).catch(() => undefined)
  }

  async function copy() {
    await navigator.clipboard.writeText(window.location.href)
    setCopied(true)
  }

  function openShare(target: string) {
    const url = encodeURIComponent(window.location.href)
    const links: Record<string, string> = {
      x: `https://twitter.com/intent/tweet?url=${url}`,
      telegram: `https://t.me/share/url?url=${url}`,
      line: `https://social-plugins.line.me/lineit/share?url=${url}`,
    }
    window.open(links[target], '_blank', 'noopener')
  }

  if (loadError) return <EmptyState title={t('video.missing')} description={loadError} />
  if (!detail || !play) return <SkeletonGrid count={4} />

  return (
    <>
      <div className="video-title-row">
        <div>
          <h1>{detail.title}</h1>
          <p className="sub">
            <span>{t('video.plays', { count: detail.viewCount })}</span>
            {detail.publishedAt && <span>{detail.publishedAt.slice(0, 10)}</span>}
            <span>{detail.accessType === 'FREE' ? t('access.free') : detail.accessType === 'MEMBER' ? t('access.member') : t('access.ppv')}</span>
          </p>
        </div>
      </div>

      <div className="player-stage">
        {play.allowed && play.playUrl ? (
          <div className="player-wrap">
            <video ref={videoRef} controls playsInline onPlay={onPlay} />
          </div>
        ) : play.allowed ? (
          <div className="paywall">
            <h2>{t('video.unplayable')}</h2>
            <p>{t('video.later')}</p>
          </div>
        ) : (
          <Paywall play={play} videoId={detail.id} />
        )}
      </div>

      {ad && (
        <a className="player-ad" href={ad.targetUrl} target="_blank" rel="noreferrer">
          <img src={ad.imageUrl} alt="" />
        </a>
      )}

      <div className="row-actions">
        <button className="btn" onClick={() => {
          if (!authToken()) {
            setSaved(toggleFavorite(detail.id))
            return
          }
          api<{ saved: boolean }>(`/api/v1/me/favorites/${detail.id}`, { method: 'PUT' }).then((state) => setSaved(state.saved)).catch(() => undefined)
        }}>{saved ? t('video.saved') : t('video.save')}</button>
        <button className="btn ghost" onClick={copy}>{copied ? t('video.copied') : t('video.copy')}</button>
        <button className="btn ghost" onClick={() => openShare('x')}>{t('video.shareX')}</button>
        <button className="btn ghost" onClick={() => openShare('telegram')}>Telegram</button>
        <button className="btn ghost" onClick={() => openShare('line')}>LINE</button>
        <Link className="btn ghost" to={`/report?video=${detail.id}`}>{t('video.report')}</Link>
      </div>

      <div className="chips">
        {detail.categories.map((item) => <Link className="chip" key={item.id} to={`/categories/${item.slug}`}>{item.name}</Link>)}
        {detail.tags.map((item) => <Link className="chip" key={item.id} to={`/tags/${item.slug}`}>#{item.name}</Link>)}
      </div>

      {detail.people.length > 0 && (
        <Section title={t('video.cast')}>
          <div className="cast-row">
            {detail.people.map((person) => (
              <Link className="cast-person" key={person.id} to={`/people/${person.slug}`}>
                {person.avatarUrl
                  ? <img className="cast-photo" src={person.avatarUrl} alt="" />
                  : <span className="cast-photo placeholder">{person.name.slice(0, 1)}</span>}
                <span>{person.name}</span>
              </Link>
            ))}
          </div>
        </Section>
      )}

      {detail.seriesSlug && (
        <p className="sub" style={{ marginTop: 16 }}>
          <Link to={`/series/${detail.seriesSlug}`}>{t('video.episode', { title: detail.seriesTitle || '', no: detail.episodeNo || '' })}</Link>
        </p>
      )}

      {same && same.total > 0 && (
        <Section title={t('video.same')}>
          <VideoGrid items={same.items} />
          <Pager page={same.page} total={same.total} pageSize={same.pageSize} onPage={setSamePage} />
        </Section>
      )}
      {detail.tags.length > 0 && related && (
        <Section title={t('video.tags')}>
          {related.total ? <VideoGrid items={related.items} /> : <EmptyState title={t('video.none')} />}
          <Pager page={related.page} total={related.total} pageSize={related.pageSize} onPage={setRelatedPage} />
        </Section>
      )}
    </>
  )
}

function Paywall({ play, videoId }: { play: PlayResult; videoId: number }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const signedIn = Boolean(authToken())
  const [channels, setChannels] = useState<PaymentChannel[]>([])
  const [error, setError] = useState('')
  useEffect(() => { api<PaymentChannel[]>('/api/v1/payment-channels').then(setChannels).catch(() => setChannels([])) }, [])
  const back = `/videos/${videoId}`
  async function buy(paymentMethod?: string) {
    if (!signedIn) {
      navigate(`/login?back=${encodeURIComponent(back)}`)
      return
    }
    try {
      const order = await api<OrderView>('/api/v1/orders', { method: 'POST', body: JSON.stringify({ type: 'PPV', videoId, paymentMethod }) })
      navigate(`/orders/${order.id}?back=${encodeURIComponent(back)}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : t('pay.unavailable'))
    }
  }
  return (
    <div className="paywall">
      <h2>{play.accessType === 'MEMBER' ? t('pay.member') : t('pay.ppv')}</h2>
      <p>
        {play.accessType === 'PPV' && play.priceUsdt != null
          ? t('pay.price', { price: Number(play.priceUsdt).toFixed(2) })
          : t('pay.hint')}
      </p>
      {error && <p className="error">{error}</p>}
      <div className="row-actions" style={{ justifyContent: 'center' }}>
        {play.accessType === 'PPV' && channels.length === 0 && <p className="sub">{t('pay.unavailable')}</p>}
        {play.accessType === 'PPV' && channels.length > 1 && signedIn && channels.map((channel) => (
          <button className="btn" key={channel.method} onClick={() => buy(channel.method)}>{t(payChannelKey(channel.method))}</button>
        ))}
        {play.accessType === 'PPV' && channels.length === 1 && signedIn && <button className="btn" onClick={() => buy(channels[0].method)}>{t('pay.buy')}</button>}
        {play.accessType === 'PPV' && channels.length > 0 && !signedIn && <button className="btn" onClick={() => buy()}>{t('pay.loginBuy')}</button>}
        {play.accessType === 'MEMBER' && !signedIn && <Link className="btn" to={`/login?back=${encodeURIComponent(back)}`}>{t('pay.loginOpen')}</Link>}
        {play.accessType === 'MEMBER' && play.plans.map((plan) => <Link className="btn ghost" key={plan.id} to={signedIn ? `/membership?back=${encodeURIComponent(back)}` : `/login?back=${encodeURIComponent(back)}`}>{plan.name}</Link>)}
        {!play.plans.length && play.accessType === 'MEMBER' && signedIn && <Link className="btn" to={`/membership?back=${encodeURIComponent(back)}`}>{t('pay.open')}</Link>}
      </div>
    </div>
  )
}
