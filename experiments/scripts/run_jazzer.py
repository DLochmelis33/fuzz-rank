import os
import subprocess
import tempfile
import shutil
import sys
import logging
import hashlib

from my_util import *

def single_autofuzz(
        cp: list[str], 
        autofuzz_target: str, 
        run_workdir: str,
        time_per_target_seconds: int,
):
    cp_str = os.pathsep.join(cp)
    run_workdir = str(pathlib.Path(run_workdir).absolute())
    os.makedirs(run_workdir)
    
    with open(f'{run_workdir}/target_name.txt', 'w') as f:
        f.write(autofuzz_target)
    
    if time_per_target_seconds < 1:
        time_per_target_seconds = 1
    # logging.debug(f'running {autofuzz_target} for {time_per_target_seconds} sec')

    command = [
        'java',
        # JVM arguments
        f'-javaagent:{JACOCO_HOME}/jacocoagent.jar=excludes=com.code_intelligence.jazzer.*',
        '-cp',
        f'{JAZZER_HOME}/jazzer_standalone.jar{os.pathsep}{cp_str}',
        'com.code_intelligence.jazzer.Jazzer',
        # Jazzer arguments
        f'--autofuzz={autofuzz_target.replace(" ", "").replace("<init>", "new")}',
        '--autofuzz_ignore=java.lang.NullPointerException', # maybe not?
        f'-max_total_time={time_per_target_seconds}',
        '--keep_going=1000', # too many exceptions can flood disk space
    ]
    
    # bc of Windows, subprocess cwd cannot handle long dir names. this is dumb AF but a workaround is easy.
    cwd_dir = tempfile.mkdtemp()
    cwd_ref_file = f'{run_workdir}/jazzer_workdir_ref.txt' 
    with open(cwd_ref_file, 'w') as f:
        f.write(cwd_dir)

    retcode = subprocess.run(
        args=command,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        cwd=cwd_dir,
    ).returncode
    
    shutil.copytree(src=cwd_dir, dst=f'{run_workdir}/jazzer_workdir')
    shutil.rmtree(cwd_dir)
    os.remove(cwd_ref_file)
    
    if retcode != 0:
        # something was wrong, but not a critical error
        logging.warning(f'jazzer returned {retcode} when running {run_workdir}')
  
  
def make_ranking_autofuzz_args(
    *,
    cp: list[str],
    targets: list[str],
    workdir: str,
    time_per_strategy_seconds: int,
) -> list[list[str]]:
    if len(targets) == 0:
        logging.warning(f'empty targets list')
        return []
        
    time_per_target_seconds = time_per_strategy_seconds // len(targets)
    
    def get_workdir_name(target: str) -> str:
        if len(target) < 100:
            return workdir + '/' + target.replace(':', '_')
        else:
            return workdir + '/' + hashlib.sha256(target.encode()).hexdigest()
    
    return [
        # cp, target, workdir, time_limit
        [cp, t, get_workdir_name(t), time_per_target_seconds]
    for t in targets]
    