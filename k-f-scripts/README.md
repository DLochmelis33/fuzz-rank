### Credit: https://github.com/JetBrains-Research/kotlinx.fuzz/tree/main/kotlinx.fuzz.examples/scripts 

Python dependencies: `pandas`, `shutil`

## `run-experiment` 

Complete scripts to perform an experiment. Run with `-h` to see it's arguments.

What is does:

1. runs `run-targets`
2. copies `$PROJECT_DIR/src/test/resources`
3. runs `parse-logs`
4. runs `overall-stats`
5. runs `jacoco merge`
6. runs `jacoco report`


## `run-targets` 

Allows to run multiple targets (incl. in parallel) and collecting jazzer logs.

## `parse-logs`

Parses jazzer logs to csvs with fuzzing progress and csvs with findings.

## `overall-stats`

Parses csvs from `parse-logs` to single csv with overall info about each target

## `plot` 

Parses csv from `parse-logs` and draws some charts

