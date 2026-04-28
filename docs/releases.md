# 2026-04-28 1.11.2
- Fix a bug causing cross-sections to be initially hidden

# 2026-04-19 1.11.1
- Fix a bug causing symbols to be hidden
- Fix a bug causing deletion radius to be badly calculated

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

# 2025-11-23 1.9.0
- Support for new instruments: the DiscoX, and (very experimentally) CavwayX1
- Improved support for FCL (thanks Michael Glazer)
- Improved leg detection algorithm (thanks Thomas Holder)
- Better formatting and performance for Data Table view
- New SVG export options
- Better colours, especially in dark mode
- Better use of Material Design (i.e. generally looks nicer)
- Menu reorganisation
- Instrument connection quick-action in menu and better instrument state management
- Fullscreen mode toggle
- Miscellaneous bugfixes and other minor UI improvements

# 2024-09-19 1.8.3
- French translation
- FCL support
- Minor bugfixes

# 2024-04-09 1.8.2
- Fix for default Therion export thconfig file possibly refusing to build

# 2024-03-21 1.8.1
- German translation

# 2024-03-02 1.8.0
- Support for symbols
- Export improvements

# 2023-09-19 1.7.0
- Google-enforced change to file handling: users now have to select/create folders or files themselves to open or save things
- Google-enforced Android version increase to SDK 22 (Android 5/Lollipop)
- The file handling changes described above gives users more control over project structure
- Supports the new SAP6 (thanks Phil Underwood for the code)
- Support for the new DistoX BLE (thanks Siwei Tian)
- Bluetooth should now work properly on modern phones
- First attempt at the oft-requested dark mode
- Better scaling of text with bigger screens
- Autosave now tracks more stuff
- Minor performance improvements
- Most text now in XML, paving the way for translations soon
- Minor text improvements
- Minor GUI improvements (thanks Olly Legg for a PR)
- Several minor bug fixes
- Code improvements, e.g. preference code rewritten
- A lot of Google-enforced admin changes
- Git: "master" branch is now "main"
- Github: automated tests now running on check-in to main

# 2021-07-18 1.6.0
- Big performance improvements (thanks Dan Workman)
- Rewrite of the comms logic to better handle different instruments
- Fix a bug with newest versions of Android not being able to access files
- Survey files will now be saved with whitespace instead of just a big text blob
- Most doubles in the code now replaced with floats to save memory and reduce messy casts
- Big tidy up of the code with most lint warnings fixed
- Menu entries tidied up and dev menu now hidden behind a settings flag
- Some instrumented (higher level) tests created, which should start improving stability going forward

# 2021-03-30 1.5.0
- Improvements to text autoscaling

# 2020-01-18 1.4.2
- Fix problems with reversed legs
- Allow partial deletion of sketch lines

# 2020-01-13 1.4.0
- Data entries are now in chronological order
- Made SexyTopo more stable in a bunch of different places
- The station notification buzz should keep working now (hopefully)
- Calibration screen no longer times out
- Several minor UI improvements
- Increase minimum SDK version

# 2019-08-06 1.3.6
- Fixed problems importing PocketTopo files
- Add basic ability to jump between stations in different views
- Minor performance improvement for Table

# 2019-07-31 1.3.5
- Bugfixes

# 2019-07-08 1.3.4
- Fixed linked survey transparency bug
- Some more performance improvements
- Minor change to Therion file layout
- Graphical display of hot corners

# 2019-06-12 1.3.2
- Performance improvements

# 2019-06-09 1.3.1
- Fix major Therion export bug
- Simple SVG exporter

# 2019-06-04 1.3.0
- More performance improvements when updating with new info (more to come)
- Stations with X-sections now have an indicator showing their angle and direction
- Handle any X-section error more gracefully
- Less confusing warning when deleting stations
- System log now starts off at the latest (bottom) entry
- Slight rejigging of menus
- Fix a rare null pointer bug
- Add extra a couple of extra stats to stats page
- Working with the new Shetland Attack Pony (thanks Phil Underwood)
