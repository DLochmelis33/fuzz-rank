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
        time_per_target_seconds: int,
):
    cp_str = os.pathsep.join(cp)
    os.makedirs(run_workdir)
    run_workdir = str(pathlib.Path(run_workdir).absolute())
    
    if time_per_target_seconds < 1:
        # print('too little time per target! set to 1 second')
        time_per_target_seconds = 1
    # print(f'running {autofuzz_target} for {time_per_target_seconds} sec')

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
        f'-max_total_time={time_per_target_seconds}',
        '--keep_going=0',
    ]
    
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
  
  
def make_ranking_autofuzz_args(
    *,
    cp: list[str],
    targets: list[str],
    workdir: str,
    time_per_ranking_seconds: int,
) -> list[list[str]]:
    if len(targets) == 0:
        print(f'WARN: empty targets list')
        return []
        
    time_per_target_seconds = time_per_ranking_seconds // len(targets)
    
    # escape ':' on windows
    single_workdir = lambda target: workdir + '/' + target.replace(':', '_')
    
    return [
        # cp, target, workdir, time_limit
        [cp, t, single_workdir(t), time_per_target_seconds]
    for t in targets]
    