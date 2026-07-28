import { NavLink } from 'react-router-dom'

const linkClass = ({ isActive }) =>
  `px-4 py-2 rounded-full text-sm font-medium transition-colors ${
    isActive ? 'bg-ink text-paper' : 'text-ink2 hover:bg-line/60'
  }`

export default function Navbar() {
  return (
    <header className="border-b border-line bg-paper/95 backdrop-blur sticky top-0 z-20">
      <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
        <NavLink to="/" className="flex items-center gap-2 group">
          <span className="w-8 h-8 rounded-full bg-seal text-paper flex items-center justify-center font-display font-semibold text-sm">
            SB
          </span>
          <span className="font-display text-lg font-semibold text-ink">PDF Converter</span>
        </NavLink>
        <nav className="flex items-center gap-2">
          <NavLink to="/" end className={linkClass}>Home</NavLink>
          <NavLink to="/convert" className={linkClass}>Convert Files</NavLink>
          <NavLink to="/cover-page" className={linkClass}>Cover Page</NavLink>
        </nav>
      </div>
    </header>
  )
}
