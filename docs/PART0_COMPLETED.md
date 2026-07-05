# Part 0 — Publish preparation (completed in project)

## 0.1 Package name
- **applicationId:** `com.vidyarthi.lalkitab` (was `com.example.vidyarthi_lalkitab`)
- All Kotlin sources moved to `com.vidyarthi.lalkitab`
- Layout XML custom views updated

**Note:** If you already installed the old package on a phone, uninstall it first. Play Store treats this as a new app.

## 0.2 AdMob
- **Debug:** Google test ad units (automatic)
- **Release:** Create `admob.properties` from `admob.properties.example` with real IDs from https://admob.google.com
- Release build fails with a clear error if `admob.properties` is missing

## 0.3 Privacy policy
- Template: `docs/privacy_policy.html`
- Hosting steps: `docs/HOST_PRIVACY_POLICY.txt`
- Replace `YOUR_EMAIL@example.com` in the HTML
- Set `privacy_policy_url` in `res/values/strings.xml` after hosting

## 0.4 Store listing text
- English: `docs/play_store_listing_en.txt`
- Hindi: `docs/play_store_listing_hi.txt`
- You still need: 512×512 icon, 1024×500 feature graphic, 2+ phone screenshots

## Your next steps
1. AdMob → create app + banner unit → fill `admob.properties`
2. Host privacy policy → update URL in strings.xml
3. Build → Generate Signed Bundle (release)
4. Play Console → Part 1 onward from main guide
