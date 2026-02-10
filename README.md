# Shipment Tracking Simulator

## Project Overview

This is a small Kotlin Compose-based client-server application that showcases shipment lifecycle modeling using object-oriented design principles such as the Factory, Observer, Strategy, and State patterns to create a modular and extensible shipment tracking system.

## Features

- **Shipment Lifecycle Management**: Models complete shipment journey from creation to delivery
- **Real-time Simulation**: Track shipments through various stages (created, delivered, delayed, lost, location, cancelled, shipped, note added)
- **Event-driven Architecture**: Client-side shipment updates trigger server-side state updates
- **Compose UI**: Minimal Kotlin Compose interface

## Quick Start

### Requirements:
- JDK 17+
- Gradle (wrapper included)

### Build and run:

Use `.\gradlew.bat` on Windows, or `./gradlew` on Mac/Linux.

```sh
gradle build
gradle run
```

## Usage

Shipment update format:

```text
updateType,shipmentId,timestampOfUpdate,otherInfo (optional)
```

- `UpdateType`: {`created`, `delivered`, `delayed`, `lost`, `cancelled`, `location`, `shipped`, `noteAdded`}
- `shipmentId`: unique string
- `timestampOfUpdate`: Unix epoch (ms since 01/01/1970)
- `otherInfo`:
    - `delayed`: timestamp of expected delivery (Unix epoch)
    - `location`: current shipment location
    - `noteAdded`: note text

Once the app is running:

1. Locate both the client and server GUI windows (one may be on top of the other)
2. Enter a properly formatted (see above) shipment update in client window text field
    - Click button to send update to server
3. Enter shipment ID in server window text field
    - Click left button to track shipment
    - Click right button to stop tracking shipment
4. Repeat steps 2 and 3 as desired
    - Send additional shipment updates to server via client window
    - View update history of tracked shipments in server window

## Architecture

This project uses several OOP patterns:

- **State Pattern**: Model shipment states and transitions
- **Observer Pattern**: Notify UI of state changes
- **Strategy Pattern**: Different strategies of handling updates
- **Factory Pattern**: Create shipment objects

## Diagrams

### Version 2 UML

![UML Diagram](img/Shipment%20Tracking%20Simulator%20V2.svg)

### Version 1 UML

![UML Diagram](img/Shipment%20Tracking%20Simulator.svg)
