# 2026-04-11 1.11.0
- Extensive Therion file and scrap naming improvements with cross-section handling (thanks Andrew Atkinson)
- Connection lines and splays in XVI export (thanks Andrew Atkinson)
- Added option to merge splay with leg (thanks Andrew Atkinson)
- PocketTopo .top file import support (thanks Andrew Atkinson)
- Trip management overhaul: date picker, team member selection, caver name storage (with Andrew Atkinson)
- LRUD perpendicular mode option (thanks Damian Ivereigh)
- Improved context menus: separate leg and station menus, with from/to station awareness (with Damian Ivereigh). This is partly a reversion to previous behaviour, but keeping the unified station menu across different views. 
- New compass display on plan view (turn-offable in sketch quick settings)
- Basic survey sharing support through the Android share system
- Experimental 3D view (a bit of a toy at this point - don't complain about the lack of features!)
- Drawing engine now uses dp instead of pixels for screen-size scaling
- Colour tweaks to distinguish different data items in sketch view
- Sketch items too small to be visible are now culled for performance
- Landscape mode layout improvements
- Version and survey name added to non-survey data files
- Translation updates
- Material3 component and style updates throughout

# 2026-02-21 1.10.2
- Several bugs fixed (thanks Damian Ivereigh and Andrew Atkinson)
- Improved comment handling on table view (thanks Damian Ivereigh)
- SVG export improvements (thanks Damian Ivereigh)
- Work in progress for supporting CavwayX1
- Improved UI for linking surveys
- New "Find Station" tool
- Italian, Spanish, Polish and Portuguese translations
- Some updates to the Manual

# 2026-01-16 1.10.1
- Bugfixes including another rotation bug and station renaming issues

# 2026-01-16 1.10.0
- Table UI redesign - tap to view / edit rows and edit data more intuitively
- Unified context menu - can now do (most) operations from any data view
- Symbols and text now drawn in XVI file for Therion export (thanks TopoDroid!)
- Symbol export now optional
- Material Design for dialogs and remaining views
- Lots of miscellaneous minor UI improvements
- Bugfixes for issues including corrupted SVG export and device rotation crash
