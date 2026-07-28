/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        paper: '#FAF8F3',
        ink: '#1E2A45',
        ink2: '#3A4A6B',
        stamp: '#B23A2E',
        seal: '#0F6B5C',
        line: '#E4DFD3'
      },
      fontFamily: {
        display: ['"Source Serif 4"', 'Georgia', 'serif'],
        body: ['Inter', 'system-ui', 'sans-serif']
      },
      boxShadow: {
        card: '0 1px 2px rgba(30,42,69,0.06), 0 8px 24px -12px rgba(30,42,69,0.15)'
      }
    },
  },
  plugins: [],
}
