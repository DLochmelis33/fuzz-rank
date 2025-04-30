import os
import json

JAZZER_HOME = os.environ['JAZZ_HOME']
JACOCO_HOME = os.environ['JACOCO_HOME']
DATASET_HOME = os.environ['DATASET_HOME']

dataset = {'oops': 'you forgot to init dataset'}

def init_dataset():
    with open(f'{DATASET_HOME}/benchmarks.json', 'r') as f:
        global dataset 
        dataset = json.loads(f.read())

init_dataset()
