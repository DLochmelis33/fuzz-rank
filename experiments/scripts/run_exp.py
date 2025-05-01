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
    project_workdir: Path,
    parallelism: int,
    total_time_limit_seconds: int,
):
    build_id = rankings_file.removesuffix('.json').rsplit('/')[-1]
    
    dataset_entry = next(entry for entry in dataset if entry['build_id'] == build_id)
    cp: list[str] = dataset_entry['classPath']

    rankings = _read_rankings(rankings_file)
    ranking_workdir_list = []
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
            total_time_limit_seconds=total_time_limit_seconds,
        )
    
        os.listdir()
        exec_files = [ f'{ranking_workdir}/{target_dir}/jazzer_workdir/jacoco.exec' 
                      for target_dir in os.listdir(ranking_workdir) ]
        run_jacoco.jacoco_merge(exec_files, f'{ranking_workdir}/jacoco_merged.exec')
