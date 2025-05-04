#  Fuzz-Rank: applying target selection methods to coverage-guided fuzzing

TODO

## How to run experiments

1. `export FUZZ_RANK_HOME=$(pwd)` or its Windows alternative. This will only be used during installation, so you can type absolute paths yourself.
1. `git clone git@github.com:gitbugactions/gitbug-java.git` to clone [GitBug-Java](https://github.com/gitbugactions/gitbug-java) dataset.
1. Make sure `java -version` outputs something sensible.
1. `python3 experiments/scripts/gitbug/gitbug_setup.py $FUZZ_RANK_HOME/gitbug-java $FUZZ_RANK_HOME/dataset false` will clone all dataset projects and try to build them. This step can easily take over an hour.
   - Tip 1: you can avoid building multiple versions of each project if you go to `gitbug-java/data/bugs` and only keep the first line in each file.
   - Tip 2: almost certainly some projects won't build automatically, and that's ok, you can simply ignore them, as 30 built projects will already make the basic experiment take over 3 days.
1. `./gradlew run` will read the dataset from `dataset/benchmarks.json` and generate rankings in `experiments/results/rankings`. The paths can be modified in `app/src/main/kotlin/me/dl33/fuzzrank/Main.kt`
1. Set the following env vars:
   1. `JAZZ_HOME` to absolute path of `jazzer/`. NOTE: jazzer distribution is platform-dependent, the one committed in this repo is for Windows. If in tests all targets fail with a message "native library not found" in `[target_name]/jazzer_workdir/stderr.txt` files, this is the issue.    
   1. `JACOCO_HOME` to absolute path of `jacoco/`
   1. `DATASET_FILE` to absoulte path of `dataset/benchmarks.json` or any other valid dataset file.
1. The scripts in `experiments/scripts/` can now be used to run the experiment.
