# Song Hunter v1

Android app starter for searching downloadable audio in:
- Internet Archive
- Wikimedia Commons

## Features
- One search field for both sources
- Audio file results with format and size
- Shows creator and licence metadata when available
- Uses Android DownloadManager to save permitted files under `Music/SongHunter`
- Does not expose a download action if an open licence/public-domain marker is not recognized

## Build APK with GitHub Actions
1. Open the repository's **Actions** tab.
2. Run **Build Song Hunter APK** (or push to `main`).
3. Download the `SongHunter-debug-apk` artifact from the completed workflow.

## Important
Archive metadata and licence labels can be incomplete or user-supplied. The app is conservative but cannot make a legal determination. Review the item page and licence conditions before reuse. This first version searches two sources only; additional catalogues can be added in later versions.
