# Survey Data Model

## Overview

SexyTopo represents cave surveys as a directed graph structure where stations are nodes connected by legs (edges). Understanding this model is critical for working with survey data operations.

## Core Entities

### Station
A `Station` represents a point in the survey (a node in the graph).

**Key Properties:**
- `name`: String identifier (e.g., "0", "1", "2", "3.1")
- `onwardLegs`: List of legs going FROM this station (outbound edges)
- `comment`: Optional text annotation
- `extendedElevationDirection`: LEFT or RIGHT for extended elevation views

**Important Concepts:**
- A station "owns" its onward legs (legs going OUT from it)
- A station does NOT store a reference to the leg that leads TO it (the "referring leg")
- The origin station (usually "0") has NO referring leg

### Leg
A `Leg` represents a measurement between two points (a directed edge in the graph).

**Key Properties:**
- `distance`: Length in metres
- `azimuth`: Compass bearing (0-360°)
- `inclination`: Vertical angle (-90° to +90°)
- `destination`: The station this leg points TO (may be `Survey.NULL_STATION`)
- `wasShotBackwards`: Whether this was measured as a backsight
- `promotedFrom`: Array of legs if this was upgraded from splay(s)

**Types of Legs:**

1. **Full Leg (Connected Leg)**
   - Has a `destination` that is NOT `Survey.NULL_STATION`
   - Creates/connects to a named station
   - Forms the skeleton/centreline of the survey
   - Check with: `leg.hasDestination()` returns `true`

2. **Splay (Unconnected Leg)**
   - Has `destination == Survey.NULL_STATION`
   - Used to capture cave passage detail (walls, floor, ceiling)
   - Does NOT create a new station
   - Check with: `leg.hasDestination()` returns `false`

## Graph Structure

The survey forms a **tree structure**:

```
                    [0] origin
                     |
            +--------+--------+
            |                 |
        full leg           splay (no destination)
            |
           [1]
            |
       +----+----+
       |         |
   full leg    splay
       |
      [2]
```

**Key Relationships:**

- **From Station → Leg**: A station OWNS its onward legs
  - Access via: `station.getOnwardLegs()`
  - A station can have multiple onward legs (both full legs and splays)

- **Leg → To Station**: A full leg POINTS TO a destination station
  - Access via: `leg.getDestination()` (or `Survey.NULL_STATION` for splays)

- **Station ← Leg**: Finding the leg that leads TO a station (the "referring leg")
  - Access via: `survey.getReferringLeg(station)`
  - Searches the entire survey graph to find which leg has this station as destination
  - Returns `null` for the origin station
  - **IMPORTANT**: This is an expensive traversal operation

- **Leg → From Station**: Finding which station a leg originates from
  - Access via: `survey.getOriginatingStation(leg)`
  - Searches all stations to find which has this leg in its `onwardLegs` list
  - **IMPORTANT**: This is an expensive traversal operation

## Deletion Semantics

### Deleting a Station
When you delete a station, you MUST delete the leg that forms it:

```java
// A station comes as a package with the leg that leads to it
Leg referringLeg = survey.getReferringLeg(station);
Station fromStation = survey.getOriginatingStation(referringLeg);
SurveyUpdater.deleteLeg(survey, fromStation, referringLeg);
```

**What gets deleted:**
- The referring leg (that creates the station)
- The station itself (implicitly, as no leg points to it anymore)
- ALL onward legs from that station (full legs AND splays)
- ALL descendant stations and their legs (entire subtree)

### Deleting a Leg
There are two cases:

**1. Deleting a Full Leg (has destination):**
```java
SurveyUpdater.deleteLeg(survey, fromStation, leg);
```
- Removes the leg from `fromStation.getOnwardLegs()`
- Removes the destination station (as nothing points to it anymore)
- Removes ALL legs in the entire subtree rooted at the destination
- Removes ALL chronological records of those legs

**2. Deleting a Splay (no destination):**
```java
SurveyUpdater.deleteLeg(survey, fromStation, splay);
```
- Simply removes the splay from `fromStation.getOnwardLegs()`
- No cascade deletion (splays have no descendants)

### Key Insight for UI Operations

When the user clicks on a row in the table or a point in the graph view:

**For a FULL LEG row/point:**
- The "from" station is where the leg originates
- The "to" station is `leg.getDestination()`
- Deleting deletes: the leg + the destination station + entire subtree

**For a SPLAY row/point:**
- The "from" station is where the splay originates
- There is NO "to" station (`leg.getDestination() == Survey.NULL_STATION`)
- Deleting deletes: just that single splay measurement

## Chronological Order

The `Survey` maintains:
- `legsInChronoOrder`: A stack of legs in the order they were measured
- Used for undo functionality and export to certain formats

## Data Integrity

After any structural change, call:
```java
survey.checkSurveyIntegrity();
survey.setSaved(false);
```

This validates:
- All destination stations are reachable from origin
- No orphaned stations exist
- The graph is a valid tree structure

## Common Operations Mapping

| User Action | Correct Approach |
|-------------|-----------------|
| Delete station "X" | Get referring leg to X, delete that leg |
| Delete leg (from table/graph) | Use the specific leg object from the view |
| Delete splay (from table) | Use the specific splay leg object, NOT getReferringLeg |
| Count what will be deleted | Traverse subtree from leg's destination (if it has one) |

## Edge Cases

1. **Origin Station**
   - Cannot be deleted
   - Has no referring leg (`getReferringLeg(origin)` returns `null`)
   - Check: `station == survey.getOrigin()`

2. **NULL_STATION**
   - Sentinel value `Survey.NULL_STATION`
   - Used as destination for splays
   - NOT a real station in the graph

3. **Backwards Shots**
   - `leg.wasShotBackwards()` indicates measurement direction
   - Same graph structure, just metadata for display/export
   - Doesn't affect deletion logic
