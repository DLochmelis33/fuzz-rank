import subprocess
import sys
import pathlib

from my_util import *

def jacoco_merge(exec_files: list[str], output: str):
    command = [
        'java',
        '-jar',
        f'{JACOCO_HOME}/jacococli.jar',
        'merge',
        *exec_files,
        '--destfile',
        output,
        '--quiet',
    ]
    retcode = subprocess.run(command).returncode
    if retcode != 0:
        raise ValueError(f'jacoco returned {retcode}')


def jacoco_report(exec_file: str, project_cp: list[str], output_dir: str):
    os.makedirs(output_dir)
    classpath_args = [
        a
        for c in project_cp
        for a in ['--classfiles', c]
    ]
    command = [
        'java',
        '-jar',
        f'{JACOCO_HOME}/jacococli.jar',
        'report',
        exec_file,
        *classpath_args,
        '--csv',
        f'{output_dir}/report.csv',
        '--html',
        f'{output_dir}/html',
        '--xml',
        f'{output_dir}/report.xml',
        '--quiet',
    ]
    retcode = subprocess.run(command).returncode
    if retcode != 0:
        raise ValueError(f'jacoco returned {retcode}')
