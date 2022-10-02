## Features

### Multiple segments

- A new segment file will be created once the current active file's size has reached a threshold

### Multithreading

- Concurrent reading / writing

### Crash handling and recovery / Load Previous State

- recreate hash map offsets
    - Replay segments

### Compaction

- Removing duplicate keys only keeping most recent
- Merge multiple segments into a single, new segment
- deleting old segments

### Configuration

- Server Configuration
- Storage Configuration
- Adjustable via cli flags & property file

### Binary serialization

- length header
    - key / value

## TODO:

### Commands

- del: delete key:value mapping
    - tombstoning:
        - Prevent mapping during startup loading
        - Ignore key during compaction
    - Pre-reload handling options:
      a. Need to delete mapping from all segment's with key
      b. Have active segment store key map as deleted

### Caching

- Cache read values (write-through cache?)
- Populate during startup segment loading

### Compaction / Deletion

- Using [event bus](https://github.com/google/guava/wiki/EventBusExplained) for publishing
- Tombstones for deleted segments (in case of failure)

### Containerization

- Docker / podman deployments

### Crash handling and recovery / Load Previous State

- Store hashmap snapshots to speed startup
- Detect partially written records

### Logging

- Persist logs to disk

### Testing

- Integration tests

### Configuration

- Client specific configurations

#### Additions

- overwrite pre-existing segment files
- performance testing
- logging level
