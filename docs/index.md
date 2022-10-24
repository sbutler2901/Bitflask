## Features

### Entry Querying / Modification

- Get (read)
- Set (write)
- Del (delete)

### Multiple segments

- A new segment file will be created once the current active file's size has reached a threshold

### Multithreading

- Concurrent reading / writing

### Crash handling and recovery / Load Previous State

- recreate hash map offsets
    - Replay segments

### Compaction

- Removing duplicate keys only keeping most recent
- Removing deleted keys
- Merge multiple segments into a single, new segment
- Deleting old segments

### Configuration

- Server Configuration
- Storage Configuration
- Adjustable via cli flags & property file

### Containerization

#### Server

- Build and execution within Docker
- Depends on custom gradle Dockerfile fork to support JDK 19

### Client

- REPL (interactive) command execution
- inline command execution
- file piping execution

### Segment File Encoding

#### Header

- Segment Key

#### Entries

- Main header:
    - key value entry
    - deleted entry
- Length headers:
    - key
    - value

## TODO / Ideas:

### Segment

#### Ordering

- By Segment key
    - update segment keys after compaction
- Frozen header in segment file
    - Time segment was frozen

### Caching

- Cache read values (write-through cache?)
- Populate during startup segment loading

### Compaction / Deletion

- Using [event bus](https://github.com/google/guava/wiki/EventBusExplained)
  - Still needed?
  - current writable segment publish that its size limit has been exceeded
  - compaction indicates it has completed
    - SegmentManager update with results
    - SegmentDeleter activated

### Crash handling and recovery / Load Previous State

- Store hashmap snapshots to speed startup
- Detect partially written records

### Logging

- Persist logs to disk

### Integration Tests

- Test client container that executes commands
- Evaluate final file results
- Need to confirm compaction correct

### Client

### Configuration

#### Server

- performance testing
- logging level

#### Client
