import fs from "node:fs";
import path from "node:path";

function parseCsvLine(line) {
  // Minimal CSV parser supporting quotes.
  const out = [];
  let cur = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }
    if (ch === "," && !inQuotes) {
      out.push(cur);
      cur = "";
      continue;
    }
    cur += ch;
  }
  out.push(cur);
  return out;
}

function percentile(values, p) {
  if (!values.length) return 0;
  const xs = Array.from(values).sort((a, b) => a - b);
  const k = (xs.length - 1) * p;
  const f = Math.floor(k);
  const c = Math.ceil(k);
  if (f === c) return xs[k | 0];
  const d0 = xs[f] * (c - k);
  const d1 = xs[c] * (k - f);
  return d0 + d1;
}

function movingAverage(values, window) {
  const out = new Array(values.length);
  let sum = 0;
  for (let i = 0; i < values.length; i++) {
    sum += values[i];
    if (i >= window) sum -= values[i - window];
    const denom = Math.min(i + 1, window);
    out[i] = sum / denom;
  }
  return out;
}

function readJtl(jtlPath) {
  const text = fs.readFileSync(jtlPath, "utf8");
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (lines.length < 2) {
    return { rows: [], header: [] };
  }
  const header = parseCsvLine(lines[0]);
  const idx = Object.fromEntries(header.map((h, i) => [h, i]));
  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    const timeStamp = Number(cols[idx.timeStamp] ?? 0);
    const elapsed = Number(cols[idx.elapsed] ?? 0);
    const success = String(cols[idx.success] ?? "").toLowerCase() === "true";
    rows.push({ timeStamp, elapsed, success });
  }
  return { rows, header };
}

function computeStats(rows) {
  const elapsed = rows.map((r) => r.elapsed).filter((n) => Number.isFinite(n));
  const stamps = rows.map((r) => r.timeStamp).filter((n) => Number.isFinite(n) && n > 0);
  const samples = rows.length;
  const ok = rows.reduce((a, r) => a + (r.success ? 1 : 0), 0);
  const errorRate = samples ? (samples - ok) / samples : 0;
  const durationS = stamps.length ? (Math.max(...stamps) - Math.min(...stamps)) / 1000 : 0;
  const throughputRps = durationS > 0 ? samples / durationS : 0;

  elapsed.sort((a, b) => a - b);
  const sum = elapsed.reduce((a, b) => a + b, 0);
  const avg = elapsed.length ? sum / elapsed.length : 0;
  const median = elapsed.length
    ? elapsed.length % 2
      ? elapsed[(elapsed.length - 1) / 2]
      : (elapsed[elapsed.length / 2 - 1] + elapsed[elapsed.length / 2]) / 2
    : 0;

  return {
    samples,
    avgMs: avg,
    medianMs: median,
    p90Ms: percentile(elapsed, 0.9),
    p95Ms: percentile(elapsed, 0.95),
    p99Ms: percentile(elapsed, 0.99),
    minMs: elapsed.length ? elapsed[0] : 0,
    maxMs: elapsed.length ? elapsed[elapsed.length - 1] : 0,
    errorRate,
    throughputRps,
    durationS,
  };
}

function svgChart({ title, elapsed, avgMs, p95Ms, outPath, window = 50 }) {
  const W = 1100;
  const H = 520;
  const margin = { l: 70, r: 20, t: 45, b: 60 };
  const pw = W - margin.l - margin.r;
  const ph = H - margin.t - margin.b;

  const maxY = Math.max(1, ...elapsed, p95Ms, avgMs);
  const yMax = Math.ceil(maxY / 10) * 10;
  const yMin = 0;
  const xMin = 1;
  const xMax = Math.max(1, elapsed.length);

  const x = (i) => margin.l + ((i - xMin) / (xMax - xMin || 1)) * pw;
  const y = (v) => margin.t + (1 - (v - yMin) / (yMax - yMin || 1)) * ph;

  const ma = movingAverage(elapsed, window);

  const gridY = 6;
  const gridX = 6;

  const dots = elapsed
    .map((v, i) => {
      const cx = x(i + 1);
      const cy = y(v);
      return `<circle cx="${cx.toFixed(2)}" cy="${cy.toFixed(2)}" r="1.2" fill="#9aa0a6" fill-opacity="0.35" />`;
    })
    .join("\n");

  const maPath = ma
    .map((v, i) => `${i === 0 ? "M" : "L"} ${x(i + 1).toFixed(2)} ${y(v).toFixed(2)}`)
    .join(" ");

  const avgY = y(avgMs);
  const p95Y = y(p95Ms);

  const gridLines = [
    ...Array.from({ length: gridY + 1 }, (_, i) => {
      const v = (yMax / gridY) * i;
      const yy = y(v);
      return `<line x1="${margin.l}" y1="${yy.toFixed(2)}" x2="${W - margin.r}" y2="${yy.toFixed(
        2
      )}" stroke="#e5e7eb" stroke-width="1" />\n<text x="${
        margin.l - 10
      }" y="${(yy + 4).toFixed(2)}" text-anchor="end" font-size="12" fill="#374151">${v.toFixed(
        0
      )}</text>`;
    }),
    ...Array.from({ length: gridX + 1 }, (_, i) => {
      const v = Math.round((xMax / gridX) * i);
      const xx = x(Math.max(1, v));
      return `<line x1="${xx.toFixed(2)}" y1="${margin.t}" x2="${xx.toFixed(
        2
      )}" y2="${H - margin.b}" stroke="#f3f4f6" stroke-width="1" />`;
    }),
  ].join("\n");

  const legend = `
    <rect x="${margin.l}" y="${margin.t - 30}" width="420" height="22" rx="6" fill="#ffffff" fill-opacity="0.85" stroke="#e5e7eb"/>
    <circle cx="${margin.l + 14}" cy="${margin.t - 19}" r="3" fill="#9aa0a6" fill-opacity="0.5"/>
    <text x="${margin.l + 26}" y="${margin.t - 15}" font-size="12" fill="#111827">单次请求</text>
    <line x1="${margin.l + 92}" y1="${margin.t - 19}" x2="${margin.l + 116}" y2="${margin.t - 19}" stroke="#2563eb" stroke-width="2"/>
    <text x="${margin.l + 124}" y="${margin.t - 15}" font-size="12" fill="#111827">移动平均</text>
    <line x1="${margin.l + 200}" y1="${margin.t - 19}" x2="${margin.l + 224}" y2="${margin.t - 19}" stroke="#ef4444" stroke-width="2"/>
    <text x="${margin.l + 232}" y="${margin.t - 15}" font-size="12" fill="#111827">平均值</text>
    <line x1="${margin.l + 292}" y1="${margin.t - 19}" x2="${margin.l + 316}" y2="${margin.t - 19}" stroke="#16a34a" stroke-width="2"/>
    <text x="${margin.l + 324}" y="${margin.t - 15}" font-size="12" fill="#111827">95%分位</text>
  `;

  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="#ffffff"/>
  <text x="${W / 2}" y="26" text-anchor="middle" font-size="18" font-weight="600" fill="#111827">${title}</text>
  <text x="18" y="${margin.t + 10}" font-size="12" fill="#374151">响应时间(ms)</text>
  <text x="${W / 2}" y="${H - 18}" text-anchor="middle" font-size="12" fill="#374151">样本序号</text>

  ${gridLines}

  <rect x="${margin.l}" y="${margin.t}" width="${pw}" height="${ph}" fill="transparent" stroke="#d1d5db"/>

  ${dots}

  <path d="${maPath}" fill="none" stroke="#2563eb" stroke-width="2" stroke-linejoin="round"/>

  <line x1="${margin.l}" y1="${avgY.toFixed(2)}" x2="${W - margin.r}" y2="${avgY.toFixed(
    2
  )}" stroke="#ef4444" stroke-width="2" />
  <text x="${W - margin.r - 6}" y="${(avgY - 6).toFixed(
    2
  )}" text-anchor="end" font-size="12" fill="#ef4444">平均值 ${avgMs.toFixed(2)} ms</text>

  <line x1="${margin.l}" y1="${p95Y.toFixed(2)}" x2="${W - margin.r}" y2="${p95Y.toFixed(
    2
  )}" stroke="#16a34a" stroke-width="2" />
  <text x="${W - margin.r - 6}" y="${(p95Y - 6).toFixed(
    2
  )}" text-anchor="end" font-size="12" fill="#16a34a">95%分位 ${p95Ms.toFixed(0)} ms</text>

  ${legend}
</svg>`;

  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, svg, "utf8");
}

function formatPct(x) {
  return `${(x * 100).toFixed(3)}%`;
}

function fmtMs(x) {
  return `${Number(x).toFixed(2)}`;
}

function mdTable(stats) {
  return [
    `| 指标 | 值 |`,
    `|---|---:|`,
    `| 样本 | ${stats.samples} |`,
    `| 平均值(ms) | ${fmtMs(stats.avgMs)} |`,
    `| 中位数(ms) | ${fmtMs(stats.medianMs)} |`,
    `| 90%分位(ms) | ${fmtMs(stats.p90Ms)} |`,
    `| 95%分位(ms) | ${fmtMs(stats.p95Ms)} |`,
    `| 99%分位(ms) | ${fmtMs(stats.p99Ms)} |`,
    `| 最小值/最大值(ms) | ${fmtMs(stats.minMs)} / ${fmtMs(stats.maxMs)} |`,
    `| 异常率 | ${formatPct(stats.errorRate)} |`,
    `| 吞吐量(RPS) | ${stats.throughputRps.toFixed(2)} |`,
    `| 测试时长(s) | ${stats.durationS.toFixed(2)} |`,
  ].join("\n");
}

function main() {
  const root = process.cwd();
  const reportDir = path.join(root, "docs", "jmeter_performance_assets");
  fs.mkdirSync(reportDir, { recursive: true });

  const inputs = [
    { name: "samples_1000", title: "系统样本约1000性能测试图形结果" },
    { name: "samples_3000", title: "系统样本约3000性能测试图形结果" },
    { name: "samples_5000", title: "系统样本约5000性能测试图形结果" },
  ];

  const results = [];
  for (const it of inputs) {
    const jtl = path.join(root, "jmeter_results", `${it.name}.jtl`);
    if (!fs.existsSync(jtl)) continue;
    const { rows } = readJtl(jtl);
    const stats = computeStats(rows);
    const elapsed = rows.map((r) => r.elapsed);
    const svgPath = path.join(reportDir, `${it.name}.svg`);
    svgChart({
      title: it.title,
      elapsed,
      avgMs: stats.avgMs,
      p95Ms: stats.p95Ms,
      outPath: svgPath,
      window: 50,
    });
    results.push({ ...it, jtl, svgPath, stats });
  }

  const mdPath = path.join(root, "docs", "jmeter_performance_report.md");
  const lines = [];
  lines.push("## JMeter 性能测试报告");
  lines.push("");
  lines.push("- **测试对象**: `GET /api/stream`（来自 `scripts/watch_stream_benchmark.jmx`）");
  lines.push("- **说明**: 指标均基于 `.jtl`（CSV）结果统计生成；图形为根据单次请求响应时间绘制。");
  lines.push("");

  for (const r of results) {
    lines.push(`## 样本量约 ${r.stats.samples}`);
    lines.push("");
    lines.push(mdTable(r.stats));
    lines.push("");
    const rel = path.relative(path.dirname(mdPath), r.svgPath).replaceAll("\\", "/");
    lines.push(`![${r.title}](${rel})`);
    lines.push("");
  }

  fs.writeFileSync(mdPath, lines.join("\n"), "utf8");
  console.log(`Wrote ${path.relative(root, mdPath)}`);
  for (const r of results) {
    console.log(`${r.name}: samples=${r.stats.samples}, avgMs=${r.stats.avgMs.toFixed(2)}, p95Ms=${r.stats.p95Ms.toFixed(2)}, err=${formatPct(r.stats.errorRate)}`);
  }
}

main();

