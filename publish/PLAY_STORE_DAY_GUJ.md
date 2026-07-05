# Play Store મૂકવાનો દિવસ — ફક્ત 30–60 મિનિટ

બાકી બધું પહેલેથી તૈયાર છે. આ દિવસે ફક્ત નીચેનું કરો.

---

## પહેલાં ચેક (2 મિનિટ)

- `publisher.config` ભરેલું + `.\scripts\sync-publish-config.ps1` ચલાવ્યું
- GitHub Pages પર privacy live (link browser માં ખુલે)
- `keystore.properties` + `release/vidyarthi-lalkitab.jks` છે
- Phone screenshots folder `publish/screenshots/` માં 4–6 ફોટો

---

## Step 1 — AAB બનાવો (5–10 મિનિટ)

PowerShell, project folder માં:

```powershell
.\scripts\build-release-bundle.ps1
```

આખરે path દેખાશે: `app-release.aab` — આ file Play Console પર upload કરવાની છે.

---

## Step 2 — Play Console (20–40 મિનિટ)

1. [play.google.com/console](https://play.google.com/console) → login
2. જો account નથી: **Register** ($25, એક વાર)
3. **Create app** → Vidyarthi Lalkitab → Free → Declarations

### Store listing

`publish/PLAY_CONSOLE_COPY_PASTE.txt` ખોલો — text copy-paste કરો.  
Screenshots + 512 icon + 1024×500 feature graphic upload.

### Policy


| Menu            | શું કરવું                                |
| --------------- | ---------------------------------------- |
| Privacy policy  | `publisher.config` માંની URL             |
| Ads             | Yes, contains ads                        |
| Content rating  | `publish/CONTENT_RATING_ANSWERS.txt` જુઓ |
| Data safety     | `publish/DATA_SAFETY_ANSWERS.txt` જુઓ    |
| Target audience | 13+ / not for children                   |


### Release

**Production** → **Create release** → `app-release.aab` upload → Release notes:  
`First release: Lal Kitab kundli, panchang, varshfal.`

### Review

બધા tasks green → **Send for review**

---

## Step 3 — રાહ (Google)

Review: કલાકોથી 7 દિવસ. Email આવશે.

---

## ભૂલ થાય તો


| સમસ્યા           | ઉકેલ                                          |
| ---------------- | --------------------------------------------- |
| AAB build fail   | `admob.properties`, `keystore.properties` ચેક |
| Privacy reject   | URL browser માં ખુલે છે? email HTML માં?      |
| Package conflict | જૂની `com.example...` app uninstall           |


---

**બાકીની તૈયારી:** `publish/READY_BEFORE_PUBLISH_DAY_GUJ.md`