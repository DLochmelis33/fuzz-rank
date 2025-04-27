#!/usr/bin/env python3

import dataclasses
import re
from dataclasses import dataclass

import pandas as pd


@dataclass
class LibfuzzerLogEntry:
    execNr: int
    cov: int
    ft: int
    crashes: int


@dataclass
class CrashEntry:
    execNr: int
    inputPath: str


# 944	NEW    cov: 57 ft: 58 corp: 10/20b lim: 6 exec/s: 0 rss: 718Mb L: 2/3 MS: 2 ChangeBinInt-Custom-
# 957	REDUCE cov: 63 ft: 64 corp: 11/26b lim: 6 exec/s: 0 rss: 718Mb L: 6/6 MS: 6 CMP-Custom-CrossOver-Custom-ShuffleBytes-Custom- DE: "\377\377\377\000"-

def libfuzzer_output_to_csv(filename, duration) -> (str, str):
    lines = [LibfuzzerLogEntry(execNr=0, cov=0, ft=0, crashes=0)]
    crashes = list[CrashEntry]()
    duration_seconds = parse_duration(duration)

    with open(filename, "r") as file:  # read libfuzzer output
        for line in file:  # and process it line by line
            tokens = line.split()  # split line into tokens
            if len(tokens) < 2: continue

            if tokens[0].startswith('artifact_prefix='):
                crashes.append(CrashEntry(lines[-1].execNr, tokens[5]))
                continue

            elif tokens[0].startswith('#') and len(tokens) >= 14 and (
                tokens[1] == 'NEW' or tokens[1] == 'REDUCE' or tokens[1] == 'pulse'):
                execs = int(tokens[0][1:])
                cov_blks = int(tokens[3])
                cov_ft = int(tokens[5])
                lines.append(LibfuzzerLogEntry(execNr=execs, cov=cov_blks, ft=cov_ft, crashes=len(crashes)))

            # if tokens[0].startswith("DEDUP_TOKEN"):
            #     crashes.append(coverage[-1][0])
    stats = pd.DataFrame.from_records(map(lambda x: dataclasses.asdict(x), lines))
    crashes = pd.DataFrame.from_records(map(lambda x: dataclasses.asdict(x), crashes))
    max_exec_nr = stats['execNr'].max()

    if max_exec_nr <= 0:
        print(f"Error: max execNr is 0 for {filename}")
        return stats.to_csv(index=False), crashes.to_csv(index=False)

    stats['timestamp'] = (stats['execNr'] * duration_seconds / max_exec_nr).astype(int)
    if crashes.size > 0:
        crashes['timestamp'] = (crashes['execNr'] * duration_seconds / max_exec_nr).astype(int)

    return stats.to_csv(index=False), crashes.to_csv(index=False)


def parse_duration(duration):
    match = re.match(r'^((?P<hours>\d+)h)?((?P<minutes>\d+)m)?((?P<seconds>\d+)s)?$', duration)
    if not match:
        raise ValueError(f"Invalid duration format: {duration}")

    parts = match.groupdict(default='0')
    return int(parts['hours']) * 3600 + int(parts['minutes']) * 60 + int(parts['seconds'])
