import os
import json
import pathlib

JAZZER_HOME = os.environ['JAZZ_HOME']
JACOCO_HOME = os.environ['JACOCO_HOME']
DATASET_FILE = os.environ['DATASET_FILE']

dataset = {'oops': 'you forgot to init dataset'}


def _init_dataset():
    global dataset 
    with open(DATASET_FILE, 'r') as f:
        dataset = json.loads(f.read())


def _verify_envvars():
    if not os.path.isfile(f'{JAZZER_HOME}/jazzer_standalone.jar'):
        raise ValueError(f'bad JAZZ_HOME: "{JAZZER_HOME}"')
    if not os.path.isfile(f'{JACOCO_HOME}/jacocoagent.jar'):
        raise ValueError(f'bad JACOCO_HOME: "{JACOCO_HOME}"')


_init_dataset()
_verify_envvars()
