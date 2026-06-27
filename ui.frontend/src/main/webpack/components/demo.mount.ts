import { createApp } from 'vue';
import Demo from './demo.vue';

/**
 * Mount Vue app vào element có data-cmp-is="ms-demo".
 * Props được đọc từ data-* attributes (do HTL render).
 */
export function mountDemo() {
  const elements = document.querySelectorAll('[data-cmp-is="ms-demo"]');

  elements.forEach((el) => {
    const root = el as HTMLElement;

    // Đọc props từ data attributes
    const name = root.dataset.name || 'Nguyen Van A';

    const app = createApp(Demo, { name });
    app.mount(root);
  });
}

// Auto-mount khi script load + DOM sẵn sàng
if (document.readyState !== 'loading') {
  mountDemo();
} else {
  document.addEventListener('DOMContentLoaded', mountDemo);
}
