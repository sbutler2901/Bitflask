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
- Dispatches client messages to CommandProcessingService

### CommandProcessingService

- accepts server commands
- processes the server specific commands, or dispatches processing to the StorageService

### StorageService

- processes storage related tasks

## Dependencies / Communication

- NetworkService creates ClientHandlingServices
- ClientHandlingServices submits to CommandProcessingService
- CommandProcessingService dispatches to StorageService
    - StorageCommandDispatcher can be used to submit commands for processing
