// /home/cmngu/Workspace/BootForum2/src/main/ngapp/tailwind.config.js
module.exports = {
  darkMode: 'class',
  content: [
    "./src/**/*.{html,ts}", // This tells Tailwind to scan all .html and .ts files in your src folder
  ],
  theme: {
    extend: {
      // You can extend the default theme here
      // For example:
      // colors: {
      //   'brand-blue': '#1992d4',
      // },
    },
  },
  plugins: [
    require('@tailwindcss/forms'), // If you use this plugin
    require('@tailwindcss/typography'), // For the 'prose' classes, useful for styling HTML from a CMS/Markdown
    require('@tailwindcss/line-clamp'), // For text ellipsis after a certain number of lines
    // Add other Tailwind plugins here
  ],
}
