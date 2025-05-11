#!/usr/bin/env python3
"""
Summarize JaCoCo coverage reports across multiple subdirectories into a CSV (only instructions and branches with percentages).
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

def main():
    parser = argparse.ArgumentParser(
        description='Summarize JaCoCo coverage reports into CSV (instructions & branches).'
    )
    parser.add_argument(
        'root_dir',
        help='Root directory containing subdirectories with cov_reports/report.xml'
    )
    parser.add_argument(
        '-o', '--output',
        default='coverage_summary.csv',
        help='Output CSV file path'
    )
    args = parser.parse_args()

    types = ['INSTRUCTION', 'BRANCH']
    headers = [
        'name',
        'INSTRUCTION_covered', 'BRANCH_covered',
        'INSTRUCTION_missed', 'BRANCH_missed',
        'INSTRUCTION_rate', 'BRANCH_rate'
    ]

    with open(args.output, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(headers)

        for entry in sorted(os.listdir(args.root_dir)):
            entry_path = os.path.join(args.root_dir, entry)
            if not os.path.isdir(entry_path):
                continue
            report_path = os.path.join(entry_path, 'cov_reports', 'report.xml')
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

    print(f"Summary written to {args.output}")

if __name__ == '__main__':
    main()
