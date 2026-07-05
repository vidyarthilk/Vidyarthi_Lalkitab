#!/usr/bin/env python3
"""
Rebuild cities.db with India populated places (villages/towns) where:
  - GeoNames population > 100, OR
  - population unknown (0) but feature is a village/town code (PPL, PPLL, ...)

Non-India rows are kept from the existing bundled DB.

Requires on this PC (defaults):
  D:\\allCountries.txt
  D:\\admin1CodesASCII.txt

Output:
  app/src/main/assets/database/cities.db
"""
from __future__ import annotations

import os
import shutil
import sqlite3
import sys
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_GEONAMES = r"D:\allCountries.txt"
DEFAULT_ADMIN1 = r"D:\admin1CodesASCII.txt"
TARGET_DB = os.path.join(ROOT, "app", "src", "main", "assets", "database", "cities.db")

VILLAGE_CODES = frozenset(
    {"PPL", "PPLA", "PPLA2", "PPLA3", "PPLA4", "PPLL", "PPLF", "PPLG", "PPLR", "PPLS"}
)

# Same boxes as fill_india_states.py / IndiaStateInfer.kt
BOXES = [
    ("Andaman and Nicobar Islands", 6.45, 14.75, 92.0, 94.25),
    ("Lakshadweep", 8.0, 12.5, 71.5, 74.5),
    ("Delhi", 28.4, 28.9, 76.85, 77.35),
    ("Chandigarh", 30.65, 30.78, 76.75, 76.82),
    ("Puducherry", 11.85, 12.05, 79.75, 79.9),
    ("Dadra and Nagar Haveli and Daman and Diu", 20.0, 20.85, 72.6, 73.2),
    ("Goa", 14.75, 15.78, 73.55, 74.42),
    ("Sikkim", 27.05, 28.15, 88.0, 88.95),
    ("Arunachal Pradesh", 26.35, 29.55, 91.35, 97.45),
    ("Nagaland", 25.15, 26.75, 93.15, 95.75),
    ("Manipur", 23.75, 25.85, 92.85, 94.85),
    ("Mizoram", 21.85, 24.65, 92.15, 93.55),
    ("Tripura", 22.75, 24.65, 90.85, 92.35),
    ("Meghalaya", 25.0, 26.35, 89.75, 92.85),
    ("Assam", 24.05, 28.05, 89.45, 96.05),
    ("Telangana", 15.75, 19.95, 77.15, 81.75),
    ("Karnataka", 11.55, 18.5, 74.0, 78.65),
    ("Tamil Nadu", 8.05, 13.55, 76.15, 80.35),
    ("Andhra Pradesh", 12.45, 19.35, 76.65, 84.85),
    ("Gujarat", 20.0, 24.95, 68.1, 74.65),
    ("Maharashtra", 15.55, 22.05, 72.55, 80.95),
    ("Kerala", 8.15, 12.92, 74.85, 77.45),
    ("Odisha", 17.65, 22.65, 81.15, 87.55),
    ("Chhattisgarh", 17.75, 24.15, 80.15, 84.55),
    ("Jharkhand", 21.45, 25.45, 83.0, 88.0),
    ("Bihar", 24.25, 27.75, 83.25, 88.55),
    ("West Bengal", 21.45, 27.35, 85.75, 89.95),
    ("Uttar Pradesh", 23.45, 31.45, 76.95, 84.75),
    ("Rajasthan", 23.0, 32.5, 69.3, 78.4),
    ("Madhya Pradesh", 21.45, 26.95, 74.0, 82.95),
    ("Himachal Pradesh", 30.15, 33.45, 75.55, 79.05),
    ("Uttarakhand", 28.55, 31.5, 77.45, 81.05),
    ("Haryana", 27.45, 31.05, 74.45, 77.55),
    ("Punjab", 29.45, 32.55, 73.45, 76.25),
    ("Ladakh", 32.45, 35.65, 75.95, 79.85),
    ("Jammu and Kashmir", 32.25, 37.25, 73.45, 80.35),
]


def infer_state(lat: float, lon: float) -> str | None:
    if lat < 6.2 or lat > 37.6 or lon < 67.8 or lon > 98.6:
        return None
    for name, min_lat, max_lat, min_lon, max_lon in BOXES:
        if min_lat <= lat <= max_lat and min_lon <= lon <= max_lon:
            return name
    return None


def load_admin1(path: str) -> dict[str, str]:
    mapping: dict[str, str] = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 2:
                continue
            code, name = parts[0], parts[1]
            if code.startswith("IN."):
                mapping[code[3:]] = name
    return mapping


def is_india(country: str | None) -> bool:
    if not country:
        return False
    return country.strip().lower() in ("india", "in", "ind")


def include_india_row(pop: int, feature_code: str) -> bool:
    if pop > 100:
        return True
    if pop == 0 and feature_code in VILLAGE_CODES:
        return True
    return False


def normalize_city(name: str) -> str:
    return name.strip().lower()


def export_non_india(source_db: str) -> list[tuple]:
    con = sqlite3.connect(source_db)
    cur = con.cursor()
    rows = cur.execute(
        """
        SELECT city, state, country, lat, lon, timezone, dst
        FROM cities
        WHERE lower(country) NOT IN ('india', 'in', 'ind')
        """
    ).fetchall()
    con.close()
    return rows


def build(geonames_path: str, admin1_path: str, target_db: str) -> None:
    if not os.path.isfile(geonames_path):
        raise SystemExit(f"Missing GeoNames file: {geonames_path}")
    if not os.path.isfile(admin1_path):
        raise SystemExit(f"Missing admin1 file: {admin1_path}")

    admin1 = load_admin1(admin1_path)
    backup_dir = os.path.join(ROOT, "scripts", "db_backups")
    os.makedirs(backup_dir, exist_ok=True)
    if os.path.isfile(target_db):
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup = os.path.join(backup_dir, f"cities.db.bak_{ts}")
        shutil.copy2(target_db, backup)
        print("Backup:", backup)

    tmp_db = target_db + ".new"
    if os.path.isfile(tmp_db):
        os.remove(tmp_db)

    non_india = export_non_india(target_db) if os.path.isfile(target_db) else []
    print("Non-India rows kept:", len(non_india))

    con = sqlite3.connect(tmp_db)
    cur = con.cursor()
    cur.executescript(
        """
        PRAGMA journal_mode=OFF;
        PRAGMA synchronous=OFF;
        CREATE TABLE cities (
            city TEXT,
            state TEXT,
            country TEXT,
            lat REAL,
            lon REAL,
            timezone TEXT,
            dst INTEGER
        );
        """
    )

    if non_india:
        cur.executemany(
            "INSERT INTO cities VALUES (?,?,?,?,?,?,?)",
            non_india,
        )
        con.commit()

    batch: list[tuple] = []
    india_count = 0

    def flush() -> None:
        nonlocal batch
        if not batch:
            return
        cur.executemany("INSERT INTO cities VALUES (?,?,?,?,?,?,?)", batch)
        con.commit()
        batch = []

    with open(geonames_path, encoding="utf-8", errors="replace") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) < 18:
                continue
            if p[8] != "IN" or p[6] != "P":
                continue
            try:
                pop = int(p[14] or "0")
            except ValueError:
                pop = 0
            if not include_india_row(pop, p[7]):
                continue

            lat = float(p[4])
            lon = float(p[5])
            city = normalize_city(p[2] or p[1])
            if not city:
                continue

            admin1_code = p[10] or ""
            state = admin1.get(admin1_code) or infer_state(lat, lon) or ""
            tz = p[17] or "Asia/Kolkata"
            batch.append((city, state, "india", lat, lon, tz, 0))
            india_count += 1
            if len(batch) >= 5000:
                flush()

    flush()
    print("India rows added:", india_count)

    cur.execute("CREATE INDEX IF NOT EXISTS idx_cities_city ON cities(city COLLATE NOCASE)")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_cities_country ON cities(country)")
    cur.execute(
        "CREATE INDEX IF NOT EXISTS idx_cities_city_country ON cities(city COLLATE NOCASE, country)"
    )
    con.commit()

    total = cur.execute("SELECT COUNT(*) FROM cities").fetchone()[0]
    india_total = cur.execute(
        "SELECT COUNT(*) FROM cities WHERE lower(country)='india'"
    ).fetchone()[0]
    con.close()

    os.replace(tmp_db, target_db)
    size_mb = os.path.getsize(target_db) / 1024 / 1024
    print(f"Done: {target_db}")
    print(f"Total rows: {total:,} | India: {india_total:,} | Size: {size_mb:.1f} MB")


if __name__ == "__main__":
    geonames = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_GEONAMES
    admin1 = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_ADMIN1
    build(geonames, admin1, TARGET_DB)
