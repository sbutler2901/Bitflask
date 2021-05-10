## Features
### Multiple segments
- A new segment file will be created once the current active file's size has reached a threshold
    - current performance: creating 22000 entries ~1sec

## TODO:
### Logger
- Add proper logging with configuration

### Binary serialization
- length header
  - key / value
  
### Multi threaded
- A singular writer thread exist
- multiple reader threads can exists

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