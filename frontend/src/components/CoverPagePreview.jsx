export default function CoverPagePreview({ data }) {
    const logoSrc = data.logoPreview || '/tu-logo.png' // permanent default, overridable

    return (
        <div className="bg-white border border-line rounded-2xl shadow-card p-8 aspect-[1/1.35] flex flex-col items-center text-center overflow-hidden">
            <img src={logoSrc} alt="logo" className="w-16 h-16 object-contain mb-4" />

            <p className="font-display font-semibold text-ink text-[15px] leading-snug">{data.universityName}</p>
            <p className="font-display font-semibold text-ink text-[12px] leading-snug mb-6">{data.facultyName}</p>

            <p className="font-semibold text-[12px] text-ink mb-3">{data.reportLabel}</p>
            <p className="font-semibold text-[12px] mb-6" style={{ color: data.subjectColor }}>{data.subjectName || 'Your subject name'}</p>

            <p className="font-semibold text-[11px] text-ink">{data.submittedToLabel}</p>
            <p className="font-semibold text-[11px] text-ink">{data.departmentName}</p>
            <p className="font-semibold text-[11px]" style={{ color: data.campusNameColor }}>{data.campusName || 'Your campus name'}</p>
            <p className="font-semibold text-[11px] text-ink mb-6">{data.campusAddress}</p>

            <p className="italic font-semibold text-[10px] text-ink2 leading-relaxed mb-8 px-4">{data.fulfillmentLine}</p>

            <p className="text-[11px] text-ink mb-1">Submitted by</p>
            <p className="text-[11px] text-ink">Name: {data.studentName || '—'}</p>
            <p className="text-[11px] text-ink">Registration No: {data.registrationNo || '—'}</p>
            <p className="text-[11px] text-ink mb-8">Semester: <span className="font-semibold">{data.semester || '—'}</span></p>

            <div className="mt-auto w-full flex justify-between text-[10px] text-ink pt-4">
                <div className="text-left">
                    <div className="w-28 border-t border-ink mb-1" />
                    <p className="font-semibold">{data.internalExaminerName || '\u00A0'}</p>
                    <p className="font-semibold">{data.internalExaminerLabel}</p>
                </div>

                <div className="text-left">
                    <div className="w-28 border-t border-ink mb-1" />
                    <p className="font-semibold">{data.externalExaminerName || '\u00A0'}</p>
                    <p className="font-semibold">{data.externalExaminerLabel}</p>
                </div>
            </div>
        </div>
    )
}