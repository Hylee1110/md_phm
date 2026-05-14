import fs from "node:fs";

function parseCsvLine(line) {
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

function percentile(sortedValues, p) {
  if (!sortedValues.length) return 0;
  const k = (sortedValues.length - 1) * p;
  const f = Math.floor(k);
  const c = Math.ceil(k);
  if (f === c) return sortedValues[k | 0];
  const d0 = sortedValues[f] * (c - k);
  const d1 = sortedValues[c] * (k - f);
  return d0 + d1;
}

function readJtl(jtlPath) {
  const text = fs.readFileSync(jtlPath, "utf8");
  const lines = text.split(/\r?\n/).filter(Boolean);
  const header = parseCsvLine(lines[0] ?? "");
  const idx = Object.fromEntries(header.map((h, i) => [h, i]));
  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    const timeStamp = Number(cols[idx.timeStamp] ?? 0);
    const elapsed = Number(cols[idx.elapsed] ?? 0);
    const success = String(cols[idx.success] ?? "").toLowerCase() === "true";
    rows.push({ timeStamp, elapsed, success });
  }
  return rows;
}

function stats(rows) {
  const samples = rows.length;
  const ok = rows.reduce((a, r) => a + (r.success ? 1 : 0), 0);
  const errorRate = samples ? (samples - ok) / samples : 0;

  const stamps = rows.map((r) => r.timeStamp).filter((n) => Number.isFinite(n) && n > 0);
  const durationS = stamps.length ? (Math.max(...stamps) - Math.min(...stamps)) / 1000 : 0;
  const throughput = durationS > 0 ? samples / durationS : 0;

  const elapsed = rows.map((r) => r.elapsed).filter((n) => Number.isFinite(n)).sort((a, b) => a - b);
  const sum = elapsed.reduce((a, b) => a + b, 0);
  const avg = elapsed.length ? sum / elapsed.length : 0;
  const p95 = percentile(elapsed, 0.95);
  const max = elapsed.length ? elapsed[elapsed.length - 1] : 0;

  return { samples, avg, p95, max, throughput, errorRate };
}

function fmt2(n) {
  return Number(n).toFixed(2);
}

function fmtPct(n) {
  return `${(n * 100).toFixed(2)}%`;
}

function buildTable(rows) {
  return [
    "| 并发线程数 | 样本数 | 平均响应时间/ms | 95%响应时间/ms | 最大响应时间/ms | 吞吐量/req/s | 错误率 |",
    "|---:|---:|---:|---:|---:|---:|---:|",
    ...rows.map(
      (r) =>
        `| ${r.threads} | ${r.samples} | ${fmt2(r.avg)} | ${Math.round(r.p95)} | ${Math.round(r.max)} | ${fmt2(
          r.throughput
        )} | ${fmtPct(r.errorRate)} |`
    ),
  ].join("\n");
}

function updateDoc({ docPath, tableMarkdown }) {
  const doc = fs.readFileSync(docPath, "utf8");
  const marker = "**表 6-3  `/api/stream` 接口性能测试结果**";
  const idx = doc.indexOf(marker);
  if (idx === -1) throw new Error("Table marker not found in doc.");

  const afterMarker = doc.slice(idx);
  const tableStart = afterMarker.indexOf("\n\n| ");
  if (tableStart === -1) throw new Error("Existing table not found after marker.");

  const absTableStart = idx + tableStart + 2; // points to the '|' line
  const rest = doc.slice(absTableStart);
  const nextSection = rest.search(/\n\n[^|]/); // first blank line then non-table char
  if (nextSection === -1) throw new Error("Could not find table end.");
  const absTableEnd = absTableStart + nextSection;

  return doc.slice(0, absTableStart) + tableMarkdown + doc.slice(absTableEnd);
}

function main() {
  const t10 = stats(readJtl("jmeter_results/table6_3_threads10.jtl"));
  const t30 = stats(readJtl("jmeter_results/table6_3_threads30.jtl"));
  const t50 = stats(readJtl("jmeter_results/table6_3_threads50.jtl"));

  const table = buildTable([
    { threads: 10, ...t10 },
    { threads: 30, ...t30 },
    { threads: 50, ...t50 },
  ]);

  const docPath = "docs/jmeter_performance_report.md";
  const updated = updateDoc({ docPath, tableMarkdown: table });
  fs.writeFileSync(docPath, updated, "utf8");

  console.log("Updated table 6-3 in", docPath);
  console.log({ t10, t30, t50 });
}

main();

