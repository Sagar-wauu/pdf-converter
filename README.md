# PDF Converter

A simple tool with two jobs:

1. **Convert files** — PDF ⇄ Word (.docx), PDF ⇄ PowerPoint (.pptx)
2. **Build a cover page** — fill in a form (university, subject, your name, examiners...)
   and get a ready-to-print front page PDF, styled after a real lab-report cover page.

Built with:
- **Backend:** Java (Spring Boot)
- **Database:** PostgreSQL
- **Frontend:** React + Tailwind CSS

---

## What you need installed first

You only need to do this once.

| Tool | Why | Download |
|---|---|---|
| **Java 25+** | runs the backend | https://adoptium.net |
| **Maven** | builds the backend | https://maven.apache.org/download.cgi |
| **Node.js 18+** | runs the frontend | https://nodejs.org |
| **PostgreSQL** | stores conversion history & saved cover pages | https://www.postgresql.org/download |
| **LibreOffice** | does the actual Word/PowerPoint ⇄ PDF conversion | https://www.libreoffice.org/download |

LibreOffice is what actually performs the Word/PowerPoint/PDF conversion behind the
scenes — the same engine many conversion websites use. It needs to be installed on
whatever computer runs the backend, and it must be reachable from the command line as
`soffice` (see the troubleshooting section if it isn't).

---

## Step 1 — Create the database

Open a terminal and run:

```bash
psql -U postgres
```

Then inside the `psql` prompt:

```sql
CREATE DATABASE pdf_converter_db;
\q
```

That's it — the app will automatically create its tables the first time it starts.

If your Postgres username/password isn't `postgres` / `postgres`, open
`backend/src/main/resources/application.properties` and update these three lines:

```properties
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## Step 2 — Start the backend

```bash
cd backend
mvn spring-boot:run
```

Wait until you see `Started PdfConverterApplication` in the terminal. The backend is
now running at `http://localhost:8080`.

**If LibreOffice isn't on your PATH**, open `application.properties` and set the full
path to it, for example:

```properties
# Windows
app.libreoffice.path=C:\\Program Files\\LibreOffice\\program\\soffice.exe

# Mac
app.libreoffice.path=/Applications/LibreOffice.app/Contents/MacOS/soffice

# Linux
app.libreoffice.path=/usr/bin/soffice
```

---

## Step 3 — Start the frontend

Open a **new** terminal window (keep the backend running in the first one):

```bash
cd frontend
npm install
npm run dev
```

Then open the link it gives you — normally **http://localhost:5173**.

---

## Using the app

- **Home page** — two big buttons: "Convert a File" and "Build a Cover Page".
- **Convert a File** — pick a direction (e.g. PDF → Word), drag your file in, click
  **Download**. The converted file downloads straight to your computer.
- **Build a Cover Page** — fill in the form on the left; the page preview on the
  right updates as you type. Click **Download as PDF** when it looks right, or
  **Save** to come back to it later.

No technical knowledge is needed to use either feature — everything happens through
plain buttons and forms.

---

## Project structure

```
pdf-converter/
├── backend/     Java Spring Boot API (conversion + cover page generation)
├── frontend/    React + Tailwind CSS interface
└── database/    Reference SQL schema (auto-created by the backend on first run)
```

---

## Troubleshooting

**"Conversion failed" / "LibreOffice conversion failed"**
LibreOffice isn't installed, or `soffice` isn't on your system PATH. Install it from
the link above and/or set `app.libreoffice.path` as shown in Step 2.

**Frontend loads but nothing happens when I click a button**
The backend probably isn't running. Go back to Step 2 and make sure the terminal
shows `Started PdfConverterApplication` with no errors.

**"Could not reach the server"**
Check the backend terminal for errors, and make sure nothing else is using port 8080.

**Database connection errors**
Double check the database name, username and password in
`backend/src/main/resources/application.properties` match what you set up in Step 1.
