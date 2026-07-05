# Play Console — બાકી બધું (Identity approve પછી + હમણાં prep)

**App:** Vidyarthi Lalkitab  
**Package:** `com.vidyarthi.lalkitab`  
**Email:** vidyarthilalkitab@gmail.com  
**Privacy:** https://vidyarthilk.github.io/vidyarthi-lalkitab-privacy/

---

## ✅ PC પર પહેલેથી તૈયાર

| Item | Status | Location |
|------|--------|----------|
| Play account + $25 | ✅ | તમે કર્યું |
| Identity documents upload | ⏳ | Google review |
| AAB (release) | ✅ | `app\build\outputs\bundle\release\app-release.aab` |
| App icon 512 | ✅ | `publish\app-icon-512.png` |
| Feature graphic | ✅ | `publish\feature-1024x500.png` |
| Phone screenshots (7) | ✅ | `publish\screenshots\` |
| Privacy policy live | ✅ | URL browser માં ખુલે |
| AdMob IDs | ✅ | `publisher.config` |
| Keystore | ✅ | `keystore.properties` + `release\` |
| Listing text | ✅ | `publish\PLAY_CONSOLE_COPY_PASTE.txt` |

**નવું AAB જોઈએ તો:**
```powershell
cd D:\astro_data\Vidyarthi_Lalkitab
.\scripts\build-release-bundle.ps1
```

---

## ⏳ હમણાં (identity review દરમિયán) શું કરી શકો

Google review ચાલુ હોય ત્યારે **phone verify નહીં** થાય. પણ આ prep કરી શકો:

### A) Play Console માં app create (જો ન કર્યું)

1. [play.google.com/console](https://play.google.com/console)
2. **Create app**
3. Name: **Vidyarthi Lalkitab**
4. Default language: **English (United States)** — Hindi listing પછી add
5. App / Free / Declarations accept

### B) Store listing draft (અમુક fields save થઈ શકે)

**Grow users → Store presence → Main store listing**

| Field | Copy-paste |
|-------|------------|
| App name | `Vidyarthi Lalkitab` |
| Short description | `Lal Kitab kundli, panchang, varshfal & 35-year cycle — save charts offline.` |
| Full description | `PLAY_CONSOLE_COPY_PASTE.txt` માંથી |
| App icon | `publish\app-icon-512.png` upload |
| Feature graphic | `publish\feature-1024x500.png` upload |
| Phone screenshots | `publish\screenshots\` — બધા 7 JPEG upload |
| Category | **Lifestyle** (અથવા Books & Reference) |
| Email | `vidyarthilalkitab@gmail.com` |
| Privacy policy | `https://vidyarthilk.github.io/vidyarthi-lalkitab-privacy/` |

**Save draft** — publish identity પછી.

### C) Hindi listing (optional)

**Store presence → Main store listing → Add translation → Hindi**

Text: `docs\play_store_listing_hi.txt`

---

## 📧 Identity approve થયા પછી — ક્રમ (આ order follow કરો)

```
Email "Identity verified" આવે
    ↓
1. Phone verify (SMS OTP)
    ↓
2. Store listing finalize + save
    ↓
3. App content declarations
    ↓
4. Content rating
    ↓
5. Data safety
    ↓
6. Ads declaration
    ↓
7. Subscription create
    ↓
8. Production release + AAB upload
    ↓
9. Send for review
```

---

## Step 1 — Phone verify

**Settings → Developer account → About you → Contact details**

1. Contact email verify (OTP) — pending હોય તો પહેલા
2. Phone **+91** number → **Verify** → SMS code
3. Personal account: developer phone optional (India); organization માટે public phone જોઈએ

---

## Step 2 — App content (Policy → App content)

Dashboard tasks એક-એક:

| Question | Answer |
|----------|--------|
| Privacy policy | URL paste (ઉપર) |
| Ads | **Yes**, contains ads |
| App access | **All functionality available without restrictions** (email login optional — guest 10 saves) |
| Content ratings | Step 4 નીચે |
| Target audience | **13+** / Not designed for children |
| News app | **No** |
| COVID contact tracing | **No** |
| Data safety | Step 5 નીચે |
| Government apps | **No** |
| Financial features | **No** |

---

## Step 3 — Content rating

**Policy → App content → Content ratings → Start questionnaire**

Email enter → IARC questionnaire:

| Question | Answer |
|----------|--------|
| Violence | No |
| Sexuality | No |
| Bad language | No |
| Drugs | No |
| Gambling | No |
| User interaction / chat | No |
| Shares precise location | No |
| Unrestricted internet | **Yes** (AdMob) |
| Occult / astrology | Informational only / No extra flags |

Result: usually **Everyone** or **3+** — accept.

---

## Step 4 — Data safety

**Policy → App content → Data safety → Start**

### Overview
- **Does your app collect or share user data?** → **Yes**
- **Is all data encrypted in transit?** → **Yes** (HTTPS for ads)

### Data types

**Collected by YOU (developer):** → **No** for name, DOB, location  
(Birth data = device only, not sent to your server)

**Collected by third parties (AdMob):**
- **Device or other IDs** → Collected → Shared with Google → **Advertising**
- **App interactions** (ads) → if asked → Advertising

**Location:** → **No** (city typed by user, not GPS)

**Personal info:**
- **Email address** → Collected (device) → NOT Shared → Account management
- Name, DOB → **No** (device only, not sent to developer server)

### Other
- Account creation: **Yes** — username/password (email)
- Delete account URL: privacy policy link
- Delete data without account: **Yes** (delete kundlis in app)
- Purpose: **Advertising** + **Account management** + **App functionality** (billing)

Save → Submit.

(Updated June 2026 — see `DATA_SAFETY_ANSWERS.txt`.)

---

## Step 5 — Subscription (Premium)

**Monetize → Products → Subscriptions → Create subscription**

| Field | Value |
|-------|--------|
| Product ID | `vidyarthi_lalkitab_premium` |
| Name | Vidyarthi Premium |
| Description | Unlimited saved kundlis. No ads. |
| Base plan | Monthly (e.g. ₹99) અથવા Yearly |
| Status | **Activate** |

**Setup → License testing** → તમારો Gmail add (purchase test)

---

## Step 6 — Production release

**Release → Production → Create new release**

1. **Upload:** `app\build\outputs\bundle\release\app-release.aab`
2. **Release name:** `1.0.0`
3. **Release notes:**
   ```
   First release: Lal Kitab kundli, panchang, varshfal, 35-year cycle, Raja-Vazir-Dhoka, Hindi/English.
   ```
4. **Review release** → **Start rollout to Production**

---

## Step 7 — Send for review

Dashboard → બધા tasks complete → **Send app for review**

Review: **1–7 days** → email → Play Store live 🎉

---

## Quick checklist (print કરી લો)

- [ ] Identity email received (Approved)
- [ ] Phone verified
- [ ] Store listing saved (icon + 7 screenshots + feature graphic)
- [ ] Privacy URL set
- [ ] Ads = Yes
- [ ] Content rating done
- [ ] Data safety submitted
- [ ] Target audience 13+
- [ ] Subscription `vidyarthi_lalkitab_premium` **Activated**
- [ ] AAB uploaded to Production
- [ ] Send for review clicked

---

## ભૂલ થાય તો

| Problem | Fix |
|---------|-----|
| AAB reject — signing | `keystore.properties` same keystore as first upload |
| Privacy reject | URL browser માં ખુલે? email HTML માં? |
| Subscription not found in app | Product ID exact + **Activated** |
| Data safety mismatch | AdMob = declare advertising ID via Google |
| Package conflict on phone | Old `com.example...` test app uninstall |

---

**Identity approve email આવે → Step 1 (phone) થી શરૂ કરો. અડચણ આવે તો screenshot મોકલો.**
