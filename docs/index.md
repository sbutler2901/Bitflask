## Features
### Multiple segments
- A new segment file will be created once the current active file's size has reached a threshold
    - current performance: creating 17000 entries ~1sec

## TODO:
### Multi threaded
- A singular writer thread exist
- multiple reader threads can exists

### Config file
- overwrite pre-existing segment files
- segment threshold
- performance testing
- logging