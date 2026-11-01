import { contentLocale } from './i18n'

export type AccessType = 'FREE' | 'MEMBER' | 'PPV'

export type ContentNames = { zh?: string; en?: string; ja?: string; ko?: string }

export interface VideoCard {
  id: number
  title: string
  coverUrl: string | null
  durationMs: number | null
  viewCount: number
  publishedAt: string | null
  accessType: AccessType
  priceUsdt: number | null
  episodeNo: number | null
  seriesTitle: string | null
  seriesStatus: string | null
  seriesSlug: string | null
  status: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface NamedRef {
  id: number
  name: string
  slug: string
}

export interface PersonRef extends NamedRef {
  avatarUrl: string | null
  kind: string
  names?: ContentNames
}

export interface VideoDetail extends VideoCard {
  entitled: boolean
  featured: boolean
  seriesId: number | null
  categories: NamedRef[]
  tags: NamedRef[]
  people: PersonRef[]
  sourceUrl?: string
  names?: ContentNames
}

export interface PlanView {
  id: number
  name: string
  durationDays: number
  priceUsdt: number
  enabled: boolean
  names?: ContentNames
}

export interface PlayResult {
  allowed: boolean
  playUrl: string | null
  accessType: AccessType
  priceUsdt: number | null
  plans: PlanView[]
}

export interface NavItem {
  name: string
  slug: string
}

export interface AdView {
  id: number
  slot: string
  imageUrl: string
  targetUrl: string
  enabled: boolean
  startAt: string | null
  endAt: string | null
  sortOrder: number
}

export interface PaymentChannel {
  method: string
}

export interface OrderView {
  id: number
  orderType: string
  planId: number | null
  videoId: number | null
  listPrice: number
  payAmount: number
  paymentMethod: string
  state: string
  address: string
  paymentSimulate: boolean
  expiresAt: string
  createdAt: string
  userId?: number
  payerAddress?: string
  txHash?: string
  network?: string
  checkoutUrl?: string
}

function isReturnPath(path: string) {
  if (path === '/' || path === '/account' || path === '/membership' || path === '/favorites' || path === '/latest' || path === '/ranking' || path === '/search' || path === '/categories' || path === '/tags' || path === '/people' || path === '/series' || path === '/install' || path === '/privacy' || path === '/terms' || path === '/report') return true
  return path.startsWith('/videos/') || path.startsWith('/series/') || path.startsWith('/categories/') || path.startsWith('/tags/') || path.startsWith('/people/') || path.startsWith('/orders/')
}

export function sameSitePath(value: string | null | undefined) {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.includes('\\') || value.includes('://')) return ''
  const [path, query = ''] = value.split('#')[0].split('?')
  if (!isReturnPath(path)) return ''
  if (!query) return path
  const params = new URLSearchParams(query)
  if ([...params.keys()].some((key) => key !== 'back' && key !== 'q' && key !== 'video')) return path
  const nested = params.get('back')
  if (!nested) {
    if (path === '/search' && params.has('q')) return `${path}?q=${encodeURIComponent(params.get('q') || '')}`
    if (path === '/report' && params.has('video')) return `${path}?video=${encodeURIComponent(params.get('video') || '')}`
    return path
  }
  if (!nested.startsWith('/') || nested.startsWith('//') || nested.includes('?') || nested.includes('#')) return path
  return isReturnPath(nested) ? `${path}?back=${encodeURIComponent(nested)}` : path
}

export function payChannelKey(method: string) {
  switch (method) {
    case 'USDT_BEP20': return 'pay.bsc'
    case 'USDT_ERC20': return 'pay.eth'
    case 'USDT_POLYGON': return 'pay.polygon'
    case 'USDT_ARBITRUM': return 'pay.arbitrum'
    case 'USDT_AVALANCHE': return 'pay.avax'
    case 'USDT_SOLANA': return 'pay.solana'
    case 'USDT_TON': return 'pay.ton'
    default: return 'pay.tron'
  }
}

export function payAddressKey(method: string) {
  switch (method) {
    case 'USDT_BEP20': return 'order.address.bep20'
    case 'USDT_ERC20': return 'order.address.erc20'
    case 'USDT_POLYGON': return 'order.address.polygon'
    case 'USDT_ARBITRUM': return 'order.address.arbitrum'
    case 'USDT_AVALANCHE': return 'order.address.avax'
    case 'USDT_SOLANA': return 'order.address.solana'
    case 'USDT_TON': return 'order.address.ton'
    default: return 'order.address'
  }
}

export function payNetworkKey(network?: string) {
  switch (network) {
    case 'NILE': return 'order.net.nile'
    case 'BSC': return 'order.net.bsc'
    case 'BSC_TEST': return 'order.net.bscTest'
    case 'ETH': return 'order.net.eth'
    case 'ETH_TEST': return 'order.net.ethTest'
    case 'POLYGON': return 'order.net.polygon'
    case 'ARBITRUM': return 'order.net.arbitrum'
    case 'AVALANCHE': return 'order.net.avax'
    case 'SOLANA': return 'order.net.solana'
    case 'TON': return 'order.net.ton'
    default: return 'order.net.mainnet'
  }
}

export interface Me {
  id: number
  username: string
  role: string
  memberUntil: string | null
  ownedVideoIds: number[]
}

export interface AuthResponse extends Me {
  token: string
}

export interface TagCard {
  id: number
  name: string
  slug: string
  count: number
}

export interface TaxonomyView {
  id: number
  name: string
  slug: string
  sortOrder: number
  showInNav: boolean
  showOnHome: boolean
  names?: ContentNames
}

export interface CategoryRow {
  name: string
  slug: string
  videos: VideoCard[]
}

export interface PersonCard {
  id: number
  name: string
  slug: string
  avatarUrl: string | null
  kind: string
  works: number
}

export interface SeriesCard {
  id: number
  title: string
  slug: string
  coverUrl: string | null
  status: string
  episodes: number
  sourceUrl?: string
  names?: ContentNames
}

export interface JobView {
  id: number
  videoId: number
  state: string
  progress: number
  errorMessage: string | null
  retryCount: number
  updatedAt: string
}

const TOKEN = 'xtv.token'

export function authToken() {
  return localStorage.getItem(TOKEN)
}

export function setAuthToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN, token)
  else localStorage.removeItem(TOKEN)
}

export function favorites(): number[] {
  try {
    const raw = JSON.parse(localStorage.getItem('xtv.favs') || '[]')
    return Array.isArray(raw) ? raw.map(Number).filter(Boolean) : []
  } catch {
    return []
  }
}

export function toggleFavorite(id: number) {
  const current = favorites()
  const next = current.includes(id) ? current.filter((item) => item !== id) : [id, ...current]
  localStorage.setItem('xtv.favs', JSON.stringify(next))
  return next.includes(id)
}

export async function adoptLocalFavorites() {
  const ids = favorites()
  if (!ids.length || !authToken()) return
  await api('/api/v1/me/favorites/sync', { method: 'POST', body: JSON.stringify({ ids }) })
  localStorage.removeItem('xtv.favs')
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = authToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (!headers.has('Accept-Language')) headers.set('Accept-Language', contentLocale())
  const response = await fetch(path, { ...init, headers })
  if (response.status === 204) return undefined as T
  const text = await response.text()
  let body: { message?: string } | null = null
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      throw new Error(text.slice(0, 180))
    }
  }
  if (!response.ok) throw new Error(body?.message || '请求失败')
  return body as T
}

export function money(value: number | null | undefined) {
  if (value == null) return ''
  return `${Number(value).toFixed(2)} USDT`
}

export function formatPay(value: number | string | null | undefined) {
  if (value == null || value === '') return ''
  const raw = typeof value === 'number' ? value.toFixed(8) : String(value)
  const negative = raw.startsWith('-')
  const [whole, frac = ''] = raw.replace('-', '').split('.')
  return `${negative ? '-' : ''}${whole}.${(frac + '000000').slice(0, 6)} USDT`
}

export function duration(ms: number | null) {
  if (!ms) return ''
  const total = Math.round(ms / 1000)
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

export function accessLabel(card: Pick<VideoCard, 'accessType' | 'priceUsdt'>) {
  if (card.accessType === 'MEMBER') return '会员'
  if (card.accessType === 'PPV') return money(card.priceUsdt)
  return '免费'
}
