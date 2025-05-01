import json
from pathlib import Path
import os

from my_util import *
import run_jazzer
import run_jacoco


def _read_rankings(rankings_file: str) -> list:
    with open(rankings_file, 'r') as f:
        return json.loads(f.read())


def run_one_project(
    rankings_file: str, 
    global_workdir: Path,
    parallelism: int,
    time_per_project_seconds: int,
):
    build_id = rankings_file.removesuffix('.json').rsplit('/')[-1]
    
    dataset_entry = next(entry for entry in dataset if entry['build_id'] == build_id)
    cp: list[str] = dataset_entry['classPath']
    project_workdir = f'{global_workdir}/{build_id}'

    rankings = _read_rankings(rankings_file)
    ranking_workdir_list = []
    time_per_ranking_seconds = time_per_project_seconds // len(rankings)
    print(f'== time per ranking: {time_per_ranking_seconds} ==')
    for ranking in rankings:
        strategy_name: str = ranking['strategyName']
        topK: float = ranking['topK']
        entry_points: list[str] = ranking['entryPoints']
        
        ranking_name = f'{strategy_name}_{topK}'
        ranking_workdir = f'{project_workdir}/{ranking_name}'
        os.makedirs(ranking_workdir)
        ranking_workdir_list.append(ranking_workdir)
        
        print(f'running {ranking_name}, has {len(entry_points)} entry points')
        run_jazzer.parallel_autofuzz(
            cp=cp,
            targets=entry_points,
            workdir=ranking_workdir,
            parallelism=parallelism,
            time_per_ranking_seconds=time_per_ranking_seconds,
        )
    
        os.listdir()
        exec_files = [ f'{ranking_workdir}/{target_dir}/jazzer_workdir/jacoco.exec' 
                      for target_dir in os.listdir(ranking_workdir) ]
        exec_merged = f'{ranking_workdir}/jacoco_merged.exec'
        run_jacoco.jacoco_merge(exec_files, exec_merged)
        # note: only collect cov on target/classes, which is always the first element in dataset
        run_jacoco.jacoco_report(exec_merged, cp[:1], f'{ranking_workdir}/cov_reports')
        
    print(f'== project {build_id} completed ==')


def run_dataset(
    rankings_dir: str,
    workdir: str,
    parallelism: int,
    time_for_all_projects_seconds: int,
):
    rankings_files = [f'{rankings_dir}/{r}' for r in os.listdir(rankings_dir)]
    time_per_project = time_for_all_projects_seconds // len(rankings_files)
    print(f'=== time per project: {time_per_project} ===')
        
    for rankings_file in rankings_files:
        run_one_project(rankings_file, workdir, parallelism, time_per_project)
    
    print("=== dataset experiment complete ===")
