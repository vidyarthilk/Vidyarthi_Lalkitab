import sqlite3
import os
import sys


def analyze(path: str, label: str) -> None:
    print("=" * 60)
    print(label, path)
    print("Size MB:", round(os.path.getsize(path) / 1024 / 1024, 2))
    con = sqlite3.connect(path)
    cur = con.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [r[0] for r in cur.fetchall()]
    print("Tables:", tables)
    cur.execute("PRAGMA table_info(cities)")
    cols = [r[1] for r in cur.fetchall()]
    print("Columns:", cols)
    has_state = "state" in [c.lower() for c in cols]
    total = cur.execute("SELECT COUNT(*) FROM cities").fetchone()[0]
    print("Total rows:", total)

    if has_state:
        rows = cur.execute(
            "SELECT city, state, country FROM cities WHERE city LIKE 'Ahmedabad%' LIMIT 10"
        ).fetchall()
        india_total = cur.execute(
            "SELECT COUNT(*) FROM cities WHERE lower(country) IN ('india', 'in')"
        ).fetchone()[0]
        india_state = cur.execute(
            """
            SELECT COUNT(*) FROM cities
            WHERE lower(country) IN ('india', 'in')
              AND state IS NOT NULL AND trim(state) != ''
            """
        ).fetchone()[0]
        hyphen_city = cur.execute(
            "SELECT COUNT(*) FROM cities WHERE city LIKE '%-%'"
        ).fetchone()[0]
        comma_city = cur.execute(
            "SELECT COUNT(*) FROM cities WHERE city LIKE '%,%'"
        ).fetchone()[0]
        hyphen_all = cur.execute(
            """
            SELECT COUNT(*) FROM cities
            WHERE city LIKE '%-%' OR IFNULL(state,'') LIKE '%-%' OR country LIKE '%-%'
            """
        ).fetchone()[0]
        print(f"India rows: {india_total}, with state: {india_state} ({100*india_state/max(india_total,1):.1f}%)")
        print(f"City column with hyphen: {hyphen_city}, with comma: {comma_city}")
        print(f"Any field hyphen pattern rows: {hyphen_all}")
        print("Ahmedabad samples:", rows[:5])
        sample = cur.execute(
            "SELECT city, state, country FROM cities WHERE city='Ahmedabad' AND country='India' LIMIT 5"
        ).fetchall()
        print("Ahmedabad + India exact:", sample)
        ah = cur.execute(
            "SELECT city, state, country, lat, lon FROM cities WHERE city='ahmedabad' AND country='india' LIMIT 5"
        ).fetchall()
        print("ahmedabad lat/lon rows:", ah)
        with_state = cur.execute(
            "SELECT COUNT(*) FROM cities WHERE state IS NOT NULL AND trim(state) != ''"
        ).fetchone()[0]
        print("Rows with non-empty state (all countries):", with_state)
        print("Top countries:", cur.execute(
            "SELECT country, COUNT(*) c FROM cities GROUP BY country ORDER BY c DESC LIMIT 8"
        ).fetchall())
        print("Hyphen city examples (India):", cur.execute(
            "SELECT city, country FROM cities WHERE city LIKE '%-%' AND country='india' LIMIT 8"
        ).fetchall())
        print("Sample India display (app format city, state, country):")
        for r in cur.execute(
            """
            SELECT city, state, country FROM cities
            WHERE lower(country)='india'
            ORDER BY city
            LIMIT 15
            """
        ):
            st = (r[1] or "").strip()
            disp = f"{r[0]}, {st}, {r[2]}" if st else f"{r[0]}, {r[2]}"
            print(" ", disp)
    else:
        rows = cur.execute(
            "SELECT city, country FROM cities WHERE city LIKE 'Ahmedabad%' LIMIT 10"
        ).fetchall()
        print("Ahmedabad samples (no state col):", rows[:5])
    con.close()


def same_file(a: str, b: str) -> None:
    import hashlib

    def md5(p: str) -> str:
        h = hashlib.md5()
        with open(p, "rb") as f:
            for chunk in iter(lambda: f.read(1024 * 1024), b""):
                h.update(chunk)
        return h.hexdigest()

    ha, hb = md5(a), md5(b)
    print("=" * 60)
    print("Same DB file?", ha == hb, "md5", ha[:16], "...")


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    paths = [
        (os.path.join(root, "app", "src", "main", "assets", "database", "cities.db"), "Lalkitab cities.db"),
        (os.path.join(root, "app", "src", "main", "assets", "database", "cities1.db"), "Lalkitab cities1.db"),
        (r"d:\Vidyarthi\app\src\main\assets\database\cities.db", "Vidyarthi cities.db"),
        (r"d:\Vidyarthi\app\src\main\assets\database\cities1.db", "Vidyarthi cities1.db"),
    ]
    for p, label in paths:
        if not os.path.isfile(p):
            print("MISSING:", p)
            continue
        analyze(p, label)
    if len(paths) >= 2 and all(os.path.isfile(paths[0][0]) for _ in [0]) and os.path.isfile(paths[2][0]):
        same_file(paths[2][0], paths[0][0])

    # Simulate app display for ahmedabad
    print("=" * 60)
    print("App display simulation for ahmedabad, india (uses cities.db ONLY):")
    con = sqlite3.connect(paths[2][0])
    cur = con.cursor()
    row = cur.execute(
        "SELECT city, state, country, lat, lon FROM cities WHERE city='ahmedabad' AND country='india' LIMIT 1"
    ).fetchone()
    if row:
        city, state, country, lat, lon = row
        # Lalkitab: infer state if empty for India
        boxes = [("Gujarat", 20.0, 24.95, 68.1, 74.65)]
        inferred = None
        for name, min_lat, max_lat, min_lon, max_lon in boxes:
            if min_lat <= lat <= max_lat and min_lon <= lon <= max_lon:
                inferred = name
                break
        st = (state or "").strip() or inferred
        lalkitab = f"{city}, {st}, {country}" if st else f"{city}, {country}"
        vidyarthi = f"{city}, {country}" if not (state or "").strip() else f"{city}, {state}, {country}"
        print("  DB raw:", row)
        print("  Vidyarthi shows:", vidyarthi)
        print("  Lalkitab shows:", lalkitab)
        print("  User asked format (hyphen):", f"{city}-{st}-{country}" if st else f"{city}-{country}")
    con.close()
