# MoniCopy 1.1.2

- Restored 64 MiB I/O buffers so large files hash and copy more efficiently
- Copy and delete progress now show as live status on the left, with the log kept on the right
- Optional setting to preserve symbolic links when copying
- Orphan cleanup also removes leftover symbolic links
- Copy complete status shows how many files were copied vs already there
- Quieter log during copy (no per-file chatter)
