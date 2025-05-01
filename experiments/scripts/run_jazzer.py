import os
import subprocess
import multiprocessing
import math
import time
import tempfile
import shutil
import sys

from my_util import *

def single_autofuzz(
        cp: list[str], 
        autofuzz_target: str, 
        run_workdir: str,
        time_limit_seconds: int,
):
    cp_str = ';'.join(cp)
    os.makedirs(run_workdir)
    # run_workdir = str(pathlib.Path(run_workdir).absolute())

    command = [
        'java',
        # JVM arguments
        f'-javaagent:{JACOCO_HOME}/jacocoagent.jar=excludes=com.code_intelligence.jazzer.*',
        '-cp',
        f'{JAZZER_HOME}/jazzer_standalone.jar;{cp_str}',
        'com.code_intelligence.jazzer.Jazzer',
        # Jazzer arguments
        f'--autofuzz={autofuzz_target.replace(" ", "")}',
        '--autofuzz_ignore=java.lang.NullPointerException', # maybe not?
        f'-max_total_time={time_limit_seconds}',
        '--keep_going=0',
    ]
    print(f'running target {autofuzz_target}')
    
    # bc of Windows, subprocess cwd cannot handle long dir names. this is dumb AF but a workaround is easy.
    cwd_dir = tempfile.mkdtemp()
    cwd_ref_file = f'{run_workdir}/jazzer_workdir_ref.txt' 
    with open(cwd_ref_file, 'w') as f:
        f.write(cwd_dir)

    retcode = subprocess.run(
        args=command,
        stdout=open(f'{run_workdir}/stdout.txt', 'w'),
        stderr=open(f'{run_workdir}/stderr.txt', 'w'),
        cwd=cwd_dir,
    ).returncode
    
    shutil.copytree(src=cwd_dir, dst=f'{run_workdir}/jazzer_workdir')
    shutil.rmtree(cwd_dir)
    os.remove(cwd_ref_file)
    
    if retcode != 0:
        # something was wrong, but not a critical error
        print(f'WARN: jazzer returned {retcode} when running {autofuzz_target}', file=sys.stderr)
  
  
def parallel_autofuzz(
    *,
    cp: list[str],
    targets: list[str],
    workdir: str,
    parallelism: int,
    time_per_ranking_seconds: int,
):
    task_waves = math.ceil(len(targets) / parallelism)
    time_per_target_seconds = time_per_ranking_seconds // task_waves
    
    print(f'time per target: {time_per_target_seconds}')
    
    # escape ':' on windows
    single_workdir = lambda target: workdir + '/' + target.replace(':', '_')
    
    # cp, target, workdir, time_limit
    args = [
        [cp, t, single_workdir(t), time_per_target_seconds]
    for t in targets]
    
    start_time = time.time()
    # omfg python can't interrupt pool BRUH
    with multiprocessing.Pool(parallelism) as pool:
        pool.starmap(single_autofuzz, args)
    end_time = time.time()
    print(f'one ranking done, took {end_time - start_time} instead of {time_per_ranking_seconds}')
