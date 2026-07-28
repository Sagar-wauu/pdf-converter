import { useState } from 'react'
import FileDropzone from '../components/FileDropzone.jsx'
import { convertFile } from '../api/api.js'

const OPTIONS = [
  { type: 'PDF_TO_WORD', label: 'PDF → Word', desc: 'Turn a .pdf into an editable .docx', accept: '.pdf' },
  { type: 'WORD_TO_PDF', label: 'Word → PDF', desc: 'Turn a .docx into a .pdf', accept: '.doc,.docx' },
  { type: 'PDF_TO_PPT', label: 'PDF → PowerPoint', desc: 'Turn a .pdf into a .pptx slide deck', accept: '.pdf' },
  { type: 'PPT_TO_PDF', label: 'PowerPoint → PDF', desc: 'Turn a .pptx into a .pdf', accept: '.ppt,.pptx' },
]

export default function ConvertPage() {
  const [selected, setSelected] = useState(OPTIONS[0])
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const handleSelect = (opt) => {
    setSelected(opt)
    setFile(null)
    setError('')
    setSuccess(false)
  }

  const handleConvert = async () => {
    if (!file) {
      setError('Please choose a file first.')
      return
    }
    setLoading(true)
    setError('')
    setSuccess(false)
    try {
      const response = await convertFile(file, selected.type)
      const blob = new Blob([response.data])
      const url = window.URL.createObjectURL(blob)
      const disposition = response.headers['content-disposition']
      let fileName = 'converted-file'
      if (disposition) {
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match) fileName = match[1]
      }
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      setSuccess(true)
    } catch (e) {
      let message = 'Could not reach the server. Please check that the backend is running.'

      if (e.response?.data) {
        try {
          const payload = e.response.data instanceof Blob ? JSON.parse(await e.response.data.text()) : e.response.data
          message = payload?.error || payload?.message || message
        } catch {
          message = 'Something went wrong while converting your file. Please make sure it is a valid file and try again.'
        }
      }

      setError(
        message
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="font-display text-3xl font-semibold text-ink mb-2">Convert a File</h1>
      <p className="text-ink2 mb-8">Pick what kind of conversion you need, upload your file, then download the result.</p>

      <div className="grid grid-cols-2 gap-3 mb-8">
        {OPTIONS.map((opt) => (
          <button
            key={opt.type}
            onClick={() => handleSelect(opt)}
            className={`text-left rounded-xl border p-4 transition-colors ${
              selected.type === opt.type
                ? 'border-seal bg-seal/5 ring-1 ring-seal'
                : 'border-line bg-white hover:border-ink2/40'
            }`}
          >
            <p className="font-medium text-ink">{opt.label}</p>
            <p className="text-xs text-ink2 mt-1">{opt.desc}</p>
          </button>
        ))}
      </div>

      <FileDropzone file={file} accept={selected.accept} onFileSelected={(f) => { setFile(f); setSuccess(false); setError('') }} />

      {error && (
        <div className="mt-4 text-sm text-stamp bg-stamp/5 border border-stamp/20 rounded-lg px-4 py-3">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 text-sm text-seal bg-seal/5 border border-seal/20 rounded-lg px-4 py-3">
          Done! Your converted file has been downloaded.
        </div>
      )}

      <button
        onClick={handleConvert}
        disabled={loading}
        className="mt-6 w-full bg-ink text-paper font-medium rounded-xl py-3.5 hover:bg-ink2 transition-colors disabled:opacity-50"
      >
        {loading ? 'Converting…' : `Convert to ${selected.label.split('→')[1].trim()}`}
      </button>

      <p className="text-xs text-ink2/70 mt-4 text-center">
        Your file is only used to create the converted version and isn't shared anywhere.
      </p>
    </div>
  )
}
