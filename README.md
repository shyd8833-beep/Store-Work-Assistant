# Store Work Assistant

A simple native Android checklist app for daily store operations.

## Features
- Daily checklist with Done/Pending tracking and progress count
- English text-to-speech for pending tasks while the app is open
- Configurable background reminder notifications (15–120 minutes)
- Saturday Item Issue task and Alternate Day material-check placeholder

## Open in Android Studio
1. Download or clone this repository.
2. In Android Studio choose **Open** and select the repository folder.
3. Allow Gradle sync, then run on an Android device or emulator.

## Notes
- WorkManager periodic jobs are inexact; Android battery optimization may delay notifications.
- Background reminders are notifications. Spoken reminders work while the app is open.
- The exact Alternate Day materials still need confirmation from the handwritten list.
- Saturday Item Issue is currently a checklist item; automatic weekday scheduling is a next enhancement.
