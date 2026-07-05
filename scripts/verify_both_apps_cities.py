#!/usr/bin/env python3
"""Verify cities.db in both apps: state/country coverage + display format."""
from __future__ import annotations

import hashlib
import os
import sqlite3

DBS = [
    ("Vidyarthi_Lalkitab", r"d:\astro_data\Vidyarthi_Lalkitab\app\src\main\assets\database\cities.db"),
    ("Vidyarthi", r"d:\Vidyarthi\app\src\main\assets\database\cities.db"),
]

MAJOR = [
    "ahmedabad", "mumbai", "delhi", "bengaluru", "chennai", "kolkata",
    "hyderabad", "pune", "surat", "jaipur", "lucknow", "new york", "london", "dubai",
]


def md5(path: str) -> str:
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def display_line(city: str, state: str | None, country: str) -> str:
    st = (state or "").strip()
    co = (country or "").strip()
    c = (city or "").strip()
    return f"{c}, {st}, {co}" if st else f"{c}, {co}"


def analyze(label: str, path: str) -> dict:
    con = sqlite3.connect(path)
    cur = con.cursor()
    total = cur.execute("SELECT COUNT(*) FROM cities").fetchone()[0]
    no_country = cur.execute(
        "SELECT COUNT(*) FROM cities WHERE country IS NULL OR trim(country)=''"
    ).fetchone()[0]
    with_country = total - no_country

    india = cur.execute(
        "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in','ind')"
    ).fetchone()[0]
    india_state = cur.execute(
        """
        SELECT COUNT(*) FROM cities
        WHERE lower(trim(country)) IN ('india','in','ind')
          AND state IS NOT NULL AND trim(state)!=''
        """
    ).fetchone()[0]

    non_india = total - india
    non_india_state = cur.execute(
        """
        SELECT COUNT(*) FROM cities
        WHERE lower(trim(country)) NOT IN ('india','in','ind')
          AND country IS NOT NULL AND trim(country)!=''
          AND state IS NOT NULL AND trim(state)!=''
        """
    ).fetchone()[0]

    # rows that would show only "city, country" (no state)
    no_state_any = cur.execute(
        "SELECT COUNT(*) FROM cities WHERE state IS NULL OR trim(state)=''"
    ).fetchone()[0]

    samples = []
    for name in MAJOR:
        r = cur.execute(
            """
            SELECT city, state, country FROM cities
            WHERE lower(trim(city))=? LIMIT 1
            """,
            (name,),
        ).fetchone()
        if r:
            samples.append((display_line(r[0], r[1], r[2]), r))

    con.close()
    return {
        "label": label,
        "path": path,
        "exists": os.path.isfile(path),
        "md5": md5(path) if os.path.isfile(path) else None,
        "size_mb": round(os.path.getsize(path) / (1024 * 1024), 2) if os.path.isfile(path) else 0,
        "total": total,
        "with_country": with_country,
        "no_country": no_country,
        "india": india,
        "india_with_state": india_state,
        "non_india": non_india,
        "non_india_with_state": non_india_state,
        "no_state_rows": no_state_any,
        "samples": samples,
    }


def main() -> None:
    results = [analyze(l, p) for l, p in DBS]

    print("=" * 72)
    print("CITIES.DB CHECK — BOTH APPS")
    print("=" * 72)

    for r in results:
        if not r["exists"]:
            print(f"\n[{r['label']}] MISSING: {r['path']}")
            continue
        print(f"\n[{r['label']}]")
        print(f"  File: {r['path']}")
        print(f"  Size: {r['size_mb']} MB | MD5: {r['md5']}")
        print(f"  Total rows: {r['total']:,}")
        print(f"  With country: {r['with_country']:,} ({100*r['with_country']/r['total']:.2f}%)")
        print(f"  Missing country: {r['no_country']:,}")
        print(f"  India rows: {r['india']:,}")
        print(f"  India with state: {r['india_with_state']:,} ({100*r['india_with_state']/max(r['india'],1):.2f}%)")
        print(f"  Non-India with state: {r['non_india_with_state']:,} / {r['non_india']:,}")
        print(f"  Rows without state (any country): {r['no_state_rows']:,}")
        print("  Sample display lines:")
        for line, raw in r["samples"]:
            print(f"    -> {line}")

    if len(results) == 2 and results[0]["md5"] and results[1]["md5"]:
        same = results[0]["md5"] == results[1]["md5"]
        print("\n" + "=" * 72)
        print(f"Both DB files identical: {'YES' if same else 'NO'}")
        print("=" * 72)

    print("\nAPP DISPLAY LOGIC:")
    print("  Vidyarthi:       city, state, country  (state from DB only)")
    print("  Lalkitab:        city, state, country  (DB state + IndiaStateInfer fallback)")
    print("\nSTUDIO RUN NOTE:")
    print("  Uninstall old app OR clear app data first — old copied cities.db may lack state.")
    print("  Fresh install copies new assets/database/cities.db on first launch.")


if __name__ == "__main__":
    main()
