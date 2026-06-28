import { createApp } from "vue";
import One from './OneComponent.vue';
/**
 * Mount Vue app vào element có data-cmp-is="ms-demo".
 * Props được đọc từ data-* attributes (do HTL render).
 */
export function mountOne() {
  const elements = document.querySelectorAll('[data-cmp-is="ms-one"]');

  elements.forEach((el) => {
    const root = el as HTMLElement;

    // Đọc props từ data attributes
    const value1   = root.dataset.value1 || 'Nguyen Van A';

    const app = createApp(One, { value1 });
    app.mount(root);
  });
}

// Auto-mount khi script load + DOM sẵn sàng
if (document.readyState !== 'loading') {
  mountOne();
} else {
  document.addEventListener('DOMContentLoaded', mountOne);
}
