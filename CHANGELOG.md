# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

<!-- ## [Unreleased] -->
<!-- ### Added -->
<!-- ### Changed -->
<!-- ### Fixed -->

## Unreleased

### Changed

- Upgrade dependencies

### Added

- `start-garden-watcher!` / `stop-garden-watcher!`, for people using e.g.
  Integrant instead of Component

## [0.3.2] - 2017-08-14
### Changed
- Remove second println when stopping the component

## [0.3.1] - 2017-03-09
### Fixed
- Added missing dependency: `org.clojure/java.classpath`

## [0.3.0] - 2017-02-10
### Changed
- Made component idempotent

## [0.2.0] - 2016-12-29
### Changed
- Renamed to `lambdaisland/garden-watcher`

## 0.1.0 - 2016-12-29
- Initial version, released as `lambdaisland/garden-reloader`

[Unreleased]: https://github.com/plexus/garden-watcher/compare/v0.3.2...HEAD
[0.3.1]: https://github.com/plexus/garden-watcher/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/plexus/garden-watcher/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/plexus/garden-watcher/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/plexus/garden-watcher/compare/v0.1.0...v0.2.0
