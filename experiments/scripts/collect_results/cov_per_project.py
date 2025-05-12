#!/usr/bin/env python3
"""
Summarize JaCoCo coverage reports across multiple strategy subdirectories into a CSV (only instructions and branches with percentages).
Provides reusable functions `summarize_coverage`, `process_experiment`, and a CLI entrypoint that processes an experiment directory.
"""
import os
import xml.etree.ElementTree as ET
import csv
import argparse


def parse_report(xml_path):
    """Parse a JaCoCo report.xml file and return a dict of counters."""
    tree = ET.parse(xml_path)
    root = tree.getroot()
    counters = {}
    for counter in root.findall('counter'):
        t = counter.attrib.get('type')
        covered = int(counter.attrib.get('covered', 0))
        missed = int(counter.attrib.get('missed', 0))
        counters[t] = {'covered': covered, 'missed': missed}
    return counters


def summarize_coverage(project_dir, output_path):
    """
    Walk through each strategy_dir in `project_dir`, parse cov_reports/report.xml,
    and write a CSV summary of INSTRUCTION and BRANCH metrics, including percentages,
    into `output_path`.
    """
    types = ['INSTRUCTION', 'BRANCH']
    headers = [
        'strategy_dir',
        'INSTRUCTION_covered', 'BRANCH_covered',
        'INSTRUCTION_missed',  'BRANCH_missed',
        'INSTRUCTION_pct',     'BRANCH_pct'
    ]

    with open(output_path, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(headers)

        for entry in sorted(os.listdir(project_dir)):
            strategy_path = os.path.join(project_dir, entry)
            if not os.path.isdir(strategy_path):
                continue

            report_path = os.path.join(strategy_path, 'cov_reports', 'report.xml')
            data = {t: {'covered': 0, 'missed': 0} for t in types}

            if os.path.isfile(report_path):
                try:
                    counters = parse_report(report_path)
                    for t in types:
                        if t in counters:
                            data[t] = counters[t]
                except ET.ParseError as e:
                    print(f"Error parsing XML for {entry}: {e}")
            else:
                print(f"Report not found for {entry} at {report_path}")

            row = [entry]
            # covered and missed
            for t in types:
                row.append(data[t]['covered'])
            for t in types:
                row.append(data[t]['missed'])
            # percentages
            for t in types:
                cov = data[t]['covered']
                mis = data[t]['missed']
                total = cov + mis
                pct = cov / total if total > 0 else 0.0
                row.append(pct)

            writer.writerow(row)


def process_experiment(experiment_dir):
    """
    For each project_dir within `experiment_dir`, call `summarize_coverage`,
    writing its summary to `project_dir/coverage_summary.csv`.
    """
    for project in sorted(os.listdir(experiment_dir)):
        project_path = os.path.join(experiment_dir, project)
        if not os.path.isdir(project_path):
            continue
        output_file = os.path.join(project_path, 'coverage_summary.csv')
        print(f"Processing project: {project_path}")
        summarize_coverage(project_path, output_file)
        print(f"Written summary to {output_file}")


def main():
    parser = argparse.ArgumentParser(
        description='Process an experiment directory of projects, summarizing each to CSV.'
    )
    parser.add_argument(
        'experiment_dir',
        help='Directory containing project subdirectories to process'
    )
    args = parser.parse_args()

    process_experiment(args.experiment_dir)


if __name__ == '__main__':
    main()
