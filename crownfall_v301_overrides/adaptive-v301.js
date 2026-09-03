(() => {
  'use strict';

  const root = document.documentElement;
  const body = document.body;
  let resizeTimer = 0;
  let lastKey = '';

  function viewportSize() {
    const vv = window.visualViewport;
    const width = Math.max(1, Math.round(vv?.width || window.innerWidth || root.clientWidth || 1));
    const height = Math.max(1, Math.round(vv?.height || window.innerHeight || root.clientHeight || 1));
    return { width, height };
  }

  function classify() {
    const { width, height } = viewportSize();
    const shortSide = Math.min(width, height);
    const longSide = Math.max(width, height);
    const ratio = longSide / shortSide;
    const landscape = width >= height;

    /* Fold5 unfolded WebView is near-square, while a normal phone is usually >1.7:1. */
    const foldOpen = shortSide >= 600 && ratio <= 1.58;
    const large = shortSide >= 600 || (width >= 1100 && height >= 650);
    const compact = shortSide < 470 || (landscape && height < 520);
    const wide = landscape && ratio >= 1.72 && width >= 1000;

    const key = [width, height, foldOpen, large, compact, wide, landscape].join(':');
    if (key === lastKey) return;
    lastKey = key;

    body.classList.toggle('viewport-landscape', landscape);
    body.classList.toggle('viewport-portrait', !landscape);
    body.classList.toggle('viewport-fold-open', foldOpen);
    body.classList.toggle('viewport-large', large);
    body.classList.toggle('viewport-compact', compact);
    body.classList.toggle('viewport-wide', wide);
    body.dataset.viewportMode = foldOpen ? 'fold-open' : large ? 'large' : compact ? 'compact' : 'phone';

    root.style.setProperty('--viewport-w', `${width}px`);
    root.style.setProperty('--viewport-h', `${height}px`);
    root.style.setProperty('--adaptive-unit', `${Math.max(.7, Math.min(1.35, shortSide / 720)).toFixed(3)}px`);

    const canvas = document.getElementById('gameCanvas');
    const uiCanvas = document.getElementById('uiCanvas');
    for (const node of [canvas, uiCanvas]) {
      if (!node) continue;
      node.style.width = `${width}px`;
      node.style.height = `${height}px`;
    }

    window.dispatchEvent(new CustomEvent('crownfallviewportchange', {
      detail: { width, height, shortSide, longSide, ratio, landscape, foldOpen, large, compact, wide }
    }));
  }

  function schedule() {
    clearTimeout(resizeTimer);
    resizeTimer = window.setTimeout(() => {
      classify();
      requestAnimationFrame(classify);
      window.setTimeout(classify, 240);
    }, 40);
  }

  window.CrownfallViewport = {
    refresh: schedule,
    get mode() { return body.dataset.viewportMode || 'unknown'; }
  };

  window.addEventListener('resize', schedule, { passive: true });
  window.addEventListener('orientationchange', schedule, { passive: true });
  window.addEventListener('pageshow', schedule, { passive: true });
  window.visualViewport?.addEventListener('resize', schedule, { passive: true });
  window.visualViewport?.addEventListener('scroll', schedule, { passive: true });

  if ('ResizeObserver' in window) {
    new ResizeObserver(schedule).observe(root);
  }

  classify();
  document.addEventListener('DOMContentLoaded', schedule, { once: true });
})();
