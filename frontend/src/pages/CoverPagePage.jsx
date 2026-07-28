import { useState } from 'react'
import CoverPagePreview from '../components/CoverPagePreview.jsx'
import { generateCoverPage, saveCoverPage } from '../api/api.js'

const DEFAULTS = {
  templateName: '',
  universityName: 'Tribhuvan University',
  facultyName: 'Faculty of Humanities and Social Science',
  reportLabel: 'LAB REPORT ON',
  subjectName: '',
  submittedToLabel: 'Submitted to',
  departmentName: 'Department of Computer Application',
  campusName: '',
  campusAddress: '',
  fulfillmentLine: 'In partial fulfillment of the requirements for the Bachelors in Computer Application',
  studentName: '',
  registrationNo: '',
  semester: '',
  internalExaminerName: '',
  externalExaminerName: '', // Optional field
  internalExaminerLabel: 'Internal Examiner',
  externalExaminerLabel: 'External Examiner',
  subjectColor: '#B23A2E',
  campusNameColor: '#2E75B6',
  logoBase64: '',
  logoPreview: '',
}

const FIELD_GROUPS = [
  {
    title: 'Institution',
    fields: [
      ['universityName', 'University name'],
      ['facultyName', 'Faculty name'],
      ['reportLabel', 'Report label (e.g. LAB REPORT ON)'],
      ['subjectName', 'Subject / course name'],
    ],
  },
  {
    title: 'Submitted to',
    fields: [
      ['submittedToLabel', '"Submitted to" text'],
      ['departmentName', 'Department name'],
      ['campusName', 'Campus name'],
      ['campusAddress', 'Campus address'],
    ],
  },
  {
    title: 'Your details',
    fields: [
      ['studentName', 'Your full name'],
      ['registrationNo', 'Registration number'],
      ['semester', 'Semester'],
    ],
  },
  {
    title: 'Examiners',
    fields: [
      ['internalExaminerName', 'Internal examiner name'],
      ['externalExaminerName', 'External examiner name (Optional)'],
      ['internalExaminerLabel', 'Internal examiner title'],
      ['externalExaminerLabel', 'External examiner title'],
    ],
  },
]

export default function CoverPagePage() {
  const [data, setData] = useState(DEFAULTS)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  const update = (key, value) => setData((d) => ({ ...d, [key]: value }))

  const handleLogoUpload = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      setData((d) => ({ ...d, logoBase64: reader.result, logoPreview: reader.result }))
    }
    reader.readAsDataURL(file)
  }

  const handleGenerate = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await generateCoverPage(data)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${data.templateName || 'cover-page'}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (e) {
      setError('Could not generate the PDF. Please check that the backend is running and try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    setError('')
    try {
      await saveCoverPage(data)
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (e) {
      setError('Could not save this cover page. Please check that the backend is running.')
    }
  }

  return (
      <div>
        <h1 className="font-display text-3xl font-semibold text-ink mb-2">Build Your Cover Page</h1>
        <p className="text-ink2 mb-8">Fill in the fields below — the preview on the right updates as you type.</p>

        <div className="grid lg:grid-cols-[1.1fr_0.9fr] gap-10 items-start">
          <div className="space-y-8">
            <div>
              <label className="block text-sm font-medium text-ink mb-1.5">Save this as (optional name)</label>
              <input
                  className="w-full rounded-lg border border-line px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-seal/40"
                  placeholder="e.g. Cloud Computing Lab Report"
                  value={data.templateName}
                  onChange={(e) => update('templateName', e.target.value)}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-ink mb-1.5">Logo (optional)</label>
              <input type="file" accept="image/*" onChange={handleLogoUpload} className="text-sm" />
            </div>

            {FIELD_GROUPS.map((group) => (
                <div key={group.title}>
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-ink2 mb-3">{group.title}</h3>
                  <div className="grid sm:grid-cols-2 gap-4">
                    {group.fields.map(([key, label]) => (
                        <div key={key} className={label.length > 22 ? 'sm:col-span-2' : ''}>
                          <label className="block text-sm text-ink2 mb-1.5">{label}</label>
                          <input
                              className="w-full rounded-lg border border-line px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-seal/40"
                              value={data[key]}
                              onChange={(e) => update(key, e.target.value)}
                          />
                        </div>
                    ))}
                  </div>
                </div>
            ))}

            <div>
              <label className="block text-sm text-ink2 mb-1.5">Fulfillment statement</label>
              <textarea
                  rows={2}
                  className="w-full rounded-lg border border-line px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-seal/40"
                  value={data.fulfillmentLine}
                  onChange={(e) => update('fulfillmentLine', e.target.value)}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-ink2 mb-1.5">Subject text color</label>
                <input type="color" value={data.subjectColor} onChange={(e) => update('subjectColor', e.target.value)} className="w-full h-10 rounded-lg border border-line" />
              </div>
              <div>
                <label className="block text-sm text-ink2 mb-1.5">Campus text color</label>
                <input type="color" value={data.campusNameColor} onChange={(e) => update('campusNameColor', e.target.value)} className="w-full h-10 rounded-lg border border-line" />
              </div>
            </div>

            {error && (
                <div className="text-sm text-stamp bg-stamp/5 border border-stamp/20 rounded-lg px-4 py-3">{error}</div>
            )}
            {saved && (
                <div className="text-sm text-seal bg-seal/5 border border-seal/20 rounded-lg px-4 py-3">Saved! You can reopen it any time from your history.</div>
            )}

            <div className="flex gap-3">
              <button
                  onClick={handleGenerate}
                  disabled={loading}
                  className="flex-1 bg-ink text-paper font-medium rounded-xl py-3.5 hover:bg-ink2 transition-colors disabled:opacity-50"
              >
                {loading ? 'Creating PDF…' : 'Download as PDF'}
              </button>
              <button
                  onClick={handleSave}
                  className="px-5 rounded-xl border border-line font-medium text-ink hover:bg-white transition-colors"
              >
                Save
              </button>
            </div>
          </div>

          <div className="lg:sticky lg:top-24">
            <CoverPagePreview data={data} />
          </div>
        </div>
      </div>
  )
}