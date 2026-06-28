
// Stylesheets
import './main.scss';

// Auto-import all JS/TS modules in site/ and components/ directories
// Replaces webpack glob-import-loader with Vite's import.meta.glob
const siteModules = import.meta.glob('./**/*.js', { eager: true });
const siteTsModules = import.meta.glob('./**/*.ts', { eager: true });
const componentModules = import.meta.glob('../components/**/*.js', { eager: true });

// Vue mount scripts — mỗi file tự mount Vue app vào DOM element tương ứng
const vueMounts = import.meta.glob('../components/**/*.mount.ts', { eager: true });


// Eagerly load all component and page SCSS partials
const componentStyles = import.meta.glob('../components/**/_*.scss', { eager: true });
const pageStyles = import.meta.glob('./styles/*.scss', { eager: true });
