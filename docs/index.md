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

- Depends on gradle `build` and `distTar` executed on local machine

### Segment File Encoding

#### Entries

- Main header:
    - key value entry
    - deleted entry
- Length headers:
    - key
    - value

## TODO / Ideas:

### Containerization

- Figure out how to build server within docker container
    - Gradle docker image needed for JDK19?
- Persistent volume for segment files

### Files

- Segment File header
    - segment's key

### Caching

- Cache read values (write-through cache?)
- Populate during startup segment loading

### Compaction / Deletion

- Using [event bus](https://github.com/google/guava/wiki/EventBusExplained) for publishing
    - still needed?

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

- Executing commands from file

### Configuration

#### Server

- overwrite pre-existing segment files
- performance testing
- logging level

#### Client

- server IP
- server port
