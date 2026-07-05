# 1 July Play Store — Checklist (Vidyarthi Lalkitab)

**Target:** App Play Store પર live · Version **5.1 (25)**  
**Email:** vidyarthilalkitab@gmail.com  
**Privacy:** https://vidyarthilk.github.io/vidyarthi-lalkitab-privacy/

---

## ✅ PC પર તૈયાર (આપણે verify કર્યું)

| Item | Status |
|------|--------|
| AAB release build | ✅ `app\build\outputs\bundle\release\app-release.aab` |
| Version 5.1 / code 25 | ✅ |
| Keystore + admob.properties | ✅ |
| Privacy policy live (GitHub) + account deletion | ✅ |
| Screenshots (14 files) | ✅ `publish\screenshots\` |
| Icon + feature graphic | ✅ `publish\` |
| publisher.config | ✅ |
| Data safety answers doc | ✅ `DATA_SAFETY_ANSWERS.txt` |

---

## ✅ Play Console — થઈ ગયું (conversation મુજબ)

- [x] Production AAB upload + review
- [x] Data safety submitted
- [x] Phone screenshots updated
- [x] Approve થયું

---

## ⏳ Play Console — તમે verify કરો (1 July પહેલાં)

### A) App live છે કે pending?

**Publishing overview** ખોલો:

| Status | 1 July માટે શું કરવું |
|--------|----------------------|
| **Published / Live** | ✅ Done — phone પર Play Store શોધી install test |
| **Approved, pending publish** | 1 July સવારે **Publish** દબાવો |
| **In review** | રાહ — approve થતાં publish |

**Managed publishing ON** હોય → changes approve થયા પછી તમે manually publish કરો (1 July perfect).

Path: **Publishing overview** → Managed publishing

---

### B) Subscription (Premium) — ⚠️ ચેck કરો

App માં Premium buy button છે. Console માં product **Active** હોવું જોઈએ.

**Monetize → Products → Subscriptions**

| Field | Value |
|-------|--------|
| Product ID | `vidyarthi_lalkitab_premium` |
| Status | **Active** |
| Price | set (e.g. ₹99/month) |

**Setup → License testing** → `vidyarthilalkitab@gmail.com` add (purchase test)

❌ જો subscription નથી બનાવ્યું → Settings → Subscribe કામ નહીં કરે.

---

### C) Store listing contact

**Grow users → Store presence → Store settings**

| Field | Value |
|-------|--------|
| Email | vidyarthilalkitab@gmail.com |
| Phone | optional — personal number ન મૂકો જો નઈ જોઈતું |
| Website | privacy URL (optional) |

---

### D) App content — એક વાર ફરી ચેck

**Policy → App content** — બધા green?

| Item | Expected answer |
|------|-----------------|
| Privacy policy | URL set ✅ |
| Ads | Yes, contains ads |
| App access | All functionality available (login optional) |
| Target audience | 13+ |
| Content rating | Done |
| Data safety | Submitted ✅ |

---

### E) Countries / Pricing

**Reach → Countries** → India (અથવા જે countries જોઈએ) selected?

**Pricing** → Free ✅

---

### F) Hindi listing (optional, recommended)

**Store listings → Add translation → Hindi (hi-IN)**  
Text: `docs\play_store_listing_hi.txt`

---

## 📅 1 July — દિવસની action list

```
08:00  Play Console → Publishing overview → status check
       → "Publish" (જો pending હોય)

09:00  Phone પર Play Store → "Vidyarthi Lalkitab" search
       → Install → test: kundli, panchang, login, ads, premium

10:00  Subscription test (license tester account)
       Settings → Subscribe → purchase flow

બાકી   WhatsApp / social share link (optional)
```

---

## ❌ 1 July માટે PC પર નવું AAB જરૂરી નથી

Screenshots, privacy, data safety — બધું listing/content change છે.  
Code change નહીં → **નવો AAB upload નહીં**.

---

## Post-launch (1 July પછી)

- [ ] Play Store link share
- [ ] AdMob dashboard — impressions check
- [ ] User reviews monitor
- [ ] Crash-free? (Play Console → Android vitals)

---

## Address / phone public issue

Monetize app (ads + subscription) → Google legal address public કરે.  
Hide fully possible નથી. Support email = vidyarthilalkitab@gmail.com set કરો.

---

**Last checked:** June 2026 — project files on PC verified.
