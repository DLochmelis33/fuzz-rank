import argparse
import json
from pathlib import Path
import os
import multiprocessing
import datetime
import logging

from my_util import *
import run_jazzer
import run_jacoco


def _read_rankings(rankings_file: str) -> list:
    with open(rankings_file, 'r') as f:
        return json.loads(f.read())


def _make_one_project_tasks(
        rankings: list,
        project_workdir: str,
        cp: list[str],
        time_per_ranking_seconds: int,
) -> list[list[str]]:
    
    tasks_list = []
    for ranking in rankings:
        # WARNING: code duplication
        strategy_name: str = ranking['strategyName']
        topK: float = ranking['topK']
        entry_points: list[str] = ranking['entryPoints']
        
        ranking_name = f'{strategy_name}_{topK}'
        ranking_workdir = f'{project_workdir}/{ranking_name}'
        os.makedirs(ranking_workdir)
        
        single_target_tasks = run_jazzer.make_ranking_autofuzz_args(
            cp=cp,
            targets=entry_points,
            workdir=ranking_workdir,
            time_per_ranking_seconds=time_per_ranking_seconds,
        )
        tasks_list.extend(single_target_tasks)
    return tasks_list


def _merge_stats(
    rankings: list,
    project_workdir: str,
    cp: list[str],
):
    for ranking in rankings:
        # WARNING: code duplication
        strategy_name: str = ranking['strategyName']
        topK: float = ranking['topK']
        
        ranking_name = f'{strategy_name}_{topK}'
        ranking_workdir = f'{project_workdir}/{ranking_name}'
        
        exec_files = [ f'{ranking_workdir}/{target_dir}/jazzer_workdir/jacoco.exec' 
                      for target_dir in os.listdir(ranking_workdir) ]
        exec_merged = f'{ranking_workdir}/jacoco_merged.exec'
        run_jacoco.jacoco_merge(exec_files, exec_merged)
        # note: only collect cov on `target/classes`, which is always the first element in dataset
        run_jacoco.jacoco_report(exec_merged, cp[:1], f'{ranking_workdir}/cov_reports')


def run_one_project(
    rankings_file: str, 
    global_workdir: Path,
    parallelism: int,
    time_per_ranking_seconds: int,
):
    build_id = rankings_file.removesuffix('.json').rsplit('/')[-1]
    logging.info(f'== starting project {build_id} ==')
    
    dataset_entry = next(entry for entry in dataset if entry['build_id'] == build_id)
    cp: list[str] = dataset_entry['classPath']
    project_workdir = f'{global_workdir}/{build_id}'

    rankings = _read_rankings(rankings_file)
    
    tasks = _make_one_project_tasks(
        rankings=rankings,
        project_workdir=project_workdir,
        cp=cp,
        time_per_ranking_seconds=time_per_ranking_seconds,
    )

    # runs all tasks in the pool, wasting as little time as possible
    with multiprocessing.Pool(parallelism) as pool:
        pool.starmap(run_jazzer.single_autofuzz, tasks)
    
    logging.info("finished fuzzing, merging jacoco reports")
    _merge_stats(
        rankings=rankings, 
        project_workdir=project_workdir, 
        cp=cp
    )
    
    logging.info(f'== project {build_id} ==')


def run_dataset(
    rankings_dir: str,
    workdir: str,
    parallelism: int,
    time_per_ranking_seconds: int,
):
    rankings_files = [f'{rankings_dir}/{r}' for r in os.listdir(rankings_dir)]
    projects_num = len(rankings_files)
    projects_cnt = 0
    
    estimate_time_per_project = datetime.timedelta(
        seconds=time_per_ranking_seconds * 20 / parallelism
    )
    estimate_time_total = estimate_time_per_project * projects_num
    logging.info(f"===== experiment starting at {datetime.datetime.now()} =====")
    logging.info(f"=== time per ranking: {time_per_ranking_seconds}")
    logging.info(f"=== estimated time per project: {estimate_time_per_project}")
    logging.info(f"=== estimated total time: {estimate_time_total}")
        
    for rankings_file in rankings_files:
        run_one_project(
            rankings_file=rankings_file, 
            global_workdir=workdir, 
            parallelism=parallelism, 
            time_per_ranking_seconds=time_per_ranking_seconds,
        )
        projects_cnt += 1
        logging.info(f'== progress: {projects_cnt} / {projects_num} projects')
    logging.info("===== experiment end =====")
    
    
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s: %(message)s"
)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="run main experiment")
    parser.add_argument("rankings_dir", type=str, help="Path to the rankings directory")
    parser.add_argument("workdir", type=str, help="Working directory")
    parser.add_argument("parallelism", type=int, help="Number of parallel processes to run")
    parser.add_argument("time_per_ranking_seconds", type=int, help="Time per ranking in seconds")
    
    args = parser.parse_args()
    
    run_dataset(args.rankings_dir, args.workdir, args.parallelism, args.time_per_ranking_seconds)
