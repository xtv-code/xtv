import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AccountPage, FavoritesPage, InstallPage, LoginPage, MembershipPage, OrderPage, PrivacyPage, ReportPage, TermsPage } from './Account'
import { AdminPage } from './Admin'
import { CategoriesPage, CategoryVideosPage, HomePage, LatestPage, PeoplePage, PersonVideosPage, RankingPage, SearchPage, SeriesDetailPage, SeriesListPage, TagVideosPage, TagsPage } from './Browse'
import { Layout } from './Layout'
import { VideoPage } from './VideoPage'

function RegisterRedirect() {
  const { search } = useLocation()
  return <Navigate to={`/login${search}`} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="latest" element={<LatestPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="categories/:slug" element={<CategoryVideosPage />} />
          <Route path="tags" element={<TagsPage />} />
          <Route path="tags/:slug" element={<TagVideosPage />} />
          <Route path="people" element={<PeoplePage />} />
          <Route path="people/:slug" element={<PersonVideosPage />} />
          <Route path="series" element={<SeriesListPage />} />
          <Route path="series/:slug" element={<SeriesDetailPage />} />
          <Route path="ranking" element={<RankingPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="videos/:id" element={<VideoPage />} />
          <Route path="favorites" element={<FavoritesPage />} />
          <Route path="install" element={<InstallPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterRedirect />} />
          <Route path="account" element={<AccountPage />} />
          <Route path="membership" element={<MembershipPage />} />
          <Route path="orders/:id" element={<OrderPage />} />
          <Route path="privacy" element={<PrivacyPage />} />
          <Route path="terms" element={<TermsPage />} />
          <Route path="report" element={<ReportPage />} />
          <Route path="admin" element={<AdminPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
