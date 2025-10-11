function renderSummary(summary) {
  const container = document.getElementById('summary');
  if (!container || !summary) return;

  const nf = new Intl.NumberFormat('pt-BR');

  container.querySelectorAll('.card').forEach(card => {
    const key = card.dataset.key;
    const valueEl = card.querySelector('.value');
    if (!valueEl) return;
    const val = Number(summary[key] ?? 0);
    valueEl.textContent = nf.format(val);
  });

  const last = document.getElementById('lastUpdated');
  if (last) {
    last.textContent = `Updated at ${new Date().toLocaleString('pt-BR')}`;
  } else {
    console.warn('Element #lastUpdated not found in DOM');
  }
}