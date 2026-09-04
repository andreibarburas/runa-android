# Runa — Changelog

## v1.2.0 (2026-07-24)

🗺️ Changed:
- Map tile provider migrated from CartoDB to OpenStreetMap MAPNIK (CartoDB now requires an API key)
- Dark mode uses MAPNIK tiles with a warm luminance-inversion ColorMatrixColorFilter instead of a separate dark tile provider
- All basemaps.cartocdn.com references removed

## v1.1.1 (2026-07-24)

🔧 Maintenance:
- compileSdk and targetSdk raised to 36
- AGP 8.10.1, Gradle 8.11.1
- Room 2.7.1, Coroutines 1.10.2, Lifecycle 2.9.0, Navigation 2.9.0
- DataStore 1.1.4, core-ktx 1.16.0, Compose BOM 2025.05.00
- ProGuard rules cleaned up

## v1.1.0 (2026-07-24)

✨ Added:
- Tag support — add tags to any entry in Write/Edit screens
- Filter entries by tag in the Read screen (horizontal tag chip row)
- Tags displayed in entry list and entry detail view
- Tags included in Nextcloud sync and ZIP export

## v1.0.1 (2026-07-12)

✨ Added:
- Join r/BarburasLab link in Settings → Support

## v1.0.0 (2026-06-26)

✨ New:
- Write tab: compose journal entries with title, body, date/time, and up to 5 photos
- Date & time chips on Write screen — tappable, pre-filled to now, editable for retroactive journaling
- Photo attachments via gallery or camera, with EXIF rotation correction and downsampling
- Share image(s) from any app → opens Runa's Write screen with photos pre-attached
- Read tab: chronological entry list with title, preview, date, and photo thumbnail
- Entry detail screen: full-page nostalgic journal view with hero photo pager, decorative rule, and generous line height
- Edit entry screen: full editing with date/time and photo management
- Storage choice screen: local-only or Nextcloud sync
- Nextcloud Login Flow v2 with poll URL rewriting (Tailscale/reverse-proxy compatible)
- Biometric app lock
- Theme toggle: system / light / dark
- Text size scaling: S / M / L / XL
- Font pairing toggle: DM Serif Display + Inter Tight
- Settings screen: account, security, display, and support sections
