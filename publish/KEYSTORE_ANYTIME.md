# Release keystore (publish દિવસ પહેલાં કરી લો)

**ખોટું:** keystore/password ખોવાય → app update impossible. USB + paper પર backup.

## Android Studio

1. **Build** → **Generate Signed App Bundle / APK**  
2. **Android App Bundle** → Next  
3. **Create new...**  
   - Path: `D:\astro_data\Vidyarthi_Lalkitab\release\vidyarthi-lalkitab.jks`  
   - Password, Alias `vidyarthi_lalkitab` — લખી રાખો  
4. **release** → Finish (optional test build)

## keystore.properties

```
copy keystore.properties.example → keystore.properties
```

ભરો: passwords, `storeFile=release/vidyarthi-lalkitab.jks`

## Command-line build (publish day)

```powershell
.\scripts\build-release-bundle.ps1
```
