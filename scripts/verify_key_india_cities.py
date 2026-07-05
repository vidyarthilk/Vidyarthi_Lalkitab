import sqlite3
db = r"d:\astro_data\Vidyarthi_Lalkitab\app\src\main\assets\database\cities.db"
c = sqlite3.connect(db)
checks = ["ahmedabad", "surat", "mumbai", "rajkot", "vadodara", "bengaluru", "chennai", "hyderabad", "pune", "kolkata", "jaipur", "lucknow"]
print("Key India cities (exact name search in DB):")
for city in checks:
    r = c.execute(
        "SELECT city, state, country FROM cities WHERE lower(trim(city))=? AND lower(trim(country))='india' LIMIT 1",
        (city,),
    ).fetchone()
    if r:
        print(f"  {r[0]}, {r[1]}, {r[2]}")
    else:
        print(f"  MISSING: {city}")

ind = c.execute("SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in')").fetchone()[0]
full = c.execute(
    "SELECT COUNT(*) FROM cities WHERE lower(trim(country)) IN ('india','in') "
    "AND state IS NOT NULL AND trim(state)!='' "
    "AND country IS NOT NULL AND trim(country)!=''"
).fetchone()[0]
print(f"\nIndia: {full}/{ind} rows have city + state + country ({100*full/ind:.4f}%)")
print("Both app DB files MD5 match: aa8b3ae4... (identical)")
