# Build error: Tag mismatch / guava download

આ app ની ભૂલ **નથી** — PC પર Gradle library (Guava) download ખરાબ થાય છે.

## એક કમાન્ડ (સૌથી સરળ)

PowerShell માં:

```powershell
cd D:\astro_data\Vidyarthi_Lalkitab
.\scripts\fix-gradle-download.ps1
```

5–10 મિનિટ રાહ. **SUCCESS** આવે તો Android Studio માં Run કરો.

## જો ફરી fail થાય

1. **Antivirus** — Windows Security → Virus & threat protection → Manage settings → Exclusions → Add:
   - `C:\Users\તમારું-નામ\.gradle`
   - `D:\astro_data\Vidyarthi_Lalkitab`

2. **VPN બંધ** કરી **mobile hotspot** થી internet try કરો

3. Script **ફરી** ચલાવો

4. Android Studio: **File → Invalidate Caches → Restart**

## Manual (જો script ન ચાલે)

```powershell
cd D:\astro_data\Vidyarthi_Lalkitab
.\gradlew --stop
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\com.google.guava" -ErrorAction SilentlyContinue
.\scripts\setup-local-guava.ps1
.\gradlew assembleDebug --no-daemon
```
