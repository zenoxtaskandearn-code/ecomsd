/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        fetch: {
          lavender: '#D7DDF7',
          'lavender-soft': '#E8EAF8',
          beige: '#D6B894',
          sand: '#E9D8C4',
          cream: '#F7F5F1',
          neutral: '#F5F5F7',
          ink: '#171717',
          muted: '#5D5D5D',
          dark: '#10111D',
        },
      },
      fontFamily: {
        sans: ['"DM Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      container: {
        center: true,
        padding: {
          DEFAULT: '1.25rem',
          sm: '1.5rem',
          lg: '2rem',
        },
        screens: {
          sm: '640px',
          md: '768px',
          lg: '1024px',
          xl: '1200px',
        },
      },
      borderRadius: {
        pill: '9999px',
      },
      letterSpacing: {
        tightish: '-0.015em',
      },
    },
  },
  plugins: [],
};
