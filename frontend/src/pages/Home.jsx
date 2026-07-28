import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div>
      <section className="text-center max-w-2xl mx-auto mb-14">
        <span className="inline-block text-xs tracking-widest uppercase text-seal font-semibold mb-3">
          Simple document tools
        </span>
        <h1 className="font-display text-4xl md:text-5xl font-semibold text-ink leading-tight mb-4">
          Convert files & build cover pages, without the headache
        </h1>
        <p className="text-ink2 text-base leading-relaxed">
          Turn a PDF into a Word or PowerPoint file (or the other way round), and design a
          university-style cover page in a few clicks — no design skill or technical
          knowledge required.
        </p>
      </section>

      <div className="grid md:grid-cols-2 gap-6">
        <Link
          to="/convert"
          className="group relative bg-white border border-line rounded-2xl p-8 shadow-card hover:-translate-y-0.5 transition-transform"
        >
          <div className="w-12 h-12 rounded-xl bg-seal/10 text-seal flex items-center justify-center mb-5">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" />
              <path d="M14 3v5h5" />
              <path d="M9 13l2.5 2.5L15 12" />
            </svg>
          </div>
          <h2 className="font-display text-xl font-semibold mb-2">Convert a File</h2>
          <p className="text-sm text-ink2 leading-relaxed mb-4">
            PDF ⇄ Word, and PDF ⇄ PowerPoint. Upload a file, pick what you want it turned
            into, and download the result — that's it.
          </p>
          <span className="text-sm font-medium text-seal group-hover:underline">
            Start converting →
          </span>
        </Link>

        <Link
          to="/cover-page"
          className="group relative bg-white border border-line rounded-2xl p-8 shadow-card hover:-translate-y-0.5 transition-transform"
        >
          <div className="w-12 h-12 rounded-xl bg-stamp/10 text-stamp flex items-center justify-center mb-5">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <rect x="4" y="3" width="16" height="18" rx="1.5" />
              <path d="M8 8h8M8 12h8M8 16h4" />
            </svg>
          </div>
          <h2 className="font-display text-xl font-semibold mb-2">Build a Cover Page</h2>
          <p className="text-sm text-ink2 leading-relaxed mb-4">
            Fill in your university, subject, name and examiners, and get a ready-to-print
            front page PDF — styled after a real lab report cover page.
          </p>
          <span className="text-sm font-medium text-stamp group-hover:underline">
            Design your cover page →
          </span>
        </Link>
      </div>
    </div>
  )
}
