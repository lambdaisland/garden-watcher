# Unreleased

## Added

## Fixed

## Changed

# 1.0.45 (2021-09-13 / aba992d)

## Added

- Support for `hawk` options

## Changed

- Make `new-garden-watcher` function accept optional argument with `hawk` options.
  Example of using `hawk` options can be found in [Polling Watches](https://github.com/wkf/hawk#polling-watches).

# 1.0.36 (2021-01-14 / 4628d5b)

## Changed

- Update dependencies
  - com.stuartsierra/component {:mvn/version "0.4.0"} -> {:mvn/version "1.0.0"}
  - org.clojure/java.classpath {:mvn/version "0.3.0"} -> {:mvn/version "1.0.0"}
  - garden {:mvn/version "1.3.9"} -> {:mvn/version "1.3.10"}

# 1.0.33 (2021-01-14 / 777c23b)

## Added

- Support for *.cljc files

## Changed

- Make namespace to file path conversion (and vice versa) more robust

# 1.0.27 (2020-10-30 / 634bccc)

## Fixed

- Make classpath detection more robust

# [0.3.5] - 2019-11-09

## Fixed

- Fixed order or `start-garden-watcher!` and  `compile-garden-namespaces`

# [0.3.4] - 2019-11-08

## Changed

- Upgrade dependencies

## Added

- `start-garden-watcher!` / `stop-garden-watcher!`, for people using e.g.
  Integrant instead of Component

# [0.3.3] - 2018-10-15

## Changed

- Upgrade dependencies

# [0.3.2] - 2017-08-14

## Changed
- Remove second println when stopping the component

# [0.3.1] - 2017-03-09

## Fixed
- Added missing dependency: `org.clojure/java.classpath`

# [0.3.0] - 2017-02-10

## Changed
- Made component idempotent

# [0.2.0] - 2016-12-29

## Changed
- Renamed to `lambdaisland/garden-watcher`

# 0.1.0 - 2016-12-29
- Initial version, released as `lambdaisland/garden-reloader`

[Unreleased]: https://github.com/plexus/garden-watcher/compare/v0.3.5...HEAD
[0.3.5]: https://github.com/plexus/garden-watcher/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/plexus/garden-watcher/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/plexus/garden-watcher/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/plexus/garden-watcher/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/plexus/garden-watcher/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/plexus/garden-watcher/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/plexus/garden-watcher/compare/v0.1.0...v0.2.0