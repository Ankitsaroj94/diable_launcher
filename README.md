# Diable Launcher

A minimal, list-based Android home-screen launcher (`in.ankitsaroj.diable`), written in
Kotlin with Jetpack Compose.

## Features
- One continuous home list: clock and favorites on top, every app A–Z below, with a
  curved alphabet scrubber on the right edge
- App gestures: tap or swipe left to open, swipe right for a pop-up (shortcuts,
  notifications, quick replies, pinned apps, a pop-up widget), long-press for the app menu
  (rename, change icon, categories, hide, uninstall)
- Notification dots and previews, media player, Notification Summary, Usage Breaker
- Pop-up folders, widgets with widget stacks, Move/resize widget
- Search with word-prefix matching, calculator, contacts and web suggestions
- Themes: built-in icon styles and third-party icon packs, fonts, font size, theme
  colours, light/dark mode, wallpapers from Openverse
- The Diable button, Quick Lock, backup & restore

## Build
```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity in.ankitsaroj.diable/.MainActivity
```

Some features need special access granted in system settings: notification access
(dots, media, quick replies, summary) and accessibility (Quick Lock). The app asks when a
feature needs it.
