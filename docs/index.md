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

### Binary serialization

- length header
    - key / value

## TODO:

### Caching

- Cache read values (write-through cache?)
- Cache the latest segment with key for reading
    - When key not already in cache
    - Compaction will invalidate cache

### Networking

- [gRPC](https://grpc.io)
    - [Protocol Buffers](https://developers.google.com/protocol-buffers)

### Compaction / Deletion

- Using [event bus](https://github.com/google/guava/wiki/EventBusExplained) for publishing
- Tombstones for deleted key:value pairs
    - Prevent copying during compaction
- Tombstones for deleted segments (in case of failure)

### Multithreading

- Lock only portions of file while writing rather than entire file being locked

### Crash handling and recovery / Load Previous State

- Store hashmap snapshots to speed startup
- Detect partially written records

### Clean shutdown

- Provide clean way to shut server down even during sigkill
    - including finishing writes, closing all sockets / threads

### Logging

- Persist logs to disk

### Testing

- Integration tests

### Config file

- overwrite pre-existing segment files
- segment threshold
- performance testing
- logging level

### Various inputs

- Configurable by env / properties / etc
- REPL
- HTTP
- etc
