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
        time_per_strategy_seconds: int,
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
            time_per_strategy_seconds=time_per_strategy_seconds,
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
    time_per_strategy_seconds: int,
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
        time_per_strategy_seconds=time_per_strategy_seconds,
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
    
    logging.info(f'== finished project {build_id} ==')


def run_dataset(
    rankings_dir: str,
    workdir: str,
    parallelism: int,
    total_real_time_seconds: int,
    dry_run: bool,
):
    rankings_files = [f'{rankings_dir}/{r}' for r in os.listdir(rankings_dir)]
    projects_num = len(rankings_files)
    projects_cnt = 0
    
    total_strategies_num = 0
    entry_points_num_s: list[int] = []
    for ranking_file in rankings_files:
        with open(ranking_file, 'r') as f:
            strategies = json.loads(f.read())
            total_strategies_num += len(strategies)
            for s in strategies:
                entry_points_num_s.append(len(s["entryPoints"]))
    
    time_per_strategy_seconds = total_real_time_seconds * parallelism / total_strategies_num
    
    percentage_of_strategies_with_over_15min_per_entrypoint = sum((
        1 for ep_num in entry_points_num_s if ep_num > 0 and time_per_strategy_seconds / ep_num > 15 * 60
    )) / total_strategies_num
    
    percentage_of_entry_points_with_over_15mins_time = sum((
        ep_num for ep_num in entry_points_num_s if ep_num > 0 and time_per_strategy_seconds / ep_num > 15 * 60
    )) / sum(entry_points_num_s)
    
    logging.info(f"===== experiment starting =====")
    logging.info(f"=== total time: {datetime.timedelta(seconds=total_real_time_seconds)}")
    logging.info(f"=== # strategies in total: {total_strategies_num}")
    logging.info(f"=== time per strategy: {datetime.timedelta(seconds=time_per_strategy_seconds)}")
    logging.info(f"=== % of targets with over 15mins: {percentage_of_entry_points_with_over_15mins_time}")
    logging.info(f"=== % of strategies with over 15mins per target: {percentage_of_strategies_with_over_15min_per_entrypoint}")
    
    if dry_run:
        logging.warning('dry run! stopping')
        return
    
    for rankings_file in rankings_files:
        run_one_project(
            rankings_file=rankings_file, 
            global_workdir=workdir, 
            parallelism=parallelism, 
            time_per_strategy_seconds=time_per_strategy_seconds,
        )
        projects_cnt += 1
        logging.info(f'== global progress: {projects_cnt} / {projects_num} projects')
    
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
    parser.add_argument("total_time_seconds", type=int, help="Approximate total experiment time in seconds")
    parser.add_argument("dry_run", type=bool, help="if true, only calculate time limits")
    
    args = parser.parse_args()
    
    run_dataset(args.rankings_dir, args.workdir, args.parallelism, args.total_time_seconds, args.dry_run)
