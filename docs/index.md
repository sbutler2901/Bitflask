## Features
### Multiple segments

- A new segment file will be created once the current active file's size has reached a threshold
  - current performance: creating 22000 entries ~1sec

## TODO:

### Compaction

- Removing duplicate keys only keeping most recent
- Merge multiple segments into a single, new segment
- deleting old segments

### Load previous state

- On start up, load previously written data

### Clean shutdown

- Provide clean way to shut server down even during sigkill
  - including finishing writes, closing all sockets / threads

### Logger

- Add proper logging with configuration

### Binary serialization

- length header
  - key / value

### Multi threaded

- A singular writer thread exist
- multiple reader threads can exist

### Crash handling and recovery
- recreate hash map offsets
  - Replay segments
  - or, store hashmap snapshots

### Config file
- overwrite pre-existing segment files
- segment threshold
- performance testing
- logging

### Various inputs
- Configurable by env / properties / etc
- REPL
- HTTP
- etc