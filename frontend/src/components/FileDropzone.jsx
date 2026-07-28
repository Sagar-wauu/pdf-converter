import { useRef, useState } from 'react'

export default function FileDropzone({ file, onFileSelected, accept }) {
  const inputRef = useRef(null)
  const [dragOver, setDragOver] = useState(false)

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    const dropped = e.dataTransfer.files?.[0]
    if (dropped) onFileSelected(dropped)
  }

  return (
    <div
      onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      className={`cursor-pointer rounded-2xl border-2 border-dashed p-10 text-center transition-colors ${
        dragOver ? 'border-seal bg-seal/5' : 'border-line bg-white hover:border-ink2/40'
      }`}
    >
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        onChange={(e) => e.target.files?.[0] && onFileSelected(e.target.files[0])}
      />
      {file ? (
        <div>
          <p className="font-medium text-ink">{file.name}</p>
          <p className="text-sm text-ink2 mt-1">{(file.size / 1024 / 1024).toFixed(2)} MB — click to choose a different file</p>
        </div>
      ) : (
        <div>
          <svg className="mx-auto mb-3 text-ink2" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
            <path d="M12 16V4M12 4l-4 4M12 4l4 4" />
            <path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3" />
          </svg>
          <p className="font-medium text-ink">Drag & drop your file here</p>
          <p className="text-sm text-ink2 mt-1">or click to browse from your computer</p>
        </div>
      )}
    </div>
  )
}
