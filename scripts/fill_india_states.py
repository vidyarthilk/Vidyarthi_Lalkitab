#!/usr/bin/env python3
"""
Fill empty [state] for India rows in cities.db using lat/lon bounding boxes
(mirror of IndiaStateInfer.kt in the app).
"""
from __future__ import annotations

import os
import shutil
import sqlite3
import sys
from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class Box:
    state: str
    min_lat: float
    max_lat: float
    min_lon: float
    max_lon: float


# Same order as IndiaStateInfer.kt — first match wins (specific boxes before large states).
BOXES: list[Box] = [
    Box("Andaman and Nicobar Islands", 6.45, 14.75, 92.0, 94.25),
    Box("Lakshadweep", 8.0, 12.5, 71.5, 74.5),
    Box("Delhi", 28.4, 28.9, 76.85, 77.35),
    Box("Chandigarh", 30.65, 30.78, 76.75, 76.82),
    Box("Puducherry", 11.85, 12.05, 79.75, 79.9),
    Box("Dadra and Nagar Haveli and Daman and Diu", 20.0, 20.85, 72.6, 73.2),
    Box("Goa", 14.75, 15.78, 73.55, 74.42),
    Box("Sikkim", 27.05, 28.15, 88.0, 88.95),
    Box("Arunachal Pradesh", 26.35, 29.55, 91.35, 97.45),
    Box("Nagaland", 25.15, 26.75, 93.15, 95.75),
    Box("Manipur", 23.75, 25.85, 92.85, 94.85),
    Box("Mizoram", 21.85, 24.65, 92.15, 93.55),
    Box("Tripura", 22.75, 24.65, 90.85, 92.35),
    Box("Meghalaya", 25.0, 26.35, 89.75, 92.85),
    Box("Assam", 24.05, 28.05, 89.45, 96.05),
    Box("Telangana", 15.75, 19.95, 77.15, 81.75),
    Box("Karnataka", 11.55, 18.5, 74.0, 78.65),
    Box("Tamil Nadu", 8.05, 13.55, 76.15, 80.35),
    Box("Andhra Pradesh", 12.45, 19.35, 76.65, 84.85),
    Box("Gujarat", 20.0, 24.95, 68.1, 74.65),
    Box("Maharashtra", 15.55, 22.05, 72.55, 80.95),
    Box("Kerala", 8.15, 12.92, 74.85, 77.45),
    Box("Odisha", 17.65, 22.65, 81.15, 87.55),
    Box("Chhattisgarh", 17.75, 24.15, 80.15, 84.55),
    Box("Jharkhand", 21.45, 25.45, 83.0, 88.0),
    Box("Bihar", 24.25, 27.75, 83.25, 88.55),
    Box("West Bengal", 21.45, 27.35, 85.75, 89.95),
    Box("Uttar Pradesh", 23.45, 31.45, 76.95, 84.75),
    Box("Rajasthan", 23.0, 32.5, 69.3, 78.4),
    Box("Madhya Pradesh", 21.45, 26.95, 74.0, 82.95),
    Box("Himachal Pradesh", 30.15, 33.45, 75.55, 79.05),
    Box("Uttarakhand", 28.55, 31.5, 77.45, 81.05),
    Box("Haryana", 27.45, 31.05, 74.45, 77.55),
    Box("Punjab", 29.45, 32.55, 73.45, 76.25),
    Box("Ladakh", 32.45, 35.65, 75.95, 79.85),
    Box("Jammu and Kashmir", 32.25, 37.25, 73.45, 80.35),
]


def infer_state(lat: float, lon: float) -> str | None:
    if lat < 6.2 or lat > 37.6 or lon < 67.8 or lon > 98.6:
        return None
    for b in BOXES:
        if b.min_lat <= lat <= b.max_lat and b.min_lon <= lon <= b.max_lon:
            return b.state
    return None


def is_india(country: str | None) -> bool:
    if not country:
        return False
    c = country.strip().lower()
    return c in ("india", "in", "ind")


def process_db(db_path: str, dry_run: bool = False, force: bool = False) -> dict:
    con = sqlite3.connect(db_path)
    cur = con.cursor()
    cols = [r[1].lower() for r in cur.execute("PRAGMA table_info(cities)")]
    if "state" not in cols:
        raise SystemExit(f"No state column in {db_path}")

    if force and not dry_run:
        cur.execute(
            """
            UPDATE cities SET state = NULL
            WHERE lower(trim(country)) IN ('india', 'in', 'ind')
            """
        )
        con.commit()

    state_filter = "" if force else "AND (state IS NULL OR trim(state) = '')"
    rows = cur.execute(
        f"""
        SELECT rowid, city, country, lat, lon, state
        FROM cities
        WHERE lower(trim(country)) IN ('india', 'in', 'ind')
          {state_filter}
        """
    ).fetchall()

    updates: list[tuple[str, int]] = []
    unmatched = 0
    for rowid, city, country, lat, lon, _state in rows:
        if lat is None or lon is None:
            unmatched += 1
            continue
        st = infer_state(float(lat), float(lon))
        if st:
            updates.append((st, rowid))
        else:
            unmatched += 1

    if not dry_run and updates:
        cur.executemany("UPDATE cities SET state = ? WHERE rowid = ?", updates)
        con.commit()
        cur.execute("VACUUM")

    india_total = cur.execute(
        "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india', 'in', 'ind')"
    ).fetchone()[0]
    india_with_state = cur.execute(
        """
        SELECT COUNT(*) FROM cities
        WHERE lower(trim(country)) IN ('india', 'in', 'ind')
          AND state IS NOT NULL AND trim(state) != ''
        """
    ).fetchone()[0]
    ahmedabad = cur.execute(
        """
        SELECT city, state, country, lat, lon FROM cities
        WHERE lower(trim(city)) = 'ahmedabad'
          AND lower(trim(country)) IN ('india', 'in', 'ind')
        LIMIT 3
        """
    ).fetchall()

    con.close()
    return {
        "path": db_path,
        "india_empty_before": len(rows),
        "updated": len(updates),
        "unmatched": unmatched,
        "india_total": india_total,
        "india_with_state": india_with_state,
        "ahmedabad": ahmedabad,
        "dry_run": dry_run,
    }


def main() -> None:
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    source = os.path.join(root, "app", "src", "main", "assets", "database", "cities.db")
    targets = [
        source,
        r"d:\Vidyarthi\app\src\main\assets\database\cities.db",
    ]

    if not os.path.isfile(source):
        print("ERROR: source not found:", source)
        sys.exit(1)

    dry_run = "--dry-run" in sys.argv
    force = "--force" in sys.argv
    if dry_run:
        stats = process_db(source, dry_run=True, force=force)
        print("DRY RUN:", stats)
        return

    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_dir = os.path.join(root, "scripts", "backups")
    os.makedirs(backup_dir, exist_ok=True)

    if not force:
        backup = os.path.join(backup_dir, f"cities.db.bak_{ts}")
        shutil.copy2(source, backup)
        print("Backup:", backup)

    stats = process_db(source, dry_run=False, force=force)
    print("Updated source:", stats)

    for dest in targets[1:]:
        if not os.path.isfile(dest):
            print("Skip missing:", dest)
            continue
        shutil.copy2(source, dest)
        verify = process_db(dest, dry_run=True)
        print("Copied to:", dest)
        print("  verify india_with_state:", verify["india_with_state"], "/", verify["india_total"])
        print("  ahmedabad:", verify["ahmedabad"])


if __name__ == "__main__":
    main()
