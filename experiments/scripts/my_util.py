import os
import json
import pathlib

JAZZER_HOME = os.environ['JAZZ_HOME']
JACOCO_HOME = os.environ['JACOCO_HOME']
DATASET_FILE = os.environ['DATASET_FILE']

dataset = {'oops': 'you forgot to init dataset'}

def init_dataset():
    global dataset 
    with open(DATASET_FILE, 'r') as f:
        dataset = json.loads(f.read())

init_dataset()
