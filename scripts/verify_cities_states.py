#!/usr/bin/env python3
import sqlite3
import sys

db = sys.argv[1] if len(sys.argv) > 1 else r"d:\astro_data\Vidyarthi_Lalkitab\app\src\main\assets\database\cities.db"
con = sqlite3.connect(db)
cur = con.cursor()
cities = ["mumbai", "delhi", "bangalore", "bengaluru", "chennai", "kolkata", "hyderabad", "pune", "ahmedabad", "surat", "jaipur", "lucknow", "vadodara", "rajkot"]
print("Major cities:")
for c in cities:
    r = cur.execute(
        "SELECT city, state, lat, lon FROM cities WHERE lower(trim(city))=? AND lower(trim(country))='india' LIMIT 1",
        (c,),
    ).fetchone()
    print(" ", r)
india = cur.execute("SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in')").fetchone()[0]
with_st = cur.execute(
    "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in') AND state IS NOT NULL AND trim(state)!=''"
).fetchone()[0]
empty = cur.execute(
    "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in') AND (state IS NULL OR trim(state)='')"
).fetchone()[0]
print(f"India: {with_st}/{india} with state, {empty} empty")
print("Unmatched sample:")
for r in cur.execute(
    "SELECT city, lat, lon FROM cities WHERE lower(trim(country)) IN ('india','in') AND (state IS NULL OR trim(state)='') LIMIT 20"
):
    print(" ", r)
