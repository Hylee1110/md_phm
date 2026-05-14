import argparse
import csv
import math
import statistics
from dataclasses import dataclass
from pathlib import Path


def percentile(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    xs = sorted(values)
    k = (len(xs) - 1) * p
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return float(xs[int(k)])
    d0 = xs[f] * (c - k)
    d1 = xs[c] * (k - f)
    return float(d0 + d1)


@dataclass(frozen=True)
class JMeterStats:
    samples: int
    avg_ms: float
    median_ms: float
    p90_ms: float
    p95_ms: float
    p99_ms: float
    min_ms: float
    max_ms: float
    error_rate: float
    throughput_rps: float
    duration_s: float


def load_jtl(path: Path) -> tuple[list[int], list[float], list[bool]]:
    timestamps: list[int] = []
    elapsed: list[float] = []
    success: list[bool] = []

    with path.open("r", newline="", encoding="utf-8") as f:
        r = csv.DictReader(f)
        for row in r:
            try:
                timestamps.append(int(row.get("timeStamp", "0") or 0))
            except ValueError:
                continue
            try:
                elapsed.append(float(row.get("elapsed", "0") or 0))
            except ValueError:
                elapsed.append(0.0)
            success.append(str(row.get("success", "")).lower() == "true")

    return timestamps, elapsed, success


def compute_stats(timestamps: list[int], elapsed: list[float], success: list[bool]) -> JMeterStats:
    total = len(elapsed)
    ok = sum(1 for s in success if s)
    err_rate = (total - ok) / total if total else 0.0

    if timestamps:
        dur_s = (max(timestamps) - min(timestamps)) / 1000.0
    else:
        dur_s = 0.0
    thr = (total / dur_s) if dur_s > 0 else 0.0

    return JMeterStats(
        samples=total,
        avg_ms=float(statistics.mean(elapsed)) if elapsed else 0.0,
        median_ms=float(statistics.median(elapsed)) if elapsed else 0.0,
        p90_ms=percentile(elapsed, 0.90),
        p95_ms=percentile(elapsed, 0.95),
        p99_ms=percentile(elapsed, 0.99),
        min_ms=float(min(elapsed)) if elapsed else 0.0,
        max_ms=float(max(elapsed)) if elapsed else 0.0,
        error_rate=float(err_rate),
        throughput_rps=float(thr),
        duration_s=float(dur_s),
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("jtl", type=Path, help="Path to JMeter .jtl (CSV)")
    args = ap.parse_args()

    ts, elapsed, succ = load_jtl(args.jtl)
    st = compute_stats(ts, elapsed, succ)

    # CSV-like single-line output (easy to copy into docs)
    print(
        ",".join(
            [
                f"samples={st.samples}",
                f"avg_ms={st.avg_ms:.2f}",
                f"median_ms={st.median_ms:.2f}",
                f"p90_ms={st.p90_ms:.2f}",
                f"p95_ms={st.p95_ms:.2f}",
                f"p99_ms={st.p99_ms:.2f}",
                f"min_ms={st.min_ms:.2f}",
                f"max_ms={st.max_ms:.2f}",
                f"error_rate={st.error_rate*100:.3f}%",
                f"throughput_rps={st.throughput_rps:.2f}",
                f"duration_s={st.duration_s:.2f}",
            ]
        )
    )


if __name__ == "__main__":
    main()

