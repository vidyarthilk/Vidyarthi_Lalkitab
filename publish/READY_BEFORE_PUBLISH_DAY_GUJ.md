# Publish દિવસ પહેલાં કરી લો (Play Store upload નહીં)

આ કામ એક વાર કરો — publish દિવસે ફક્ત `PLAY_STORE_DAY_GUJ.md` અનુસરો.

---

## ✅ Project માં પહેલેથી થયેલું

- Package: `com.vidyarthi.lalkitab`
- AdMob code (banner — ફક્ત input/saved screens)
- Privacy policy HTML template
- Play listing text (English + Hindi)
- Release signing setup (`keystore.properties`)
- Build scripts

---

## તમે કરો (કોઈ પણ દિવસે, 1–2 કલાક)

### 1) એક config file (15 મિનિટ)

```
publisher.config.example  →  copy  →  publisher.config
```

ભરો: email, privacy URL (પછી), AdMob IDs  
ચલાવો:

```powershell
.\scripts\sync-publish-config.ps1
```

### 2) AdMob account (15 મિનિટ)

[admob.google.com](https://admob.google.com) → App + Banner unit → IDs `publisher.config` માં.

### 3) Privacy GitHub Pages (10 મિનિટ)

`publish/GITHUB_PRIVACY_5MIN.txt` અનુસરો.  
પછી `sync-publish-config.ps1` ફરી ચલાવો.

### 4) Keystore (10 મિનિટ, એક વાર)

`publish/KEYSTORE_ANYTIME.md` — `.jks` બનાવો, backup લો, `keystore.properties` ભરો.

### 5) Screenshots (20 મિનિટ)

Phone પર app ચલાવી 4–6 screenshot:  

- New kundli form  
- Saved list  
- Panchang  
- Kundli chart

`publish/screenshots/` માં save કરો.  
Sizes: `publish/GRAPHICS_SIZES.txt`

### 6) Store graphics (optional પહેલાં)

- 512×512 app icon (Play માટે export)  
- 1024×500 feature graphic (Canva માં બનાવી શકાય)

### 7) App test

Release build phone પર ચલાવી ચેક: save kundli, chart, ads input screen પર.

---

## Publish દિવસે

ફક્ત: `**publish/PLAY_STORE_DAY_GUJ.md**` (30–60 મિનિટ)