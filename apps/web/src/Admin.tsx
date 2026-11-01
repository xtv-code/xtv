import { type FormEvent, type ReactNode, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { type AdView, type JobView, type NamedRef, type OrderView, type PageResult, type PersonRef, type PlanView, type SeriesCard, type TaxonomyView, type VideoCard, type VideoDetail, api, authToken, setAuthToken } from './api'
import { FilterInput, FilterSelect, SortTh, queryString, useAdminQuery } from './adminList'
import { Pager } from './Cards'
import { Badge, LocaleFields, contentNames, primaryName } from './ui'

type Tab = 'videos' | 'categories' | 'tags' | 'people' | 'series' | 'plans' | 'payments' | 'ads' | 'orders' | 'reports' | 'users' | 'settings' | 'legal'

const NAV: { label: string; items: Tab[] }[] = [
  { label: '内容', items: ['videos', 'categories', 'tags', 'people', 'series'] },
  { label: '交易', items: ['plans', 'payments', 'orders'] },
  { label: '运营', items: ['ads', 'reports'] },
  { label: '系统', items: ['users', 'settings', 'legal'] },
]

export function AdminPage() {
  const [tab, setTab] = useState<Tab>('videos')
  const [authed, setAuthed] = useState(Boolean(authToken()))
  const [error, setError] = useState('')
  if (!authed) return <AdminLogin onOk={() => setAuthed(true)} />
  return (
    <div className="admin">
      <aside className="admin-side">
        {NAV.map((group) => (
          <div key={group.label}>
            <div className="side-label">{group.label}</div>
            {group.items.map((item) => (
              <a key={item} href="#" className={tab === item ? 'active' : undefined} onClick={(e) => { e.preventDefault(); setError(''); setTab(item) }}>{label(item)}</a>
            ))}
          </div>
        ))}
        <button className="side-link" type="button" onClick={() => { setAuthToken(null); setAuthed(false) }}>退出登录</button>
      </aside>
      <div className="admin-main">
        {error && <p className="error">{error}</p>}
        {tab === 'videos' && <Videos onError={setError} />}
        {tab === 'categories' && <Taxonomy kind="categories" onError={setError} />}
        {tab === 'tags' && <Taxonomy kind="tags" onError={setError} />}
        {tab === 'people' && <PeopleAdmin onError={setError} />}
        {tab === 'series' && <SeriesAdmin onError={setError} />}
        {tab === 'plans' && <PlansAdmin onError={setError} />}
        {tab === 'payments' && <PaymentsAdmin onError={setError} />}
        {tab === 'ads' && <AdsAdmin onError={setError} />}
        {tab === 'orders' && <OrdersAdmin onError={setError} />}
        {tab === 'reports' && <ReportsAdmin onError={setError} />}
        {tab === 'users' && <UsersAdmin onError={setError} />}
        {tab === 'settings' && <SettingsAdmin onError={setError} />}
        {tab === 'legal' && <LegalAdmin onError={setError} />}
      </div>
    </div>
  )
}

function fail(onError: (message: string) => void, error: unknown, fallback = '操作失败') {
  onError(error instanceof Error ? error.message : fallback)
}

function PageBar({ title, onCreate }: { title: string; onCreate: () => void }) {
  return (
    <div className="page-bar">
      <h1>{title}</h1>
      <button className="btn" type="button" onClick={onCreate}>新建</button>
    </div>
  )
}

function Editor({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <>
      <div className="mask" onClick={onClose} />
      <div className="modal editor" role="dialog" aria-modal="true" aria-label={title}>
        <div className="editor-head">
          <h2>{title}</h2>
          <button className="btn ghost sm" type="button" onClick={onClose}>关闭</button>
        </div>
        {children}
      </div>
    </>
  )
}

function OpsHead() {
  return <th className="ops">操作</th>
}

function Ops({ children }: { children: ReactNode }) {
  return <td className="ops"><div className="row-actions">{children}</div></td>
}

function EmptyRow({ cols, text }: { cols: number; text: string }) {
  return <tr><td className="empty-cell" colSpan={cols}>{text}</td></tr>
}

const VIDEO_STATUS: Record<string, string> = {
  DRAFT: '草稿', UPLOADED: '已上传', TRANSCODING: '转码中', READY: '可发布', FAILED: '失败', PUBLISHED: '已发布',
}
const ACCESS_LABEL: Record<string, string> = { FREE: '免费', MEMBER: '会员', PPV: '单集付费' }
const ORDER_TYPE: Record<string, string> = { MEMBERSHIP: '会员', PPV: '单集' }
const PAY_CHANNEL: Record<string, string> = {
  USDT_TRC20: '波场', USDT_ERC20: '以太坊', USDT_BEP20: '币安链', USDT_POLYGON: 'Polygon',
  USDT_ARBITRUM: 'Arbitrum', USDT_AVALANCHE: 'Avalanche', USDT_SOLANA: 'Solana', USDT_TON: 'TON',
}
const ORDER_STATE: Record<string, string> = { PENDING: '待支付', PAID: '已支付', EXPIRED: '已关闭' }

function label(tab: Tab) {
  const names: Record<Tab, string> = {
    videos: '影片管理', categories: '分类管理', tags: '标签管理', people: '人物管理', series: '短剧管理',
    plans: '会员方案', payments: '收款方式', ads: '广告管理', orders: '订单管理', reports: '投诉处理', users: '用户管理', settings: '平台配置', legal: '法律协议',
  }
  return names[tab]
}

function AdminLogin({ onOk }: { onOk: () => void }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState('')
  async function submit(event: FormEvent) {
    event.preventDefault()
    try {
      const result = await api<{ token: string; role: string }>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
      if (result.role !== 'ADMIN') throw new Error('需要管理员账号')
      setAuthToken(result.token)
      onOk()
    } catch (e) {
      setError(e instanceof Error ? e.message : '登录失败')
    }
  }
  return (
    <div className="admin-login-wrap">
      <div className="auth-card">
        <form className="form" onSubmit={submit}>
          <h1>管理后台</h1>
          <label>用户名
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名" autoComplete="username" />
          </label>
          <label>密码
            <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="密码" autoComplete="current-password" />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="btn" type="submit">登录</button>
        </form>
      </div>
    </div>
  )
}

function jobTone(state: string): 'ok' | 'warn' | 'bad' | 'info' | 'default' {
  if (state === 'SUCCEEDED') return 'ok'
  if (state === 'FAILED') return 'bad'
  if (state === 'RUNNING' || state === 'PENDING') return 'warn'
  return 'info'
}

function Videos({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<VideoCard[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [selected, setSelected] = useState<number | null>(null)
  const [jobs, setJobs] = useState<JobView[]>([])
  const [categories, setCategories] = useState<TaxonomyView[]>([])
  const [tags, setTags] = useState<TaxonomyView[]>([])
  const [people, setPeople] = useState<PersonRef[]>([])
  const [series, setSeries] = useState<SeriesCard[]>([])
  const [currentCover, setCurrentCover] = useState('')
  const [coverFile, setCoverFile] = useState<File | null>(null)
  const [coverPreview, setCoverPreview] = useState('')
  const [sourceFile, setSourceFile] = useState<File | null>(null)
  const [editing, setEditing] = useState(false)
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(true)
  const emptyForm = { titles: contentNames(), sourceUrl: '', coverUrl: '', accessType: 'FREE', priceUsdt: '', featured: false, categoryIds: [] as number[], tagIds: [] as number[], personIds: [] as number[], seriesId: '', episodeNo: '' }
  const [form, setForm] = useState(emptyForm)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })

  async function load(nextPage = page) {
    const [videos, cats, tagList, personList, seriesList] = await Promise.all([
      api<PageResult<VideoCard>>(`/api/v1/admin/videos${q.query({ page: nextPage, size: 12 })}`),
      api<PageResult<TaxonomyView>>('/api/v1/admin/categories?page=1&size=200'),
      api<PageResult<TaxonomyView>>('/api/v1/admin/tags?page=1&size=200'),
      api<PageResult<PersonRef>>('/api/v1/admin/people?page=1&size=200'),
      api<PageResult<SeriesCard>>('/api/v1/admin/series?page=1&size=200'),
    ])
    setItems(videos.items)
    setPage(videos.page)
    setTotal(videos.total)
    setPageSize(videos.pageSize)
    setCategories(cats.items)
    setTags(tagList.items)
    setPeople(personList.items)
    setSeries(seriesList.items)
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    Promise.all([
      api<PageResult<VideoCard>>(`/api/v1/admin/videos${q.query({ page: 1, size: 12 })}`),
      api<PageResult<TaxonomyView>>('/api/v1/admin/categories?page=1&size=200'),
      api<PageResult<TaxonomyView>>('/api/v1/admin/tags?page=1&size=200'),
      api<PageResult<PersonRef>>('/api/v1/admin/people?page=1&size=200'),
      api<PageResult<SeriesCard>>('/api/v1/admin/series?page=1&size=200'),
    ]).then(([videos, cats, tagList, personList, seriesList]) => {
      if (!alive) return
      setItems(videos.items)
      setPage(videos.page)
      setTotal(videos.total)
      setPageSize(videos.pageSize)
      setCategories(cats.items)
      setTags(tagList.items)
      setPeople(personList.items)
      setSeries(seriesList.items)
    }).catch((e) => {
      if (alive) onError(e instanceof Error ? e.message : '加载失败')
    }).finally(() => {
      if (alive) setLoading(false)
    })
    return () => { alive = false }
  }, [q.requestKey])

  async function upload(id: number, file: File) {
    const ticket = await api<{ mode: string; uploadUrl: string | null }>(`/api/v1/admin/videos/${id}/upload-credential`, { method: 'POST' })
    if (ticket.mode === 'OBJECT' && ticket.uploadUrl) {
      const put = await fetch(ticket.uploadUrl, { method: 'PUT', body: new Blob([file]) })
      if (!put.ok) {
        throw new Error('对象存储拒绝了直传，请检查桶的跨域是否允许本站 PUT')
      }
      await api(`/api/v1/admin/videos/${id}/complete-upload?size=${file.size}`, { method: 'POST' })
    } else {
      const body = new FormData()
      body.append('file', file)
      await api(`/api/v1/admin/videos/${id}/source`, { method: 'POST', body })
    }
    setJobs(await api<JobView[]>(`/api/v1/admin/videos/${id}/jobs`))
    setSourceFile(null)
  }

  async function openVideo(id: number) {
    setSelected(id)
    const detail = await api<VideoDetail>(`/api/v1/videos/${id}`)
    setCurrentCover(detail.coverUrl || '')
    setSourceFile(null)
    setCoverFile(null)
    setCoverPreview('')
    setForm({
      titles: contentNames(detail.names, detail.title),
      sourceUrl: detail.sourceUrl || '',
      coverUrl: '',
      accessType: detail.accessType,
      priceUsdt: detail.priceUsdt == null ? '' : String(detail.priceUsdt),
      featured: detail.featured,
      categoryIds: detail.categories.map((item) => item.id),
      tagIds: detail.tags.map((item) => item.id),
      personIds: detail.people.map((item) => item.id),
      seriesId: detail.seriesId ? String(detail.seriesId) : '',
      episodeNo: detail.episodeNo == null ? '' : String(detail.episodeNo),
    })
    setJobs(await api<JobView[]>(`/api/v1/admin/videos/${id}/jobs`))
    setFormError('')
    setEditing(true)
  }

  function startNew() {
    setSelected(null)
    setJobs([])
    setCurrentCover('')
    setSourceFile(null)
    setCoverFile(null)
    setCoverPreview('')
    setForm(emptyForm)
    setFormError('')
    setEditing(true)
  }

  async function saveMeta(event: FormEvent) {
    event.preventDefault()
    const ppv = form.accessType === 'PPV'
    const inSeries = Boolean(form.seriesId)
    if (ppv && !(Number(form.priceUsdt) > 0)) {
      setFormError('单集付费需要填写大于 0 的价格')
      return
    }
    if (inSeries && !(Number(form.episodeNo) >= 1)) {
      setFormError('选择短剧后需要填写集数')
      return
    }
    const title = primaryName(form.titles)
    if (!title) {
      setFormError('至少填写一种语言的标题')
      return
    }
    try {
      setFormError('')
      onError('')
      let id = selected
      if (!id) {
        const created = await api<{ id: number }>('/api/v1/admin/videos', { method: 'POST', body: JSON.stringify({ title, names: form.titles }) })
        id = created.id
        setSelected(id)
      }
      await api(`/api/v1/admin/videos/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title,
          names: form.titles,
          accessType: form.accessType,
          priceUsdt: ppv ? Number(form.priceUsdt) : null,
          featured: form.featured,
          categoryIds: form.categoryIds,
          tagIds: form.tagIds,
          personIds: form.personIds,
          seriesId: inSeries ? Number(form.seriesId) : 0,
          episodeNo: inSeries ? Number(form.episodeNo) : null,
        }),
      })
      if (sourceFile) await upload(id, sourceFile)
      if (coverFile) {
        const body = new FormData()
        body.append('file', coverFile)
        const detail = await api<VideoDetail>(`/api/v1/admin/videos/${id}/cover`, { method: 'POST', body })
        setCurrentCover(detail.coverUrl || '')
        setCoverFile(null)
        setCoverPreview('')
      }
      setEditing(false)
      await load()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }

  async function remove(id: number) {
    if (!window.confirm('删除这部影片？订单和投诉记录会留下，影片、收藏和播放文件会去掉。')) return
    try {
      await api(`/api/v1/admin/videos/${id}`, { method: 'DELETE' })
      if (selected === id) {
        setSelected(null)
        setEditing(false)
      }
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }

  async function publish(id: number, published: boolean) {
    try {
      await api(`/api/v1/admin/videos/${id}/publish?published=${published}`, { method: 'PUT' })
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }

  return (
    <>
      <PageBar title="影片管理" onCreate={startNew} />
      {editing && (
      <Editor title={selected ? '编辑影片' : '新建影片'} onClose={() => setEditing(false)}>
      <form className="form" onSubmit={saveMeta}>
        {formError ? <p className="error">{formError}</p> : null}
        <LocaleFields label="标题" required value={form.titles} onChange={(titles) => setForm({ ...form, titles })} />
        <label>观看权限
          <select value={form.accessType} onChange={(e) => setForm({ ...form, accessType: e.target.value, priceUsdt: e.target.value === 'PPV' ? form.priceUsdt : '' })}>
            <option value="FREE">免费</option>
            <option value="MEMBER">会员</option>
            <option value="PPV">单集付费</option>
          </select>
        </label>
        {form.accessType === 'PPV' && (
          <label>单集价格 USDT<span className="req">必填</span>
            <input value={form.priceUsdt} onChange={(e) => setForm({ ...form, priceUsdt: e.target.value })} placeholder="例如 2.00" inputMode="decimal" required />
          </label>
        )}
        <PickList
          label="分类"
          hint="勾选后会出现在导航或首页。"
          empty="还没有分类。先在左侧「分类管理」里添加，再回到这里点选。"
          options={categories}
          values={form.categoryIds}
          onChange={(categoryIds) => setForm({ ...form, categoryIds })}
        />
        <PickList
          label="标签"
          hint="相同标签的影片会互相推荐。"
          empty="还没有标签。先在左侧「标签管理」里添加，再回到这里点选。"
          options={tags}
          values={form.tagIds}
          onChange={(tagIds) => setForm({ ...form, tagIds })}
        />
        <PickList label="人物" empty="还没有人物。先在左侧「人物管理」里添加，再回到这里点选。" options={people} values={form.personIds} onChange={(personIds) => setForm({ ...form, personIds })} />
        <label>所属短剧
          <select value={form.seriesId} onChange={(e) => setForm({ ...form, seriesId: e.target.value, episodeNo: e.target.value ? form.episodeNo : '' })}>
            <option value="">不属于短剧</option>
            {series.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
          </select>
        </label>
        {form.seriesId && (
          <label>集数<span className="req">必填</span>
            <input value={form.episodeNo} onChange={(e) => setForm({ ...form, episodeNo: e.target.value })} placeholder="第几集" inputMode="numeric" required />
          </label>
        )}
        <label className="check"><input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} /> 推荐到首页</label>
        <label>上传封面
          <input type="file" accept="image/*" onChange={(e) => {
            const file = e.target.files?.[0] || null
            setCoverFile(file)
            setCoverPreview((current) => {
              if (current.startsWith('blob:')) URL.revokeObjectURL(current)
              return file ? URL.createObjectURL(file) : ''
            })
          }} />
        </label>
        {(coverPreview || currentCover) ? <img className="import-cover" src={coverPreview || currentCover} alt="" /> : null}
        <label>上传片源
          <input type="file" accept="video/*" onChange={(e) => {
            const file = e.target.files?.[0]
            if (!file) return
            if (selected) upload(selected, file).catch((err) => setFormError(err instanceof Error ? err.message : '上传失败'))
            else setSourceFile(file)
          }} />
        </label>
        {sourceFile && !selected ? <p className="hint">已选择 {sourceFile.name}，保存后开始上传。</p> : null}
        {selected && (
          <>
            <button className="btn ghost" type="button" onClick={() => api<JobView[]>(`/api/v1/admin/videos/${selected}/jobs`).then(setJobs)}>刷新转码状态</button>
            {jobs.map((job) => (
              <p key={job.id}>
                <Badge tone={jobTone(job.state)}>{job.state}</Badge>
                {' '}{job.progress}% {job.errorMessage || ''}
              </p>
            ))}
          </>
        )}
        <div className="row-actions">
          <button className="btn" type="submit">保存</button>
        </div>
      </form>
      </Editor>
      )}
      <div className="table-wrap" style={{ marginTop: 20 }}>
        <table className="table">
          <thead>
            <tr>
              <SortTh field="title" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.title || ''} onChange={(v) => q.setFilter('title', v)} placeholder="标题" />}>标题</SortTh>
              <SortTh field="accessType" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.accessType || ''} onChange={(v) => q.setFilter('accessType', v)} options={[
                { value: 'FREE', label: '免费' }, { value: 'MEMBER', label: '会员' }, { value: 'PPV', label: '单集付费' },
              ]} />}>权限</SortTh>
              <SortTh field="status" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.status || ''} onChange={(v) => q.setFilter('status', v)} options={[
                { value: 'DRAFT', label: '草稿' }, { value: 'UPLOADED', label: '已上传' }, { value: 'TRANSCODING', label: '转码中' },
                { value: 'READY', label: '可发布' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'FAILED', label: '失败' },
              ]} />}>状态</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={4} text={loading ? '加载中' : '没有匹配的影片'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.title}</td>
                <td>{ACCESS_LABEL[item.accessType] || item.accessType}</td>
                <td><Badge tone={item.status === 'PUBLISHED' ? 'ok' : item.status === 'FAILED' ? 'bad' : 'warn'}>{VIDEO_STATUS[item.status] || item.status}</Badge></td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => openVideo(item.id).catch((err) => onError(err.message))}>编辑</button>
                  {item.status === 'PUBLISHED'
                    ? <button className="btn ghost sm" type="button" onClick={() => publish(item.id, false)}>下架</button>
                    : <button className="btn ghost sm" type="button" onClick={() => publish(item.id, true)}>发布</button>}
                  <Link className="btn ghost sm" to={`/videos/${item.id}`}>前台</Link>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item.id)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
    </>
  )
}

function PickList({ label, hint, empty, options, values, onChange }: { label: string; hint?: string; empty: string; options: NamedRef[]; values: number[]; onChange: (values: number[]) => void }) {
  function toggle(id: number) {
    onChange(values.includes(id) ? values.filter((item) => item !== id) : [...values, id])
  }
  return (
    <div className="pick">
      <span>{label}</span>
      {hint ? <p className="hint">{hint}</p> : null}
      {options.length ? (
        <div className="chips">
          {options.map((option) => {
            const on = values.includes(option.id)
            return (
              <button key={option.id} type="button" className={`chip${on ? ' active' : ''}`} aria-pressed={on} onClick={() => toggle(option.id)}>
                {option.name}
              </button>
            )
          })}
        </div>
      ) : <p className="hint">{empty}</p>}
    </div>
  )
}

function Taxonomy({ kind, onError }: { kind: 'categories' | 'tags'; onError: (message: string) => void }) {
  const [items, setItems] = useState<TaxonomyView[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [loading, setLoading] = useState(true)
  const [editor, setEditor] = useState<TaxonomyView | 'new' | null>(null)
  const q = useAdminQuery({ sort: 'sortOrder', dir: 'asc' })
  function listQuery(nextPage: number) {
    const { display, ...rest } = q.filters
    const params: Record<string, string | number | undefined> = {
      ...rest,
      page: nextPage,
      size: 12,
      sort: q.sort,
      dir: q.dir,
    }
    if (kind === 'categories') {
      if (display === 'nav') params.showInNav = 'true'
      else if (display === 'home') params.showOnHome = 'true'
      else if (display === 'both') {
        params.showInNav = 'true'
        params.showOnHome = 'true'
      } else if (display === 'none') {
        params.showInNav = 'false'
        params.showOnHome = 'false'
      }
    }
    const path = kind === 'categories' ? '/api/v1/admin/categories' : '/api/v1/admin/tags'
    return `${path}${queryString(params)}`
  }
  async function load(nextPage = page) {
    const result = await api<PageResult<TaxonomyView>>(listQuery(nextPage))
    setItems(result.items)
    setPage(result.page)
    setTotal(result.total)
    setPageSize(result.pageSize)
  }
  useEffect(() => {
    let alive = true
    setEditor(null)
    setLoading(true)
    api<PageResult<TaxonomyView>>(listQuery(1))
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e instanceof Error ? e.message : '加载失败') })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [kind, q.requestKey])
  async function remove(item: TaxonomyView) {
    const noun = kind === 'categories' ? '分类' : '标签'
    if (!window.confirm(`删除${noun}「${item.name}」？`)) return
    try {
      await api(`/api/v1/admin/${kind}/${item.id}`, { method: 'DELETE' })
      if (editor !== 'new' && editor?.id === item.id) setEditor(null)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  const title = kind === 'categories' ? '分类管理' : '标签管理'
  const empty = loading ? '加载中' : (kind === 'categories' ? '没有匹配的分类' : '没有匹配的标签')
  return (
    <>
      <PageBar title={title} onCreate={() => setEditor('new')} />
      <p className="hint">
        {kind === 'categories' ? '可选择出现在导航或首页。' : '相同标签的影片会互相推荐。'}
      </p>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="name" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.name || ''} onChange={(v) => q.setFilter('name', v)} placeholder="名称" />}>名称</SortTh>
              {kind === 'categories' && (
                <SortTh field="showInNav" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.display || ''} onChange={(v) => q.setFilter('display', v)} options={[
                  { value: 'nav', label: '进导航' },
                  { value: 'home', label: '进首页' },
                  { value: 'both', label: '导航 · 首页' },
                  { value: 'none', label: '都不进' },
                ]} />}>展示</SortTh>
              )}
              <SortTh field="sortOrder" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.sortOrder || ''} onChange={(v) => q.setFilter('sortOrder', v)} placeholder="排序值" />}>排序</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={kind === 'categories' ? 4 : 3} text={empty} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                {kind === 'categories' && <td>{item.showInNav ? '导航' : '不进导航'} · {item.showOnHome ? '首页' : '不进首页'}</td>}
                <td>{item.sortOrder}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setEditor(item)}>编辑</button>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
      {editor && (
        <TaxonomyEditor
          key={editor === 'new' ? 'new' : editor.id}
          kind={kind}
          item={editor === 'new' ? null : editor}
          sortFallback={items.length + 1}
          onClose={() => setEditor(null)}
          onSaved={() => { setEditor(null); return load() }}
          onError={onError}
        />
      )}
    </>
  )
}

function TaxonomyEditor({ kind, item, sortFallback, onClose, onSaved, onError }: { kind: 'categories' | 'tags'; item: TaxonomyView | null; sortFallback: number; onClose: () => void; onSaved: () => Promise<void>; onError: (message: string) => void }) {
  const [names, setNames] = useState(() => contentNames(item?.names, item?.name || ''))
  const [showInNav, setShowInNav] = useState(item?.showInNav || false)
  const [showOnHome, setShowOnHome] = useState(item?.showOnHome || false)
  const [sortOrder, setSortOrder] = useState(String(item?.sortOrder ?? sortFallback))
  const [formError, setFormError] = useState('')
  async function save(event: FormEvent) {
    event.preventDefault()
    const name = primaryName(names)
    if (!name) {
      setFormError('至少填写一种语言的名称')
      return
    }
    const body = {
      name,
      names,
      showInNav: kind === 'categories' && showInNav,
      showOnHome: kind === 'categories' && showOnHome,
      sortOrder: Number(sortOrder) || 0,
      slug: item?.slug,
    }
    try {
      await api(item ? `/api/v1/admin/${kind}/${item.id}` : `/api/v1/admin/${kind}`, {
        method: item ? 'PUT' : 'POST',
        body: JSON.stringify(body),
      })
      onError('')
      await onSaved()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  return (
    <Editor title={item ? (kind === 'categories' ? '编辑分类' : '编辑标签') : (kind === 'categories' ? '新建分类' : '新建标签')} onClose={onClose}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <LocaleFields label="名称" required value={names} onChange={setNames} maxLength={80} />
        {kind === 'categories' && (
          <>
            <label className="check"><input type="checkbox" checked={showInNav} onChange={(e) => setShowInNav(e.target.checked)} /> 出现在导航</label>
            <label className="check"><input type="checkbox" checked={showOnHome} onChange={(e) => setShowOnHome(e.target.checked)} /> 出现在首页分区</label>
          </>
        )}
        <label>排序
          <input value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} inputMode="numeric" />
        </label>
        <button className="btn" type="submit">保存</button>
      </form>
    </Editor>
  )
}

function PeopleAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<PersonRef[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [loading, setLoading] = useState(true)
  const [editor, setEditor] = useState<PersonRef | 'new' | null>(null)
  const q = useAdminQuery({ sort: 'name', dir: 'asc' })
  async function load(nextPage = page) {
    const result = await api<PageResult<PersonRef>>(`/api/v1/admin/people${q.query({ page: nextPage, size: 12 })}`)
    setItems(result.items)
    setPage(result.page)
    setTotal(result.total)
    setPageSize(result.pageSize)
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PageResult<PersonRef>>(`/api/v1/admin/people${q.query({ page: 1, size: 12 })}`)
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  async function remove(item: PersonRef) {
    if (!window.confirm(`删除人物「${item.name}」？`)) return
    try {
      await api(`/api/v1/admin/people/${item.id}`, { method: 'DELETE' })
      if (editor !== 'new' && editor?.id === item.id) setEditor(null)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <PageBar title="人物管理" onCreate={() => setEditor('new')} />
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="name" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.name || ''} onChange={(v) => q.setFilter('name', v)} placeholder="名字" />}>名字</SortTh>
              <SortTh field="kind" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.kind || ''} onChange={(v) => q.setFilter('kind', v)} options={[
                { value: 'CREATOR', label: '导演' }, { value: 'PERFORMER', label: '演员' },
              ]} />}>类型</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={3} text={loading ? '加载中' : '没有匹配的人物'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>
                  <span className="name-with-photo">
                    {item.avatarUrl
                      ? <img className="person-avatar" src={item.avatarUrl} alt="" />
                      : <span className="person-avatar placeholder">{item.name.slice(0, 1)}</span>}
                    {item.name}
                  </span>
                </td>
                <td>{item.kind === 'CREATOR' ? '导演' : '演员'}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setEditor(item)}>编辑</button>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
      {editor && (
        <PersonEditor
          key={editor === 'new' ? 'new' : editor.id}
          item={editor === 'new' ? null : editor}
          onClose={() => setEditor(null)}
          onSaved={async () => { setEditor(null); await load() }}
          onChanged={() => load()}
          onError={onError}
        />
      )}
    </>
  )
}

function PersonEditor({ item, onClose, onSaved, onChanged, onError }: { item: PersonRef | null; onClose: () => void; onSaved: () => Promise<void>; onChanged: () => Promise<void>; onError: (message: string) => void }) {
  const [names, setNames] = useState(() => contentNames(item?.names, item?.name || ''))
  const [kind, setKind] = useState(item?.kind || 'PERFORMER')
  const [avatarUrl, setAvatarUrl] = useState(item?.avatarUrl || '')
  const [avatarFile, setAvatarFile] = useState<File | null>(null)
  const [notice, setNotice] = useState('')
  const [formError, setFormError] = useState('')
  async function uploadAvatar(id: number, file: File) {
    const body = new FormData()
    body.append('file', file)
    const saved = await api<PersonRef>(`/api/v1/admin/people/${id}/avatar`, { method: 'POST', body })
    setAvatarUrl(saved.avatarUrl || '')
    setAvatarFile(null)
    setNotice('头像已更新')
    return saved
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    try {
      const name = primaryName(names)
      if (!name) {
        setFormError('至少填写一种语言的名字')
        return
      }
      const saved = item
        ? await api<PersonRef>(`/api/v1/admin/people/${item.id}`, { method: 'PUT', body: JSON.stringify({ name, names, kind, slug: item.slug }) })
        : await api<PersonRef>('/api/v1/admin/people', { method: 'POST', body: JSON.stringify({ name, names, kind }) })
      if (avatarFile) await uploadAvatar(saved.id, avatarFile)
      onError('')
      await onSaved()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  async function avatar(file: File) {
    setAvatarFile(file)
    if (!item) {
      setAvatarUrl(URL.createObjectURL(file))
      return
    }
    try {
      await uploadAvatar(item.id, file)
      onError('')
      await onChanged()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <Editor title={item ? '编辑人物' : '新建人物'} onClose={onClose}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <LocaleFields label="名字" required value={names} onChange={setNames} maxLength={120} />
        <label>类型
          <select value={kind} onChange={(e) => setKind(e.target.value)}><option value="PERFORMER">演员</option><option value="CREATOR">导演</option></select>
        </label>
        <label>头像
          <input type="file" accept="image/*" onChange={(e) => e.target.files?.[0] && avatar(e.target.files[0])} />
        </label>
        {avatarUrl ? <img className="import-cover person-portrait" src={avatarUrl} alt="" /> : <p className="hint">可以选择一张人物照片。</p>}
        {notice ? <p className="hint">{notice}</p> : null}
        <button className="btn" type="submit">保存</button>
      </form>
    </Editor>
  )
}

function SeriesAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<SeriesCard[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [selected, setSelected] = useState<number | null>(null)
  const [titles, setTitles] = useState(() => contentNames())
  const [status, setStatus] = useState('ONGOING')
  const [coverFile, setCoverFile] = useState<File | null>(null)
  const [currentCover, setCurrentCover] = useState('')
  const [editing, setEditing] = useState(false)
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(true)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })
  async function load(nextPage = page) {
    const result = await api<PageResult<SeriesCard>>(`/api/v1/admin/series${q.query({ page: nextPage, size: 12 })}`)
    setItems(result.items)
    setPage(result.page)
    setTotal(result.total)
    setPageSize(result.pageSize)
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PageResult<SeriesCard>>(`/api/v1/admin/series${q.query({ page: 1, size: 12 })}`)
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  function reset() {
    setSelected(null)
    setTitles(contentNames())
    setStatus('ONGOING')
    setCoverFile(null)
    setCurrentCover('')
    setFormError('')
    setEditing(true)
  }
  function open(item: SeriesCard) {
    setSelected(item.id)
    setTitles(contentNames(item.names, item.title))
    setStatus(item.status)
    setCoverFile(null)
    setCurrentCover(item.coverUrl || '')
    setFormError('')
    setEditing(true)
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    const title = primaryName(titles)
    if (!title) {
      setFormError('至少填写一种语言的剧名')
      return
    }
    try {
      setFormError('')
      const body = { title, names: titles, status }
      const saved = selected
        ? await api<SeriesCard>(`/api/v1/admin/series/${selected}`, { method: 'PUT', body: JSON.stringify({ ...body, slug: items.find((item) => item.id === selected)?.slug }) })
        : await api<SeriesCard>('/api/v1/admin/series', { method: 'POST', body: JSON.stringify(body) })
      if (coverFile) {
        const body = new FormData()
        body.append('file', coverFile)
        const next = await api<SeriesCard>(`/api/v1/admin/series/${saved.id}/cover`, { method: 'POST', body })
        setCurrentCover(next.coverUrl || '')
        setCoverFile(null)
      }
      setSelected(saved.id)
      onError('')
      setEditing(false)
      await load()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  async function removeSeries(item: SeriesCard) {
    if (!window.confirm(`删除短剧「${item.title}」？`)) return
    try {
      await api(`/api/v1/admin/series/${item.id}`, { method: 'DELETE' })
      if (selected === item.id) setEditing(false)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <PageBar title="短剧管理" onCreate={reset} />
      {editing && (
      <Editor title={selected ? '编辑短剧' : '新建短剧'} onClose={() => setEditing(false)}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <LocaleFields label="剧名" required value={titles} onChange={setTitles} maxLength={200} />
        <label>状态
          <select value={status} onChange={(e) => setStatus(e.target.value)}><option value="ONGOING">更新中</option><option value="COMPLETED">已完结</option></select>
        </label>
        <label>上传封面
          <input type="file" accept="image/*" onChange={(e) => setCoverFile(e.target.files?.[0] || null)} />
        </label>
        {currentCover ? <img className="import-cover" src={currentCover} alt="" /> : null}
        <div className="row-actions">
          <button className="btn" type="submit">保存</button>
        </div>
      </form>
      </Editor>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="title" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.title || ''} onChange={(v) => q.setFilter('title', v)} placeholder="剧名" />}>剧名</SortTh>
              <SortTh field="status" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.status || ''} onChange={(v) => q.setFilter('status', v)} options={[
                { value: 'ONGOING', label: '更新中' }, { value: 'COMPLETED', label: '已完结' },
              ]} />}>状态</SortTh>
              <SortTh field="episodes" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.episodes || ''} onChange={(v) => q.setFilter('episodes', v)} placeholder="集数" />}>集数</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={4} text={loading ? '加载中' : '没有匹配的短剧'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.title}</td>
                <td><Badge tone={item.status === 'COMPLETED' ? 'ok' : 'warn'}>{item.status === 'COMPLETED' ? '已完结' : '更新中'}</Badge></td>
                <td>{item.episodes}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => open(item)}>编辑</button>
                  <Link className="btn ghost sm" to={`/series/${item.slug}`}>前台</Link>
                  <button className="btn ghost sm" type="button" onClick={() => removeSeries(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
    </>
  )
}

function PlansAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<PlanView[]>([])
  const [loading, setLoading] = useState(true)
  const [editor, setEditor] = useState<PlanView | 'new' | null>(null)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })
  async function load() {
    setItems(await api<PlanView[]>(`/api/v1/admin/plans${q.query()}`))
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PlanView[]>(`/api/v1/admin/plans${q.query()}`)
      .then((next) => { if (alive) setItems(next) })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  async function remove(item: PlanView) {
    if (!window.confirm(`删除方案「${item.name}」？`)) return
    try {
      await api(`/api/v1/admin/plans/${item.id}`, { method: 'DELETE' })
      if (editor !== 'new' && editor?.id === item.id) setEditor(null)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <PageBar title="会员方案" onCreate={() => setEditor('new')} />
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="name" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.name || ''} onChange={(v) => q.setFilter('name', v)} placeholder="名称" />}>名称</SortTh>
              <SortTh field="enabled" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.enabled || ''} onChange={(v) => q.setFilter('enabled', v)} options={[
                { value: 'true', label: '上架' }, { value: 'false', label: '下架' },
              ]} />}>状态</SortTh>
              <SortTh field="durationDays" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.durationDays || ''} onChange={(v) => q.setFilter('durationDays', v)} placeholder="天数" />}>天数</SortTh>
              <SortTh field="priceUsdt" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.priceUsdt || ''} onChange={(v) => q.setFilter('priceUsdt', v)} placeholder="价格" />}>价格</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={5} text={loading ? '加载中' : '没有匹配的方案'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                <td><Badge tone={item.enabled ? 'ok' : 'default'}>{item.enabled ? '上架' : '下架'}</Badge></td>
                <td>{item.durationDays}</td>
                <td>{Number(item.priceUsdt).toFixed(2)} USDT</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setEditor(item)}>编辑</button>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editor && (
        <PlanEditor
          key={editor === 'new' ? 'new' : editor.id}
          item={editor === 'new' ? null : editor}
          onClose={() => setEditor(null)}
          onSaved={async () => { setEditor(null); await load() }}
          onError={onError}
        />
      )}
    </>
  )
}

function PlanEditor({ item, onClose, onSaved, onError }: { item: PlanView | null; onClose: () => void; onSaved: () => Promise<void>; onError: (message: string) => void }) {
  const [names, setNames] = useState(() => contentNames(item?.names, item?.name || ''))
  const [days, setDays] = useState(item ? String(item.durationDays) : '30')
  const [price, setPrice] = useState(item ? Number(item.priceUsdt).toFixed(2) : '10')
  const [enabled, setEnabled] = useState(item?.enabled ?? true)
  const [formError, setFormError] = useState('')
  async function save(event: FormEvent) {
    event.preventDefault()
    const name = primaryName(names)
    if (!name) {
      setFormError('至少填写一种语言的名称')
      return
    }
    const body = { name, names, durationDays: Number(days), priceUsdt: Number(price), enabled }
    try {
      await api(item ? `/api/v1/admin/plans/${item.id}` : '/api/v1/admin/plans', {
        method: item ? 'PUT' : 'POST',
        body: JSON.stringify(body),
      })
      onError('')
      await onSaved()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  return (
    <Editor title={item ? '编辑方案' : '新建方案'} onClose={onClose}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <LocaleFields label="名称" required value={names} onChange={setNames} maxLength={80} />
        <label className="check"><input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} /> 上架</label>
        <label>天数<span className="req">必填</span><input value={days} onChange={(e) => setDays(e.target.value)} inputMode="numeric" required /></label>
        <label>价格 USDT<span className="req">必填</span><input value={price} onChange={(e) => setPrice(e.target.value)} inputMode="decimal" required /></label>
        <button className="btn" type="submit">保存</button>
      </form>
    </Editor>
  )
}

function AdsAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<AdView[]>([])
  const [loading, setLoading] = useState(true)
  const [editor, setEditor] = useState<AdView | 'new' | null>(null)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })
  async function load() {
    setItems(await api<AdView[]>(`/api/v1/admin/ads${q.query()}`))
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<AdView[]>(`/api/v1/admin/ads${q.query()}`)
      .then((next) => { if (alive) setItems(next) })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  const slotName: Record<string, string> = { TOP: '页面顶部', PLAYER_BOTTOM: '播放器下方', POPUP: '弹窗' }
  async function remove(item: AdView) {
    if (!window.confirm('删除这条广告？')) return
    try {
      await api(`/api/v1/admin/ads/${item.id}`, { method: 'DELETE' })
      if (editor !== 'new' && editor?.id === item.id) setEditor(null)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <PageBar title="广告管理" onCreate={() => setEditor('new')} />
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="slot" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.slot || ''} onChange={(v) => q.setFilter('slot', v)} options={[
                { value: 'TOP', label: '页面顶部' }, { value: 'PLAYER_BOTTOM', label: '播放器下方' }, { value: 'POPUP', label: '弹窗' },
              ]} />}>位置</SortTh>
              <SortTh field="enabled" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.enabled || ''} onChange={(v) => q.setFilter('enabled', v)} options={[
                { value: 'true', label: '启用' }, { value: 'false', label: '停用' },
              ]} />}>状态</SortTh>
              <SortTh field="targetUrl" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.targetUrl || ''} onChange={(v) => q.setFilter('targetUrl', v)} placeholder="跳转链接" />}>跳转</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={4} text={loading ? '加载中' : '没有匹配的广告'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{slotName[item.slot] || item.slot}</td>
                <td><Badge tone={item.enabled ? 'ok' : 'default'}>{item.enabled ? '启用' : '停用'}</Badge></td>
                <td>{item.targetUrl || '—'}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setEditor(item)}>编辑</button>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editor && (
        <AdEditor
          key={editor === 'new' ? 'new' : editor.id}
          item={editor === 'new' ? null : editor}
          onClose={() => setEditor(null)}
          onSaved={async () => { setEditor(null); await load() }}
          onError={onError}
        />
      )}
    </>
  )
}

function AdEditor({ item, onClose, onSaved, onError }: { item: AdView | null; onClose: () => void; onSaved: () => Promise<void>; onError: (message: string) => void }) {
  const [slot, setSlot] = useState(item?.slot || 'TOP')
  const [targetUrl, setTargetUrl] = useState(item?.targetUrl || '')
  const [enabled, setEnabled] = useState(item?.enabled ?? true)
  const [startAt, setStartAt] = useState(toLocalInput(item?.startAt || null))
  const [endAt, setEndAt] = useState(toLocalInput(item?.endAt || null))
  const [sortOrder, setSortOrder] = useState(String(item?.sortOrder ?? 0))
  const [file, setFile] = useState<File | null>(null)
  const [formError, setFormError] = useState('')
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!item && !file) {
      setFormError('请选择素材')
      return
    }
    const body = new FormData()
    body.append('slot', slot)
    body.append('targetUrl', targetUrl)
    body.append('enabled', String(enabled))
    body.append('sortOrder', sortOrder || '0')
    if (startAt) body.append('startAt', new Date(startAt).toISOString())
    if (endAt) body.append('endAt', new Date(endAt).toISOString())
    if (file) body.append('file', file)
    try {
      await api(item ? `/api/v1/admin/ads/${item.id}` : '/api/v1/admin/ads', { method: item ? 'PUT' : 'POST', body })
      onError('')
      await onSaved()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  return (
    <Editor title={item ? '编辑广告' : '新建广告'} onClose={onClose}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <label>位置
          <select value={slot} onChange={(e) => setSlot(e.target.value)}>
            <option value="TOP">页面顶部</option>
            <option value="PLAYER_BOTTOM">播放器下方</option>
            <option value="POPUP">弹窗</option>
          </select>
        </label>
        <label className="check"><input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} /> 启用</label>
        <label>开始
          <input type="datetime-local" value={startAt} onChange={(e) => setStartAt(e.target.value)} />
        </label>
        <label>结束
          <input type="datetime-local" value={endAt} onChange={(e) => setEndAt(e.target.value)} />
        </label>
        <label>排序
          <input value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} inputMode="numeric" />
        </label>
        <label>素材{item ? null : <span className="req">必填</span>}
          <input type="file" accept="image/*" onChange={(e) => setFile(e.target.files?.[0] || null)} />
        </label>
        {item?.imageUrl ? <img className="import-cover" src={item.imageUrl} alt="" /> : null}
        <label>跳转链接
          <input value={targetUrl} onChange={(e) => setTargetUrl(e.target.value)} placeholder="https://" />
        </label>
        <button className="btn" type="submit">保存</button>
      </form>
    </Editor>
  )
}

function toLocalInput(value: string | null) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function when(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

function OrdersAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<OrderView[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [current, setCurrent] = useState<OrderView | null>(null)
  const [loading, setLoading] = useState(true)
  const q = useAdminQuery({ sort: 'createdAt', dir: 'desc' })
  function load(nextPage = page) {
    return api<PageResult<OrderView>>(`/api/v1/admin/orders${q.query({ page: nextPage, size: 12 })}`).then((result) => {
      setItems(result.items)
      setPage(result.page)
      setTotal(result.total)
      setPageSize(result.pageSize)
      setCurrent((open) => open ? result.items.find((item) => item.id === open.id) || open : null)
    })
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PageResult<OrderView>>(`/api/v1/admin/orders${q.query({ page: 1, size: 12 })}`)
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  async function act(id: number, action: 'confirm' | 'cancel') {
    const verb = action === 'confirm' ? '确认这笔订单已到账？' : '关闭这笔待支付订单？'
    if (!window.confirm(verb)) return
    try {
      await api(`/api/v1/admin/orders/${id}/${action}`, { method: 'POST' })
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <h1>订单管理</h1>
      <p className="hint">订单由购买产生。待支付的可以确认到账或关闭。表头可排序，筛选项会即时生效。</p>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="id" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.id || ''} onChange={(v) => q.setFilter('id', v)} placeholder="编号" />}>编号</SortTh>
              <SortTh field="orderType" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.orderType || ''} onChange={(v) => q.setFilter('orderType', v)} options={[
                { value: 'MEMBERSHIP', label: '会员' }, { value: 'PPV', label: '单集' },
              ]} />}>类型</SortTh>
              <SortTh field="state" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.state || ''} onChange={(v) => q.setFilter('state', v)} options={[
                { value: 'PENDING', label: '待支付' }, { value: 'PAID', label: '已支付' }, { value: 'EXPIRED', label: '已关闭' },
              ]} />}>状态</SortTh>
              <SortTh field="payAmount" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.payAmount || ''} onChange={(v) => q.setFilter('payAmount', v)} placeholder="金额" />}>金额</SortTh>
              <SortTh field="createdAt" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.createdAt || ''} onChange={(v) => q.setFilter('createdAt', v)} placeholder="时间" />}>时间</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={6} text={loading ? '加载中' : '没有匹配的订单'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>#{item.id}</td>
                <td>{ORDER_TYPE[item.orderType] || item.orderType}</td>
                <td><Badge tone={item.state === 'PAID' ? 'ok' : item.state === 'EXPIRED' ? 'bad' : 'warn'}>{ORDER_STATE[item.state] || item.state}</Badge></td>
                <td>{item.payAmount}</td>
                <td>{when(item.createdAt)}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setCurrent(item)}>查看</button>
                  {item.state === 'PENDING' && <button className="btn ghost sm" type="button" onClick={() => act(item.id, 'confirm')}>确认到账</button>}
                  {item.state === 'PENDING' && <button className="btn ghost sm" type="button" onClick={() => act(item.id, 'cancel')}>关闭</button>}
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
      {current && (
        <Editor title={`订单 #${current.id}`} onClose={() => setCurrent(null)}>
          <div className="form">
            <p>类型：{ORDER_TYPE[current.orderType] || current.orderType}</p>
            <p>渠道：{PAY_CHANNEL[current.paymentMethod] || current.paymentMethod}</p>
            <p>状态：{ORDER_STATE[current.state] || current.state}</p>
            <p>应付：{current.payAmount} USDT</p>
            <p>标价：{current.listPrice} USDT</p>
            <p>用户：#{current.userId ?? '—'}</p>
            <p>方案：{current.planId ? `#${current.planId}` : '—'}</p>
            <p>影片：{current.videoId ? <Link to={`/videos/${current.videoId}`}>#{current.videoId}</Link> : '—'}</p>
            <p>收款地址：{current.address || '—'}</p>
            <p>付款地址：{current.payerAddress || '—'}</p>
            <p>交易哈希：{current.txHash || '—'}</p>
            <p>创建：{when(current.createdAt)}</p>
            <p>过期：{when(current.expiresAt)}</p>
            {current.state === 'PENDING' && (
              <div className="row-actions">
                <button className="btn" type="button" onClick={() => act(current.id, 'confirm')}>确认到账</button>
                <button className="btn ghost" type="button" onClick={() => act(current.id, 'cancel')}>关闭</button>
              </div>
            )}
          </div>
        </Editor>
      )}
    </>
  )
}

type UserRow = { id: number; username: string; role: string; memberUntil: string | null }

function UsersAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<UserRow[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [editor, setEditor] = useState<UserRow | 'new' | null>(null)
  const [loading, setLoading] = useState(true)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })
  function load(nextPage = page) {
    return api<PageResult<UserRow>>(`/api/v1/admin/users${q.query({ page: nextPage, size: 12 })}`).then((result) => {
      setItems(result.items)
      setPage(result.page)
      setTotal(result.total)
      setPageSize(result.pageSize)
    })
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PageResult<UserRow>>(`/api/v1/admin/users${q.query({ page: 1, size: 12 })}`)
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  async function remove(item: UserRow) {
    if (!window.confirm(`删除用户「${item.username}」？订单记录会留下。`)) return
    try {
      await api(`/api/v1/admin/users/${item.id}`, { method: 'DELETE' })
      if (editor !== 'new' && editor?.id === item.id) setEditor(null)
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <PageBar title="用户管理" onCreate={() => setEditor('new')} />
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="username" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.username || ''} onChange={(v) => q.setFilter('username', v)} placeholder="用户名" />}>用户名</SortTh>
              <SortTh field="role" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.role || ''} onChange={(v) => q.setFilter('role', v)} options={[
                { value: 'USER', label: '用户' }, { value: 'ADMIN', label: '管理员' },
              ]} />}>角色</SortTh>
              <SortTh field="memberUntil" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.memberUntil || ''} onChange={(v) => q.setFilter('memberUntil', v)} placeholder="到期时间" />}>会员到期</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={4} text={loading ? '加载中' : '没有匹配的用户'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.username}</td>
                <td>{item.role === 'ADMIN' ? '管理员' : '用户'}</td>
                <td>{when(item.memberUntil)}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setEditor(item)}>编辑</button>
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
      {editor && (
        <UserEditor
          key={editor === 'new' ? 'new' : editor.id}
          item={editor === 'new' ? null : editor}
          onClose={() => setEditor(null)}
          onSaved={async () => { setEditor(null); await load() }}
          onError={onError}
        />
      )}
    </>
  )
}

function UserEditor({ item, onClose, onSaved, onError }: { item: UserRow | null; onClose: () => void; onSaved: () => Promise<void>; onError: (message: string) => void }) {
  const [username, setUsername] = useState(item?.username || '')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState(item?.role || 'USER')
  const [formError, setFormError] = useState('')
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!item && !username.trim()) {
      setFormError('请填写用户名')
      return
    }
    if (!item && password.trim().length < 6) {
      setFormError('密码至少 6 位')
      return
    }
    if (item && password.trim() && password.trim().length < 6) {
      setFormError('新密码至少 6 位')
      return
    }
    try {
      if (item) {
        await api(`/api/v1/admin/users/${item.id}`, { method: 'PUT', body: JSON.stringify({ role, password: password.trim() }) })
      } else {
        await api('/api/v1/admin/users', { method: 'POST', body: JSON.stringify({ username: username.trim(), password: password.trim(), role }) })
      }
      onError('')
      await onSaved()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  return (
    <Editor title={item ? '编辑用户' : '新建用户'} onClose={onClose}>
      <form className="form" onSubmit={save}>
        {formError ? <p className="error">{formError}</p> : null}
        <label>用户名{item ? null : <span className="req">必填</span>}
          <input value={username} onChange={(e) => setUsername(e.target.value)} required disabled={Boolean(item)} />
        </label>
        <label>角色
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="USER">用户</option>
            <option value="ADMIN">管理员</option>
          </select>
        </label>
        <label>{item ? '新密码' : '密码'}{item ? null : <span className="req">必填</span>}
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="text" placeholder={item ? '留空则不修改' : '至少 6 位'} autoComplete="off" />
        </label>
        {item && <p className="hint">会员到期 {when(item.memberUntil)}，由订单到账后更新。</p>}
        <button className="btn" type="submit">保存</button>
      </form>
    </Editor>
  )
}

type ReportRow = { id: number; videoId: number | null; contact: string; message: string; status: string; createdAt: string }

function ReportsAdmin({ onError }: { onError: (message: string) => void }) {
  const [items, setItems] = useState<ReportRow[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [pageSize, setPageSize] = useState(12)
  const [current, setCurrent] = useState<ReportRow | null>(null)
  const [loading, setLoading] = useState(true)
  const q = useAdminQuery({ sort: 'id', dir: 'desc' })
  function load(nextPage = page) {
    return api<PageResult<ReportRow>>(`/api/v1/admin/reports${q.query({ page: nextPage, size: 12 })}`).then((result) => {
      setItems(result.items)
      setPage(result.page)
      setTotal(result.total)
      setPageSize(result.pageSize)
      setCurrent((open) => open ? result.items.find((item) => item.id === open.id) || null : null)
    })
  }
  useEffect(() => {
    let alive = true
    setLoading(true)
    api<PageResult<ReportRow>>(`/api/v1/admin/reports${q.query({ page: 1, size: 12 })}`)
      .then((result) => {
        if (!alive) return
        setItems(result.items)
        setPage(result.page)
        setTotal(result.total)
        setPageSize(result.pageSize)
      })
      .catch((e) => { if (alive) onError(e.message) })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [q.requestKey])
  async function setStatus(item: ReportRow, handled: boolean) {
    try {
      await api(`/api/v1/admin/reports/${item.id}/${handled ? 'handle' : 'reopen'}`, { method: 'PUT' })
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  async function remove(item: ReportRow) {
    if (!window.confirm('删除这条投诉？')) return
    try {
      await api(`/api/v1/admin/reports/${item.id}`, { method: 'DELETE' })
      onError('')
      await load()
    } catch (e) {
      fail(onError, e)
    }
  }
  return (
    <>
      <h1>投诉处理</h1>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <SortTh field="contact" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.contact || ''} onChange={(v) => q.setFilter('contact', v)} placeholder="联系方式" />}>联系方式</SortTh>
              <SortTh field="videoId" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.videoId || ''} onChange={(v) => q.setFilter('videoId', v)} placeholder="影片编号" />}>影片</SortTh>
              <SortTh field="status" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterSelect value={q.draft.status || ''} onChange={(v) => q.setFilter('status', v)} options={[
                { value: 'OPEN', label: '待处理' }, { value: 'HANDLED', label: '已处理' },
              ]} />}>状态</SortTh>
              <SortTh field="createdAt" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.createdAt || ''} onChange={(v) => q.setFilter('createdAt', v)} placeholder="时间" />}>时间</SortTh>
              <SortTh field="message" sort={q.sort} dir={q.dir} onSort={q.toggleSort} filter={<FilterInput value={q.draft.message || ''} onChange={(v) => q.setFilter('message', v)} placeholder="说明" />}>说明</SortTh>
              <OpsHead />
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && <EmptyRow cols={6} text={loading ? '加载中' : '没有匹配的投诉'} />}
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.contact}</td>
                <td>{item.videoId ? <Link to={`/videos/${item.videoId}`}>#{item.videoId}</Link> : '—'}</td>
                <td><Badge tone={item.status === 'HANDLED' ? 'ok' : 'warn'}>{item.status === 'HANDLED' ? '已处理' : '待处理'}</Badge></td>
                <td>{when(item.createdAt)}</td>
                <td>{item.message.length > 24 ? `${item.message.slice(0, 24)}…` : item.message}</td>
                <Ops>
                  <button className="btn ghost sm" type="button" onClick={() => setCurrent(item)}>查看</button>
                  {item.status === 'HANDLED'
                    ? <button className="btn ghost sm" type="button" onClick={() => setStatus(item, false)}>重新打开</button>
                    : <button className="btn ghost sm" type="button" onClick={() => setStatus(item, true)}>标记已处理</button>}
                  <button className="btn ghost sm" type="button" onClick={() => remove(item)}>删除</button>
                </Ops>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={page} total={total} pageSize={pageSize} onPage={(next) => load(next).catch((e) => onError(e.message))} />
      {current && (
        <Editor title="投诉详情" onClose={() => setCurrent(null)}>
          <div className="form">
            <p>联系方式：{current.contact}</p>
            <p>影片：{current.videoId ? <Link to={`/videos/${current.videoId}`}>#{current.videoId}</Link> : '—'}</p>
            <p>状态：{current.status === 'HANDLED' ? '已处理' : '待处理'}</p>
            <p>时间：{when(current.createdAt)}</p>
            <p style={{ whiteSpace: 'pre-wrap' }}>{current.message}</p>
            <div className="row-actions">
              {current.status === 'HANDLED'
                ? <button className="btn ghost" type="button" onClick={() => setStatus(current, false)}>重新打开</button>
                : <button className="btn" type="button" onClick={() => setStatus(current, true)}>标记已处理</button>}
              <button className="btn ghost" type="button" onClick={() => remove(current)}>删除</button>
            </div>
          </div>
        </Editor>
      )}
    </>
  )
}

type PlatformForm = {
  storageMode: string
  deliveryMode: string
  originBase: string
  cdnBase: string
  objectConfigured: boolean
  usdtAddress: string
  paymentSimulate: boolean
  tronApiKeySet: boolean
  tronApiKey: string
  tronNetwork: string
  bscAddress: string
  bscNetwork: string
  siteName: string
  s3Endpoint: string
  s3Region: string
  s3Bucket: string
  s3AccessKey: string
  s3SecretKeySet: boolean
  s3SecretKey: string
}

type ChainForm = { code: string; address: string; network?: string }
type PayTab = 'TRON' | 'BSC' | 'ETH' | 'POLYGON' | 'ARBITRUM' | 'AVALANCHE' | 'SOLANA' | 'TON'

const PAY_TABS: { id: PayTab; label: string }[] = [
  { id: 'TRON', label: '波场' },
  { id: 'BSC', label: '币安链' },
  { id: 'ETH', label: '以太坊' },
  { id: 'POLYGON', label: 'Polygon' },
  { id: 'ARBITRUM', label: 'Arbitrum' },
  { id: 'AVALANCHE', label: 'Avalanche' },
  { id: 'SOLANA', label: 'Solana' },
  { id: 'TON', label: 'TON' },
]

const PAY_GUIDE: Record<PayTab, { main: string[]; test?: string[] }> = {
  TRON: {
    main: [
      '收款地址填你自己的波场地址，以 T 开头。',
      '买家要在波场主网转入 USDT。从交易所提币时，网络选 TRC20。',
    ],
    test: [
      '用 TronLink，把网络切到 Nile。收款地址也必须是 Nile 上的地址。',
      '打开 https://nileex.io/join/getJoinPage 领取测试 TRX 和测试 USDT。',
      '转账时代币选测试 USDT，金额与订单完全一致。主网 USDT 和 TRX 不会入账。',
    ],
  },
  BSC: {
    main: [
      '收款地址填 0x 开头的地址。',
      '买家要在 BNB Smart Chain 转入 USDT。从交易所提币时，网络选 BEP20。',
    ],
    test: [
      '用 MetaMask 添加 BSC 测试网，链 ID 97，RPC 填 https://bsc-testnet.publicnode.com。',
      '打开 https://www.bnbchain.org/en/testnet-faucet ，选择 USDT。领取需要主网账户里有 0.002 BNB，每 24 小时一次。',
      '只认 Chapel 上的测试 USDT，合约是 0x337610d27c682E347C9cD60BD4b3b107C9d34dDd。tBNB 和主网 USDT 不会入账。',
    ],
  },
  ETH: {
    main: [
      '收款地址填 0x 开头的地址。',
      '买家要在以太坊主网转入 USDT。从交易所提币时，网络选 ERC20。',
    ],
    test: [
      '用 MetaMask 切到 Sepolia。手续费用的 Sepolia ETH 可以在 https://faucets.chain.link/sepolia 领取。',
      '测试 USDT 在 https://bridge-testnet.aave.com/faucet/?marketName=proto_sepolia_v3 领取，选 USDT。',
      '只认这枚测试 USDT，合约是 0xaA8E23Fb1079EA71e0a56F48a2aA51851D8433D0。别的测试 USDT 和主网 USDT 都不会入账。',
    ],
  },
  POLYGON: {
    main: [
      '收款地址填 0x 开头的地址。它可以和以太坊使用同一个地址，但买家必须在 Polygon 上转。',
      '从交易所提 USDT 时，网络选 Polygon。这条链只认主网 USDT。',
    ],
  },
  ARBITRUM: {
    main: [
      '收款地址填 0x 开头的地址。',
      '从交易所提 USDT 时，网络选 Arbitrum One。这条链只认主网 USDT。',
    ],
  },
  AVALANCHE: {
    main: [
      '收款地址填 0x 开头的地址。',
      '从交易所提 USDT 时，网络选 Avalanche C-Chain。这条链只认主网 USDT。',
    ],
  },
  SOLANA: {
    main: [
      '收款地址填 Solana 地址，不是 0x 开头。',
      '从交易所提 USDT 时，网络选 Solana。这条链只认主网 USDT。',
    ],
  },
  TON: {
    main: [
      '收款地址填 TON 地址，一般以 EQ 或 UQ 开头。',
      '从交易所提 USDT 时，网络选 TON。这条链只认主网 USDT。',
    ],
  },
}

function guideText(text: string) {
  return text.split(/(https?:\/\/[^\s，。]+)/).map((part, index) => (
    part.startsWith('http')
      ? <a key={index} href={part} target="_blank" rel="noreferrer">{part}</a>
      : <span key={index}>{part}</span>
  ))
}

function PaymentsAdmin({ onError }: { onError: (message: string) => void }) {
  const [form, setForm] = useState<PlatformForm | null>(null)
  const [chains, setChains] = useState<ChainForm[]>([])
  const [tab, setTab] = useState<PayTab>('TRON')
  const [saved, setSaved] = useState(false)
  useEffect(() => {
    Promise.all([
      api<PlatformForm>('/api/v1/admin/platform-config'),
      api<ChainForm[]>('/api/v1/admin/chain-receivers'),
    ]).then(([next, rows]) => {
      setForm({ ...next, tronApiKey: '', s3SecretKey: next.s3SecretKey || '' })
      setChains(rows)
    }).catch((e) => onError(e.message))
  }, [])
  if (!form) return <h1>收款方式</h1>
  function patch(next: Partial<PlatformForm>) {
    setSaved(false)
    setForm({ ...form!, ...next })
  }
  function patchChain(code: string, next: Partial<ChainForm>) {
    setSaved(false)
    setChains((current) => current.map((item) => item.code === code ? { ...item, ...next } : item))
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!form!.usdtAddress.trim() && !form!.bscAddress.trim() && !chains.some((item) => item.address.trim())) {
      onError('请填写至少一个收款地址')
      return
    }
    try {
      const savedChains = await api<ChainForm[]>('/api/v1/admin/chain-receivers', {
        method: 'PUT',
        body: JSON.stringify(chains.map((item) => ({
          code: item.code,
          address: item.address.trim(),
          network: item.code === 'ETH' && item.network === 'TESTNET' ? 'TESTNET' : 'MAINNET',
        }))),
      })
      setChains(savedChains)
      const next = await api<PlatformForm>('/api/v1/admin/platform-config', {
        method: 'PUT',
        body: JSON.stringify({
          storageMode: form!.storageMode,
          deliveryMode: form!.deliveryMode,
          originBase: form!.originBase,
          cdnBase: form!.cdnBase,
          usdtAddress: form!.usdtAddress.trim(),
          tronApiKey: form!.tronApiKey.trim() || null,
          tronNetwork: form!.tronNetwork || 'MAINNET',
          bscAddress: form!.bscAddress.trim(),
          bscNetwork: form!.bscNetwork || 'MAINNET',
        }),
      })
      setForm({ ...next, tronApiKey: '', s3SecretKey: '' })
      setSaved(true)
      onError('')
    } catch (e) {
      setSaved(false)
      fail(onError, e)
    }
  }
  const current = chains.find((item) => item.code === tab)
  const testing = tab === 'TRON' ? form.tronNetwork === 'NILE'
    : tab === 'BSC' ? form.bscNetwork === 'TESTNET'
    : tab === 'ETH' ? current?.network === 'TESTNET'
    : false
  const guide = testing ? PAY_GUIDE[tab].test || PAY_GUIDE[tab].main : PAY_GUIDE[tab].main
  const address = tab === 'TRON' ? form.usdtAddress : tab === 'BSC' ? form.bscAddress : current?.address || ''
  const placeholder = tab === 'TRON' ? 'T 开头的 TRC20 地址'
    : tab === 'SOLANA' ? 'Solana 地址'
    : tab === 'TON' ? 'EQ 或 UQ 开头的地址'
    : '0x 开头的地址'
  function setTesting(next: boolean) {
    if (tab === 'TRON') patch({ tronNetwork: next ? 'NILE' : 'MAINNET' })
    else if (tab === 'BSC') patch({ bscNetwork: next ? 'TESTNET' : 'MAINNET' })
    else if (tab === 'ETH') patchChain('ETH', { network: next ? 'TESTNET' : 'MAINNET' })
  }
  function setAddress(value: string) {
    if (tab === 'TRON') patch({ usdtAddress: value })
    else if (tab === 'BSC') patch({ bscAddress: value })
    else patchChain(tab, { address: value })
  }
  return (
    <form className="form" onSubmit={save}>
      <h1>收款方式</h1>
      <p className="hint">一次只设置一条链。填了收款地址才会出现在购买页，地址留空就不会出现。</p>
      <div className="chips">
        {PAY_TABS.map((item) => (
          <button key={item.id} className={tab === item.id ? 'chip active' : 'chip'} type="button" onClick={() => setTab(item.id)}>{item.label}</button>
        ))}
      </div>
      <label>网络
        <select value={testing ? 'TESTNET' : 'MAINNET'} onChange={(e) => setTesting(e.target.value === 'TESTNET')}>
          <option value="MAINNET">主网</option>
          {PAY_GUIDE[tab].test && <option value="TESTNET">测试网</option>}
        </select>
      </label>
      <label>收款地址
        <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder={placeholder} spellCheck={false} />
      </label>
      {tab === 'TRON' && (
        <label>TronGrid API Key
          <input value={form.tronApiKey} onChange={(e) => patch({ tronApiKey: e.target.value })} placeholder={form.tronApiKeySet ? '已配置，留空则保留' : '用来核对链上转账，可留空'} spellCheck={false} />
        </label>
      )}
      <div className="pay-guide">
        <strong>{testing ? '测试网接入' : '主网接入'}</strong>
        <ol>
          {guide.map((step) => <li key={step}>{guideText(step)}</li>)}
        </ol>
      </div>
      <div className="row-actions">
        <button className="btn" type="submit">保存</button>
        {saved ? <span className="hint">已保存</span> : null}
      </div>
    </form>
  )
}

function LegalAdmin({ onError }: { onError: (message: string) => void }) {
  const [privacy, setPrivacy] = useState(() => contentNames())
  const [terms, setTerms] = useState(() => contentNames())
  const [saved, setSaved] = useState(false)
  const [formError, setFormError] = useState('')
  useEffect(() => {
    api<{ privacy: Record<string, string>; terms: Record<string, string> }>('/api/v1/admin/legal')
      .then((next) => {
        setPrivacy(contentNames(next.privacy))
        setTerms(contentNames(next.terms))
      })
      .catch((e) => onError(e.message))
  }, [])
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!primaryName(privacy) || !primaryName(terms)) {
      setFormError('隐私政策和使用条款都至少填写一种语言')
      return
    }
    try {
      const next = await api<{ privacy: Record<string, string>; terms: Record<string, string> }>('/api/v1/admin/legal', {
        method: 'PUT',
        body: JSON.stringify({ privacy, terms }),
      })
      setPrivacy(contentNames(next.privacy))
      setTerms(contentNames(next.terms))
      setSaved(true)
      setFormError('')
      onError('')
    } catch (e) {
      setSaved(false)
      setFormError(e instanceof Error ? e.message : '保存失败')
    }
  }
  return (
    <form className="form legal-form" onSubmit={save}>
      <h1>法律协议</h1>
      {formError ? <p className="error">{formError}</p> : null}
      <p className="hint">页脚「法律」里的隐私政策和使用条款，按访问语言显示。</p>
      <LocaleFields label="隐私政策" required value={privacy} onChange={(next) => { setSaved(false); setPrivacy(next) }} rows={6} maxLength={8000} hint="" />
      <LocaleFields label="使用条款" required value={terms} onChange={(next) => { setSaved(false); setTerms(next) }} rows={6} maxLength={8000} hint="" />
      <div className="row-actions">
        <button className="btn" type="submit">保存</button>
        {saved ? <span className="hint">已保存</span> : null}
      </div>
    </form>
  )
}

function SettingsAdmin({ onError }: { onError: (message: string) => void }) {
  const [form, setForm] = useState<PlatformForm>({
    storageMode: 'LOCAL',
    deliveryMode: 'ORIGIN',
    originBase: 'http://localhost:8080',
    cdnBase: '',
    objectConfigured: false,
    usdtAddress: '',
    paymentSimulate: true,
    tronApiKeySet: false,
    tronApiKey: '',
    tronNetwork: 'MAINNET',
    bscAddress: '',
    bscNetwork: 'MAINNET',
    siteName: 'XTV',
    s3Endpoint: '',
    s3Region: 'us-east-1',
    s3Bucket: '',
    s3AccessKey: '',
    s3SecretKeySet: false,
    s3SecretKey: '',
  })
  const [saved, setSaved] = useState(false)
  useEffect(() => {
    api<PlatformForm>('/api/v1/admin/platform-config')
      .then((next) => setForm({ ...next, tronApiKey: '', s3SecretKey: '' }))
      .catch((e) => onError(e.message))
  }, [])
  function patch(next: Partial<PlatformForm>) {
    setSaved(false)
    setForm({ ...form, ...next })
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    if (form.storageMode === 'OBJECT' && (!form.s3Endpoint.trim() || !form.s3Bucket.trim() || !form.s3AccessKey.trim() || (!form.s3SecretKey.trim() && !form.s3SecretKeySet))) {
      onError('请填写对象存储的地址、桶、Access Key 和 Secret Key')
      return
    }
    if (form.deliveryMode === 'CDN' && !form.cdnBase.trim()) {
      onError('选择 CDN 后需要填写 CDN 地址')
      return
    }
    try {
      const next = await api<PlatformForm>('/api/v1/admin/platform-config', {
        method: 'PUT',
        body: JSON.stringify({
          storageMode: form.storageMode,
          deliveryMode: form.deliveryMode,
          originBase: form.originBase,
          cdnBase: form.cdnBase,
          siteName: form.siteName.trim(),
          s3Endpoint: form.s3Endpoint.trim(),
          s3Region: form.s3Region.trim() || 'us-east-1',
          s3Bucket: form.s3Bucket.trim(),
          s3AccessKey: form.s3AccessKey.trim(),
          s3SecretKey: form.s3SecretKey.trim() || null,
        }),
      })
      setForm({ ...next, tronApiKey: '', s3SecretKey: '' })
      setSaved(true)
      onError('')
      window.dispatchEvent(new Event('xtv-site'))
    } catch (e) {
      setSaved(false)
      fail(onError, e)
    }
  }
  return (
    <form className="form" onSubmit={save}>
      <h1>平台配置</h1>
      <label>站点名称<span className="req">必填</span>
        <input value={form.siteName} onChange={(e) => patch({ siteName: e.target.value })} placeholder="XTV" maxLength={32} required />
      </label>
      <label>存储
        <select value={form.storageMode} onChange={(e) => patch({ storageMode: e.target.value })}>
          <option value="LOCAL">本地磁盘</option>
          <option value="OBJECT">对象存储</option>
        </select>
      </label>
      {form.storageMode === 'OBJECT' && (
        <>
          <label>存储地址
            <input value={form.s3Endpoint} onChange={(e) => patch({ s3Endpoint: e.target.value })} placeholder="https://" spellCheck={false} />
          </label>
          <label>区域
            <input value={form.s3Region} onChange={(e) => patch({ s3Region: e.target.value })} placeholder="us-east-1" spellCheck={false} />
          </label>
          <label>桶
            <input value={form.s3Bucket} onChange={(e) => patch({ s3Bucket: e.target.value })} spellCheck={false} />
          </label>
          <label>Access Key
            <input value={form.s3AccessKey} onChange={(e) => patch({ s3AccessKey: e.target.value })} spellCheck={false} />
          </label>
          <label>Secret Key
            <input value={form.s3SecretKey} onChange={(e) => patch({ s3SecretKey: e.target.value })} placeholder={form.s3SecretKeySet ? '已配置，留空则保留' : ''} spellCheck={false} />
          </label>
        </>
      )}
      <label>分发
        <select value={form.deliveryMode} onChange={(e) => patch({ deliveryMode: e.target.value })}>
          <option value="ORIGIN">源站</option>
          <option value="CDN">CDN</option>
        </select>
      </label>
      <label>源站地址
        <input value={form.originBase} onChange={(e) => patch({ originBase: e.target.value })} placeholder="源站地址" />
      </label>
      <label>CDN 地址
        <input value={form.cdnBase} onChange={(e) => patch({ cdnBase: e.target.value })} placeholder="选择 CDN 时必填" />
      </label>
      <div className="row-actions">
        <button className="btn" type="submit">保存</button>
        {saved ? <span className="hint">已保存</span> : null}
      </div>
    </form>
  )
}
