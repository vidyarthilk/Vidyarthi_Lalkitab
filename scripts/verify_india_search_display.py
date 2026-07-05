#!/usr/bin/env python3
import sqlite3

DB = r"d:\astro_data\Vidyarthi_Lalkitab\app\src\main\assets\database\cities.db"
QUERIES = ["ahm", "sur", "mum", "del", "ben", "hyd", "raj", "vad"]

def display(city, state, country):
    st = (state or "").strip()
    co = (country or "").strip()
    return f"{city}, {st}, {co}" if st else f"{city}, {co}"

con = sqlite3.connect(DB)
cur = con.cursor()

print("India-focused autocomplete simulation (like app search):")
for q in QUERIES:
    rows = cur.execute(
        """
        SELECT city, state, country FROM cities
        WHERE city LIKE ? ESCAPE '\\'
        ORDER BY city LIMIT 8
        """,
        (q + "%",),
    ).fetchall()
    india = [r for r in rows if (r[2] or "").strip().lower() in ("india", "in")]
    with_state = sum(1 for r in india if (r[1] or "").strip())
    print(f"\n  Query '{q}' -> {len(rows)} hits, {len(india)} India")
    for r in rows[:5]:
        mark = "OK" if (r[1] or "").strip() and (r[2] or "").strip() else "!!"
        print(f"    [{mark}] {display(*r)}")

# India-only stats
india_total = cur.execute(
    "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in')"
).fetchone()[0]
india_full = cur.execute(
    """
    SELECT COUNT(*) FROM cities
    WHERE lower(trim(country)) IN ('india','in')
      AND state IS NOT NULL AND trim(state)!=''
      AND country IS NOT NULL AND trim(country)!=''
    """
).fetchone()[0]
print(f"\nIndia rows with city+state+country: {india_full:,} / {india_total:,} ({100*india_full/india_total:.4f}%)")

con.close()
