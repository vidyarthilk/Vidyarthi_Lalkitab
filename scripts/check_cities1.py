import sqlite3
import os

paths = [
    ("Lalkitab cities1.db", r"d:\astro_data\Vidyarthi_Lalkitab\app\src\main\assets\database\cities1.db"),
    ("Vidyarthi cities1.db", r"d:\Vidyarthi\app\src\main\assets\database\cities1.db"),
]
for label, p in paths:
    print("=" * 50, label)
    print("size MB", round(os.path.getsize(p) / 1024 / 1024, 2))
    con = sqlite3.connect(p)
    cur = con.cursor()
    cols = [r[1] for r in cur.execute("PRAGMA table_info(cities)")]
    print("columns:", cols)
    n = cur.execute("SELECT COUNT(*) FROM cities").fetchone()[0]
    print("rows:", n)
    if "state" in [c.lower() for c in cols]:
        q = "SELECT city, state, country FROM cities WHERE city LIKE '%ahmedabad%' OR city LIKE '%Ahmedabad%' LIMIT 5"
    else:
        q = "SELECT city, country FROM cities WHERE city LIKE '%ahmedabad%' OR city LIKE '%Ahmedabad%' LIMIT 5"
    print("ahmedabad samples:", cur.execute(q).fetchall())
    con.close()
