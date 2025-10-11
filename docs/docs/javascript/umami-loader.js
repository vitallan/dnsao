(function () {
  var s = document.createElement('script');
  s.async = true;
  s.defer = true;
  s.src = 'https://umami.visal.pro/script.js';
  s.dataset.websiteId = 'a1c5d403-97e6-4a7f-b18d-221409f5e82e'; 
  document.head.appendChild(s);

  document.addEventListener('DOMContentLoaded', function () {
    if (typeof location$ !== 'undefined') {
      location$.subscribe(function () {
        if (window.umami && typeof window.umami.track === 'function') {
          window.umami.track('pageview');
        }
      });
    }
  });
})();
