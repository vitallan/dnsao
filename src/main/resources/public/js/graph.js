function buildChart(stats, canvasId = 'queriesChart') {

  const temporal = stats?.temporal;
  if (!temporal || !Array.isArray(temporal.columns) || !Array.isArray(temporal.rows)) {
    console.error('invalid format for temporal data');
    return;
  }

  const colIndex = Object.fromEntries(temporal.columns.map((name, i) => [name, i]));

  const labels = temporal.rows.map(r => r[colIndex.ts]);

  const metricCols = temporal.columns.filter(c => c !== 'ts');

  const palette = [
    '#2563eb', '#10b981', '#ef4444', '#14b8a6', '#8b5cf6', '#f59e0b', '#e11d48'
  ];

  const labelAlias = {
    block: 'Blocked',
    cache: 'Cache',
    local: 'Local',
    upstream: 'Upstream',
    refused: 'Refused',
    servfail: 'Servfail',
    total: 'Total'
  };

  const datasets = metricCols.map((col, i) => {
    const data = temporal.rows.map(r => Number(r[colIndex[col]]) || 0);
    return {
      label: labelAlias[col] ?? (col.charAt(0).toUpperCase() + col.slice(1)),
      data,
      borderColor: palette[i % palette.length],
      backgroundColor: palette[i % palette.length],
      borderWidth: 2,
      pointRadius: 0,
      tension: 0.15,
      fill: false
    };
  });

  const ctx = document.getElementById(canvasId)?.getContext('2d');
  if (!ctx) {
    console.error(`Canvas #${canvasId} not found`);
    return;
  }

  if (window.__dnsaoChart) {
    window.__dnsaoChart.destroy();
  }

  window.__dnsaoChart = new Chart(ctx, {
    type: 'line',
    data: { labels, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'nearest', intersect: false, axis: 'x' },
      plugins: {
        legend: { position: 'top' },
        title: { display: true, text: 'DNSao – Queries by interval' },
        tooltip: { callbacks: { label: (ctx) => `${ctx.dataset.label}: ${ctx.parsed.y}` } }
      },
      scales: {
        x: { title: { display: true, text: 'Timestamp' }, offset: true, ticks: { autoSkip: true } },
        y: { beginAtZero: true, title: { display: true, text: 'Count' } }
      },
      layout: { padding: { right: 8 } }
    }
  });
}

function buildUpstreamPie(stats, canvasId = 'upstreamPie', maxSlices = 8) {
  const upstream = stats?.upstream || {};
  const entries = Object.entries(upstream).filter(([, v]) => Number(v) > 0);

  const wrap = document.getElementById(canvasId)?.closest('#' + (document.getElementById(canvasId)?.parentElement?.id || ''));
  if (!document.getElementById(canvasId)) {
    console.warn(`Canvas #${canvasId} not found`);
    return;
  }

  if (entries.length === 0) {
    const el = document.getElementById(canvasId);
    if (el && el.getContext) {
      const ctx = el.getContext('2d');
      ctx.clearRect(0, 0, el.width, el.height);
    }
    return;
  }

  entries.sort((a, b) => b[1] - a[1]);

  let top = entries;
  if (entries.length > maxSlices) {
    const visible = entries.slice(0, maxSlices - 1);
    const restSum = entries.slice(maxSlices - 1).reduce((acc, [, v]) => acc + Number(v), 0);
    top = [...visible, ['Outros', restSum]];
  }

  const labels = top.map(([k]) => k);
  const data = top.map(([, v]) => Number(v) || 0);

  const colors = labels.map((_, i) => `hsl(${Math.round((360 / labels.length) * i)}, 70%, 55%)`);

  const ctx = document.getElementById(canvasId).getContext('2d');

  if (window.__dnsaoUpstreamPie) {
    window.__dnsaoUpstreamPie.destroy();
  }

  window.__dnsaoUpstreamPie = new Chart(ctx, {
    type: 'pie',
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: colors,
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'right' },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
              const val = Number(ctx.parsed) || 0;
              const pct = total ? ((val / total) * 100).toFixed(1) : '0.0';
              return `${ctx.label}: ${val} (${pct}%)`;
            }
          }
        }
      }
    }
  });
}