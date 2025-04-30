import os
import subprocess
import multiprocessing
import math
import time
import tempfile

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
    
    # bc of Windows, subprocess cmd cannot handle long dir names. this is dumb AF but a workaround is easy.
    cwd_dir = tempfile.mkdtemp()
    with open(f'{run_workdir}/jazzer_workdir_ref.txt', 'w') as f:
        f.write(cwd_dir)

    subprocess.run(
        args=command,
        stdout=open(f'{run_workdir}/stdout.txt', 'w'),
        stderr=open(f'{run_workdir}/stderr.txt', 'w'),
        cwd=cwd_dir,
    )
  
  
def parallel_autofuzz(
    *,
    cp: list[str],
    targets: list[str],
    workdir: str,
    parallelism: int,
    total_time_limit_seconds: int,
):
    task_waves = math.ceil(len(targets) / parallelism)
    single_time_limit_seconds = total_time_limit_seconds // task_waves
    
    print(f'time per target: {single_time_limit_seconds}')
    
    # escape ':' on windows
    single_workdir = lambda target: workdir + '/' + target.replace(':', '_')
    
    # cp, target, workdir, time_limit
    args = [
        [cp, t, single_workdir(t), single_time_limit_seconds]
    for t in targets]
    
    start_time = time.time()
    # omfg python can't interrupt pool BRUH
    with multiprocessing.Pool(parallelism) as pool:
        pool.starmap(single_autofuzz, args)
    end_time = time.time()
    print(f'one ranking done, took {end_time - start_time} instead of {total_time_limit_seconds}')
