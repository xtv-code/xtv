import { type FormEvent, useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { type AdView, type NavItem, api, authToken } from './api'
import { LanguageSwitch, useI18n } from './i18n'

const NAV_MAIN = [
  { to: '/', key: 'nav.home', end: true },
  { to: '/latest', key: 'nav.latest' },
  { to: '/ranking', key: 'nav.ranking' },
  { to: '/series', key: 'nav.series' },
] as const

const NAV_INDEX = [
  { to: '/people', key: 'nav.people' },
  { to: '/tags', key: 'nav.tags' },
  { to: '/favorites', key: 'nav.favorites' },
] as const

type SiteProfile = { siteName: string }

const DEFAULT_SITE: SiteProfile = { siteName: 'XTV' }

export function Layout() {
  const [nav, setNav] = useState<{ categories: NavItem[]; tags: NavItem[] }>({ categories: [], tags: [] })
  const [site, setSite] = useState<SiteProfile>(DEFAULT_SITE)
  const [top, setTop] = useState<AdView | null>(null)
  const [popup, setPopup] = useState<AdView | null>(null)
  const [ageOk, setAgeOk] = useState(() => localStorage.getItem('xtv.age') === '1')
  const [q, setQ] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const loggedIn = Boolean(authToken())
  const { t, locale } = useI18n()

  useEffect(() => {
    api<{ categories: NavItem[]; tags: NavItem[] }>('/api/v1/nav').then(setNav).catch(() => undefined)
  }, [locale])

  useEffect(() => {
    api<AdView | null>('/api/v1/ads?slot=TOP').then(setTop).catch(() => undefined)
    const loadSite = () => api<SiteProfile>('/api/v1/site').then(setSite).catch(() => undefined)
    loadSite()
    window.addEventListener('xtv-site', loadSite)
    return () => window.removeEventListener('xtv-site', loadSite)
  }, [])

  useEffect(() => {
    document.title = site.siteName || 'XTV'
  }, [site.siteName])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (location.pathname === '/search') setQ(new URLSearchParams(location.search).get('q') || '')
  }, [location.pathname, location.search])

  const loginTo = location.pathname === '/login' || location.pathname === '/register'
    ? '/login'
    : `/login?back=${encodeURIComponent(`${location.pathname}${location.search}`)}`

  useEffect(() => {
    const quiet = location.pathname.startsWith('/orders') || location.pathname.startsWith('/account') || location.pathname.startsWith('/membership')
    if (!ageOk || quiet || sessionStorage.getItem('xtv.popup') === '1') return
    api<AdView | null>('/api/v1/ads?slot=POPUP').then((ad) => {
      if (ad) setPopup(ad)
      sessionStorage.setItem('xtv.popup', '1')
    }).catch(() => undefined)
  }, [ageOk, location.pathname])

  function search(event: FormEvent) {
    event.preventDefault()
    navigate(`/search?q=${encodeURIComponent(q.trim())}`)
    setMenuOpen(false)
  }

  if (!ageOk) {
    return (
      <div className="shell">
        <div className="mask" />
        <div className="gate">
          <LanguageSwitch />
          <h1>{t('age.title')}</h1>
          <p>{t('age.body')}</p>
          <div className="row-actions">
            <button className="btn" onClick={() => { localStorage.setItem('xtv.age', '1'); setAgeOk(true) }}>{t('age.yes')}</button>
            <a className="btn ghost" href="https://www.google.com">{t('age.leave')}</a>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="shell">
      <header className="site-header">
        <div className="topbar">
          <Link className="brand" to="/"><Brand name={site.siteName} /></Link>
          <nav className="nav" aria-label={t('nav.aria')}>
            {NAV_MAIN.map((item) => (
              <NavLink key={item.to} to={item.to} end={'end' in item ? item.end : false}>{t(item.key)}</NavLink>
            ))}
            {nav.categories.map((item) => (
              <NavLink key={`c-${item.slug}`} to={`/categories/${item.slug}`}>{item.name}</NavLink>
            ))}
            {NAV_INDEX.map((item) => (
              <NavLink key={item.to} to={item.to}>{t(item.key)}</NavLink>
            ))}
          </nav>
          <div className="header-actions">
            <form className="search" onSubmit={search}>
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('search.placeholder')} aria-label={t('search.button')} />
              <button className="btn sm" type="submit">{t('search.button')}</button>
            </form>
            <LanguageSwitch />
            {loggedIn ? <NavLink className="btn ghost sm account-link" to="/account">{t('account.account')}</NavLink> : <NavLink className="btn sm account-link" to={loginTo}>{t('account.login')}</NavLink>}
            <button className="icon-btn menu-toggle" type="button" aria-label={t('menu.open')} onClick={() => setMenuOpen(true)}>
              <MenuIcon />
            </button>
          </div>
        </div>
      </header>

      {top && (
        <a className="banner" href={top.targetUrl} target="_blank" rel="noreferrer">
          <img src={top.imageUrl} alt="" />
        </a>
      )}

      <main className="page"><Outlet key={location.pathname.startsWith('/admin') ? 'admin' : locale} /></main>

      <footer className="footer">
        <div className="footer-col">
          <strong>{t('footer.browse')}</strong>
          <Link to="/latest">{t('nav.latest')}</Link>
          <Link to="/ranking">{t('nav.ranking')}</Link>
          <Link to="/series">{t('nav.series')}</Link>
          <Link to="/people">{t('nav.people')}</Link>
          <Link to="/tags">{t('nav.tags')}</Link>
          <Link to="/categories">{t('nav.categories')}</Link>
        </div>
        <div className="footer-col">
          <strong>{t('footer.service')}</strong>
          <Link to="/favorites">{t('nav.favorites')}</Link>
          <Link to="/install">{t('footer.install')}</Link>
          <Link to="/membership">{t('footer.membership')}</Link>
        </div>
        <div className="footer-col">
          <strong>{t('footer.legal')}</strong>
          <Link to="/privacy">{t('footer.privacy')}</Link>
          <Link to="/terms">{t('footer.terms')}</Link>
          <Link to="/report">{t('footer.report')}</Link>
        </div>
      </footer>

      {menuOpen && (
        <>
          <div className="drawer-mask" onClick={() => setMenuOpen(false)} />
          <aside className="drawer open" aria-label={t('nav.drawer')}>
            <form className="search" onSubmit={search} style={{ width: '100%', marginBottom: 12 }}>
              <input value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('search.placeholder')} aria-label={t('search.button')} style={{ flex: 1, width: '100%' }} />
              <button className="btn sm" type="submit">{t('search.button')}</button>
            </form>
            <div className="drawer-title">{t('nav.section')}</div>
            {NAV_MAIN.map((item) => (
              <NavLink key={item.to} to={item.to} end={'end' in item ? item.end : false}>{t(item.key)}</NavLink>
            ))}
            {nav.categories.map((item) => (
              <NavLink key={item.slug} to={`/categories/${item.slug}`}>{item.name}</NavLink>
            ))}
            {NAV_INDEX.map((item) => (
              <NavLink key={item.to} to={item.to}>{t(item.key)}</NavLink>
            ))}
            <div className="drawer-title">{t('nav.account')}</div>
            {loggedIn ? <NavLink to="/account">{t('account.account')}</NavLink> : <NavLink to={loginTo}>{t('account.login')}</NavLink>}
            <NavLink to="/membership">{t('footer.membership')}</NavLink>
            <NavLink to="/install">{t('footer.install')}</NavLink>
            <LanguageSwitch />
            <button className="btn ghost" type="button" onClick={() => setMenuOpen(false)}>{t('menu.close')}</button>
          </aside>
        </>
      )}

      {popup && (
        <>
          <div className="mask" onClick={() => setPopup(null)} />
          <div className="modal">
            <a href={popup.targetUrl} target="_blank" rel="noreferrer">
              <img src={popup.imageUrl} alt="" style={{ width: '100%', borderRadius: 8 }} />
            </a>
            <div className="row-actions">
              <button className="btn ghost" onClick={() => setPopup(null)}>{t('menu.close')}</button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function Brand({ name }: { name: string }) {
  return <>{name.trim() || 'XTV'}</>
}

function MenuIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden>
      <path d="M3 5h12M3 9h12M3 13h12" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}
