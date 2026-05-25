/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        serif: ['Georgia', '"Times New Roman"', 'Times', 'serif'],
        masthead: ['"Playfair Display"', 'Georgia', 'serif'],
        mono: ['"Courier New"', 'Courier', 'monospace'],
      },
      colors: {
        // Light newsprint
        paper: "#faf9f6",
        ink: "#1a1a1a",
        newsbody: "#444444",
        newsmuted: "#888888",
        newsfaint: "#999999",
        newsrule: "#d4cfc7",
        newsedge: "#e2ddd5",
        // Dark newsprint
        "paper-dark": "#16140f",
        "card-dark": "#211e18",
        "edge-dark": "#3a342b",
        "text-dark": "#e8e2d6",
        "muted-dark": "#9a9388",
      },
    },
  },
  plugins: [],
}
