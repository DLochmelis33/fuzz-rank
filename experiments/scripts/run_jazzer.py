import os
import subprocess
import shutil
import multiprocessing
import itertools

JAZZER_HOME = os.environ['JAZZ_HOME']
JACOCO_HOME = os.environ['JACOCO_HOME']


def single_autofuzz(
        cp: list[str], 
        autofuzz_target: str, 
        run_workdir: str,
        time_limit_seconds: int,
):
    cp_str = ';'.join(cp) # + PATH_SEP + f'{JAZZER_HOME}/jazzer_standalone.jar'
    if not os.path.exists(run_workdir):
        os.makedirs(run_workdir)
    
    command = [
        'java',
        # JVM arguments
        f'-javaagent:{JACOCO_HOME}/jacocoagent.jar=excludes=com.code_intelligence.jazzer.*',
        '-cp',
        f'{JAZZER_HOME}/jazzer_standalone.jar;{cp_str}',
        'com.code_intelligence.jazzer.Jazzer',
        # Jazzer arguments
        f'--autofuzz={autofuzz_target}',
        '--autofuzz_ignore=java.lang.NullPointerException', # maybe not?
        f'-max_total_time={time_limit_seconds}',
        '--keep_going=0',
    ]
    print(f'running target {autofuzz_target}')

    subprocess.run(
        args=command,
        stdout=open(f'{run_workdir}/stdout.txt', 'w'),
        stderr=open(f'{run_workdir}/stderr.txt', 'w'),
        cwd=run_workdir,
    )
  
  
def parallel_autofuzz(
    *,
    cp: list[str],
    targets: list[str],
    workdir: str,
    parallelism: int,
    total_time_limit_seconds: int,
):
    single_time_limit_seconds = int(total_time_limit_seconds / len(targets) * parallelism)
    
    # escape ':' on windows
    single_workdir = lambda target: workdir + '/' + target.replace(':', '_')
    
    # cp, target, workdir, time_limit
    args = [
        [cp, t, single_workdir(t), single_time_limit_seconds]
    for t in targets]
    
    with multiprocessing.Pool(processes=parallelism) as pool:
        pool.starmap(single_autofuzz, args)
