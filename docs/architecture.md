# Server Architecture

## Services

### NetworkService

- Accepts incoming client connections
- Spawns a corresponding ClientHandlingService
- Rate limits concurrent connections

#### ClientHandlingService

- One per client
- Manages lifecycle of client connection
- Receives and responds to client requests
- Dispatches server commands to CommandProcessingService

### CommandProcessingService

- accepts server commands
- processes the server specific commands, or dispatches processing to the StorageService

### StorageService

- processes storage related tasks

## Dependencies / Communication

- NetworkService creates ClientHandlingServices
- ClientHandlingServices dispatch to CommandProcessingService
    - ServerCommandProcessingDispatcher can be used to submit commands for processing
- CommandProcessingService dispatches to StorageService
    - StorageCommandProcessingDispatcher can be used to submit commands for processing
