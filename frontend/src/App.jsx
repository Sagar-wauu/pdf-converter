import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Home from './pages/Home.jsx'
import ConvertPage from './pages/ConvertPage.jsx'
import CoverPagePage from './pages/CoverPagePage.jsx'

export default function App() {
  return (
    <div className="min-h-screen bg-paper text-ink font-body">
      <Navbar />
      <main className="max-w-5xl mx-auto px-6 py-10">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/convert" element={<ConvertPage />} />
          <Route path="/cover-page" element={<CoverPagePage />} />
        </Routes>
      </main>
      <footer className="text-center text-xs text-ink2/70 py-8">
        Made by Sagar Bhattarai
      </footer>
    </div>
  )
}
