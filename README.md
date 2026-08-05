# Welcome

*MoniCopy* is an easy-to-use folder copy app for macOS and Windows built with Kotlin and Compose Desktop. The app is released under the Apache-2.0 License. Its usage is quite simple:

- Pick source and destination directories
- Choose directories to ignore (for example the local copy of cloud storage)
- Decide if you want to keep orphans (files that were once there, but no longer are)
- Click **Start**

MoniCopy only copies new and changed files.

<img src="./screenshots/macos_01.png" alt="MoniCopy on macOS — choose source and destination" width="30%" />
&nbsp;
<img src="./screenshots/macos_02.png" alt="MoniCopy on macOS — ignored directories" width="30%" />
&nbsp;
<img src="./screenshots/macos_03.png" alt="MoniCopy on macOS — finding files to copy" width="30%" />
&nbsp;
<img src="./screenshots/macos_04.png" alt="MoniCopy on macOS — copying in progress" width="30%" />
&nbsp;
<img src="./screenshots/macos_05.png" alt="MoniCopy on macOS — deleting orphaned files" width="30%" />
&nbsp;
<img src="./screenshots/macos_06.png" alt="MoniCopy on macOS — finished" width="30%" />

### Known limitations

- MoniCopy cannot access files that are currently in use
