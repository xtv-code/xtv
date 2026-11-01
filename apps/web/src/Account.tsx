import { type FormEvent, useEffect, useRef, useState } from 'react'
import QRCode from 'qrcode'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { type Me, type OrderView, type PaymentChannel, type PlanView, type VideoCard, adoptLocalFavorites, api, authToken, favorites, formatPay, payAddressKey, payChannelKey, payNetworkKey, sameSitePath, setAuthToken } from './api'
import { pendingInstall } from './pwa'
import { Pager, VideoGrid } from './Cards'
import { useI18n } from './i18n'
import { Badge, EmptyState, PageHeader, Panel } from './ui'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { t } = useI18n()
  async function submit(event: FormEvent) {
    event.preventDefault()
    try {
      const result = await api<{ token: string }>('/api/v1/auth/login', {
        method: 'POST', body: JSON.stringify({ username, password }),
      })
      setAuthToken(result.token)
      await adoptLocalFavorites().catch(() => undefined)
      navigate(sameSitePath(params.get('back')) || '/account')
    } catch (e) {
      setError(e instanceof Error ? e.message : t('login.fail'))
    }
  }
  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <form className="form" onSubmit={submit}>
          <h1>{t('login.title')}</h1>
          <label>{t('login.user')}
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder={t('login.user')} required minLength={3} maxLength={32} autoComplete="username" />
          </label>
          <label>{t('login.password')}
            <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder={t('login.password')} type="password" required minLength={6} maxLength={72} autoComplete="current-password" />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="btn" type="submit">{t('login.title')}</button>
        </form>
      </div>
    </div>
  )
}

export function AccountPage() {
  const [me, setMe] = useState<Me | null>(null)
  const [orders, setOrders] = useState<OrderView[]>([])
  const [owned, setOwned] = useState<VideoCard[]>([])
  const [tab, setTab] = useState<'owned' | 'orders' | 'password'>('owned')
  const { t } = useI18n()
  useEffect(() => {
    if (!authToken()) return
    api<Me>('/api/v1/me').then((next) => {
      setMe(next)
      if (!next.ownedVideoIds.length) {
        setOwned([])
        return
      }
      api<VideoCard[]>('/api/v1/videos/by-ids', { method: 'POST', body: JSON.stringify({ ids: next.ownedVideoIds }) }).then(setOwned)
    })
    api<OrderView[]>('/api/v1/orders').then(setOrders)
  }, [])
  if (!authToken()) {
    return (
      <>
        <EmptyState title={t('guest.title')} description={t('guest.body')} />
        <div className="row-actions" style={{ justifyContent: 'center' }}>
          <Link className="btn" to="/login?back=%2Faccount">{t('account.login')}</Link>
        </div>
      </>
    )
  }
  const memberText = me?.memberUntil ? t('member.until', { date: me.memberUntil.slice(0, 10) }) : t('member.none')
  return (
    <>
      <section className="account-bar">
        <div>
          <h1>{me?.username || t('account.account')}</h1>
          <p>{memberText}</p>
        </div>
        <div className="row-actions">
          <Link className="btn" to="/membership">{t('member.renew')}</Link>
          <button className="btn ghost" type="button" onClick={() => { setAuthToken(null); location.href = '/' }}>{t('logout')}</button>
        </div>
      </section>
      <div className="chips account-tabs" role="tablist">
        {([
          ['owned', t('owned.title')],
          ['orders', t('orders.title')],
          ['password', t('password.save')],
        ] as const).map(([id, label]) => (
          <button key={id} className={tab === id ? 'chip active' : 'chip'} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}>{label}</button>
        ))}
      </div>
      {tab === 'owned' && (owned.length ? <VideoGrid items={owned} /> : <EmptyState title={t('owned.empty')} />)}
      {tab === 'orders' && <OrderTable orders={orders} />}
      {tab === 'password' && <PasswordForm />}
    </>
  )
}

function PasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const { t } = useI18n()
  async function submit(event: FormEvent) {
    event.preventDefault()
    setMessage('')
    setError('')
    try {
      await api('/api/v1/me/password', { method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }) })
      setCurrentPassword('')
      setNewPassword('')
      setMessage(t('password.ok'))
    } catch (e) {
      setError(e instanceof Error ? e.message : t('password.fail'))
    }
  }
  return (
    <form className="form" onSubmit={submit}>
      <label>{t('password.current')}
        <input value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} type="password" autoComplete="current-password" required />
      </label>
      <label>{t('password.next')}
        <input value={newPassword} onChange={(e) => setNewPassword(e.target.value)} type="password" autoComplete="new-password" required minLength={6} maxLength={72} />
      </label>
      {error && <p className="error">{error}</p>}
      {message && <p className="hint">{message}</p>}
      <button className="btn" type="submit">{t('password.save')}</button>
    </form>
  )
}

export function MembershipPage() {
  const [plans, setPlans] = useState<PlanView[]>([])
  const [channels, setChannels] = useState<PaymentChannel[]>([])
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const back = sameSitePath(params.get('back'))
  const { t } = useI18n()
  useEffect(() => {
    api<PlanView[]>('/api/v1/membership-plans').then(setPlans)
    api<PaymentChannel[]>('/api/v1/payment-channels').then(setChannels).catch(() => setChannels([]))
  }, [])
  async function buy(planId: number, paymentMethod?: string) {
    if (!authToken()) {
      const next = back ? `/membership?back=${encodeURIComponent(back)}` : '/membership'
      navigate(`/login?back=${encodeURIComponent(next)}`)
      return
    }
    try {
      const order = await api<OrderView>('/api/v1/orders', { method: 'POST', body: JSON.stringify({ type: 'MEMBERSHIP', planId, paymentMethod }) })
      navigate(`/orders/${order.id}${back ? `?back=${encodeURIComponent(back)}` : ''}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : t('plans.fail'))
    }
  }
  return (
    <>
      <PageHeader title={t('plans.title')} />
      {error && <p className="error">{error}</p>}
      {plans.length ? (
        <div className="plan-grid">
          {plans.map((plan) => (
            <div className="plan-card" key={plan.id}>
              <strong>{plan.name}</strong>
              <div className="price">{Number(plan.priceUsdt).toFixed(2)}<span>USDT</span></div>
              <p className="sub">{t('plans.days', { days: plan.durationDays })}</p>
              {channels.length > 1 ? (
                <div className="row-actions">
                  {channels.map((channel) => (
                    <button className="btn" key={channel.method} onClick={() => buy(plan.id, channel.method)}>{t(payChannelKey(channel.method))}</button>
                  ))}
                </div>
              ) : channels.length === 1 ? (
                <button className="btn" onClick={() => buy(plan.id, channels[0].method)}>{t('plans.buy')}</button>
              ) : (
                <p className="sub">{t('pay.unavailable')}</p>
              )}
            </div>
          ))}
        </div>
      ) : (
        <EmptyState title={t('plans.empty')} />
      )}
    </>
  )
}

export function OrderPage() {
  const { id = '' } = useParams()
  const [order, setOrder] = useState<OrderView | null>(null)
  const [error, setError] = useState('')
  const [checking, setChecking] = useState(false)
  const [polling, setPolling] = useState(false)
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const back = sameSitePath(params.get('back'))
  const paying = useRef(Boolean(back))
  const { t } = useI18n()
  useEffect(() => {
    let stop = false
    // Refresh / open must hit the chain for pending orders — GET only reads DB and looked "stuck".
    api<OrderView>(`/api/v1/orders/${id}`)
      .then(async (next) => {
        if (stop) return
        if (next.state !== 'PENDING') {
          setOrder(next)
          return
        }
        setOrder(next)
        setPolling(true)
        try {
          const checked = await api<OrderView>(`/api/v1/orders/${id}/check`, { method: 'POST' })
          if (!stop) setOrder(checked)
        } catch {
          /* keep pending; interval below retries */
        } finally {
          if (!stop) setPolling(false)
        }
      })
      .catch((e) => { if (!stop) setError(e.message) })
    return () => { stop = true }
  }, [id])
  useEffect(() => {
    if (order?.state !== 'PENDING') return
    let stop = false
    let running = false
    const poll = () => {
      if (stop || running) return
      running = true
      setPolling(true)
      api<OrderView>(`/api/v1/orders/${id}/check`, { method: 'POST' })
        .then((next) => { if (!stop) setOrder(next) })
        .catch(() => undefined)
        .finally(() => {
          running = false
          if (!stop) setPolling(false)
        })
    }
    const first = window.setTimeout(poll, 2000)
    const timer = window.setInterval(poll, 5000)
    return () => {
      stop = true
      window.clearTimeout(first)
      window.clearInterval(timer)
    }
  }, [id, order?.state])
  useEffect(() => {
    if (order?.state === 'PENDING') paying.current = true
    if (order?.state !== 'PAID' || !paying.current) return
    navigate(back || (order.videoId ? `/videos/${order.videoId}` : '/account'), { replace: true })
  }, [order?.state, order?.videoId, back, navigate])
  async function check() {
    setChecking(true)
    setError('')
    try {
      setOrder(await api<OrderView>(`/api/v1/orders/${id}/check`, { method: 'POST' }))
    } catch (e) {
      setError(e instanceof Error ? e.message : t('order.checkFail'))
    } finally {
      setChecking(false)
    }
  }
  async function cancel() {
    setError('')
    try {
      setOrder(await api<OrderView>(`/api/v1/orders/${id}/cancel`, { method: 'POST' }))
    } catch (e) {
      setError(e instanceof Error ? e.message : t('order.cancelFail'))
    }
  }
  async function copyAddress() {
    if (!order?.address) return
    await navigator.clipboard.writeText(order.address)
  }
  async function copyAmount() {
    if (!order) return
    await navigator.clipboard.writeText(Number(order.payAmount).toFixed(6))
  }
  if (!order) return <p className="empty">{error || t('loading')}</p>
  const tone = order.state === 'PAID' ? 'ok' : order.state === 'EXPIRED' ? 'bad' : 'warn'
  const stateLabel = order.state === 'PAID' ? t('state.paid') : order.state === 'EXPIRED' ? t('state.closed') : t('state.pending')
  return (
    <Panel>
      <PageHeader title={t('order.title', { id: order.id })} description={order.orderType === 'MEMBERSHIP' ? t('order.member') : t('order.episode')} />
      <p><Badge tone={tone}>{stateLabel}</Badge></p>
      <p className="pay-amount">{t('order.amount')}<strong>{formatPay(order.payAmount)}</strong></p>
      <p>{t(payAddressKey(order.paymentMethod))}{order.address || t('order.noAddress')}</p>
      {order.state === 'PENDING' && order.address && <PayQr value={order.address} label={t('order.qrAlt')} />}
      {order.state === 'PENDING' && order.address && <p className="sub">{t('order.qr')}</p>}
      <p className="sub">{t('order.network', { network: t(payNetworkKey(order.network)) })}</p>
      <p className="sub">{t('order.transfer', { time: order.expiresAt?.replace('T', ' ').slice(0, 19) || '' })}</p>
      {order.state === 'PENDING' && (
        <div className={`pay-notice${polling || checking ? ' busy' : ''}`}>
          <p>{t('order.waitNotice')}</p>
          <p>{polling || checking ? t('order.checking') : t('order.checkHint')}</p>
          {(back || order.videoId) ? <p>{t('order.back')}</p> : null}
        </div>
      )}
      {error && <p className="error">{error}</p>}
      <div className="row-actions">
        {order.state === 'PENDING' && <button className="btn" onClick={check} disabled={checking}>{t('order.check')}</button>}
        {order.state === 'PENDING' && <button className="btn ghost" onClick={copyAmount}>{t('order.copyAmount')}</button>}
        {order.state === 'PENDING' && order.address && <button className="btn ghost" onClick={copyAddress}>{t('order.copy')}</button>}
        {order.state === 'PENDING' && <button className="btn ghost" onClick={cancel}>{t('order.cancel')}</button>}
        {order.state === 'PAID' && order.videoId && <Link className="btn" to={`/videos/${order.videoId}`}>{t('order.play')}</Link>}
        {order.state === 'PAID' && !order.videoId && <Link className="btn" to="/account">{t('order.account')}</Link>}
      </div>
    </Panel>
  )
}

function PayQr({ value, label }: { value: string; label: string }) {
  const [src, setSrc] = useState('')
  useEffect(() => {
    let stop = false
    QRCode.toDataURL(value, { margin: 4, width: 900, errorCorrectionLevel: 'M', color: { dark: '#000000', light: '#ffffff' } })
      .then((url) => { if (!stop) setSrc(url) })
      .catch(() => { if (!stop) setSrc('') })
    return () => { stop = true }
  }, [value])
  if (!src) return null
  return <img className="pay-qr" src={src} alt={label} width={300} height={300} />
}

export function FavoritesPage() {
  const [items, setItems] = useState<VideoCard[] | null>(null)
  const [error, setError] = useState('')
  const [page, setPage] = useState(1)
  const signedIn = Boolean(authToken())
  const { t } = useI18n()
  useEffect(() => {
    setError('')
    setItems(null)
    if (signedIn) {
      api<VideoCard[]>('/api/v1/me/favorites').then(setItems).catch((e) => {
        setItems([])
        setError(e instanceof Error ? e.message : t('load.fail'))
      })
      return
    }
    const ids = favorites()
    if (!ids.length) {
      setItems([])
      return
    }
    api<VideoCard[]>('/api/v1/videos/by-ids', { method: 'POST', body: JSON.stringify({ ids }) }).then(setItems).catch((e) => {
      setItems([])
      setError(e instanceof Error ? e.message : t('load.fail'))
    })
  }, [signedIn, t])
  const pageSize = 12
  const shown = items ? items.slice((page - 1) * pageSize, page * pageSize) : null
  return (
    <>
      <PageHeader title={t('favorites.title')} />
      {error ? <p className="error">{error}</p> : null}
      {shown === null ? <p className="empty">{t('loading')}</p> : <VideoGrid items={shown} />}
      {items ? <Pager page={page} total={items.length} pageSize={pageSize} onPage={setPage} /> : null}
    </>
  )
}

export function InstallPage() {
  const [ready, setReady] = useState(Boolean(pendingInstall()))
  const [done, setDone] = useState(false)
  useEffect(() => {
    const onReady = () => setReady(true)
    window.addEventListener('xtv-installable', onReady)
    return () => window.removeEventListener('xtv-installable', onReady)
  }, [])
  const { t } = useI18n()
  async function install() {
    const prompt = pendingInstall()
    if (!prompt) return
    await prompt.prompt()
    setDone(true)
  }
  return (
    <>
      <PageHeader title={t('install.title')} description={t('install.desc')} />
      <Panel>
        {done ? <p>{t('install.prompt')}</p> : null}
        {ready && !done ? <button className="btn" onClick={install}>{t('install.button')}</button> : null}
        {!ready && !done ? <p>{t('install.mobile')}</p> : null}
      </Panel>
    </>
  )
}

function useLegal(code: 'privacy' | 'terms', fallback: string) {
  const { locale } = useI18n()
  const [body, setBody] = useState(fallback)
  useEffect(() => {
    api<{ body: string }>(`/api/v1/legal/${code}`).then((page) => setBody(page.body || fallback)).catch(() => setBody(fallback))
  }, [code, fallback, locale])
  return body
}

export function PrivacyPage() {
  const { t } = useI18n()
  const body = useLegal('privacy', t('privacy.body'))
  return (
    <>
      <PageHeader title={t('privacy.title')} />
      <Panel>
        <div className="legal-body">{body}</div>
      </Panel>
    </>
  )
}

export function TermsPage() {
  const { t } = useI18n()
  const body = useLegal('terms', t('terms.body'))
  return (
    <>
      <PageHeader title={t('terms.title')} />
      <Panel>
        <div className="legal-body">{body}</div>
      </Panel>
    </>
  )
}

export function ReportPage() {
  const { t } = useI18n()
  const [params] = useSearchParams()
  const [contact, setContact] = useState('')
  const [message, setMessage] = useState('')
  const [videoId, setVideoId] = useState(params.get('video') || '')
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    try {
      await api('/api/v1/reports', { method: 'POST', body: JSON.stringify({ contact, message, videoId: videoId ? Number(videoId) : null }) })
      setDone(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : t('report.fail'))
    }
  }
  if (done) return <EmptyState title={t('report.ok')} description={t('report.okBody')} />
  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <form className="form" onSubmit={submit}>
          <h1>{t('report.title')}</h1>
          {error ? <p className="error">{error}</p> : null}
          <label>{t('report.contact')}
            <input value={contact} onChange={(e) => setContact(e.target.value)} placeholder={t('report.contactPh')} required />
          </label>
          <label>{t('report.video')}
            <input value={videoId} onChange={(e) => setVideoId(e.target.value)} placeholder={t('report.videoPh')} />
          </label>
          <label>{t('report.note')}
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} placeholder={t('report.notePh')} required />
          </label>
          <button className="btn" type="submit">{t('report.send')}</button>
        </form>
      </div>
    </div>
  )
}

function OrderTable({ orders }: { orders: OrderView[] }) {
  const { t } = useI18n()
  const listed = [...orders].sort((a, b) => {
    const rank = (state: string) => state === 'PENDING' ? 0 : 1
    const byState = rank(a.state) - rank(b.state)
    if (byState) return byState
    return (b.createdAt || '').localeCompare(a.createdAt || '')
  })
  if (!listed.length) return <EmptyState title={t('orders.empty')} />
  return (
    <div className="table-wrap">
      <table className="table account-orders">
        <thead>
          <tr>
            <th>{t('orders.id')}</th>
            <th>{t('orders.type')}</th>
            <th>{t('orders.state')}</th>
            <th>{t('orders.amount')}</th>
            <th>{t('orders.time')}</th>
          </tr>
        </thead>
        <tbody>
          {listed.map((order) => (
            <tr key={order.id}>
              <td><Link to={`/orders/${order.id}`}>#{order.id}</Link></td>
              <td>{order.orderType === 'MEMBERSHIP' ? t('type.member') : order.orderType === 'PPV' ? t('type.episode') : order.orderType}</td>
              <td><Badge tone={order.state === 'PAID' ? 'ok' : order.state === 'EXPIRED' ? 'bad' : 'warn'}>{order.state === 'PAID' ? t('state.paid') : order.state === 'EXPIRED' ? t('state.closed') : t('state.pending')}</Badge></td>
              <td className="pay-amount">{formatPay(order.payAmount)}</td>
              <td>{order.createdAt ? order.createdAt.replace('T', ' ').slice(0, 16) : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
