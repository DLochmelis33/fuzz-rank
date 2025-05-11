#!/usr/bin/env python3
import argparse
import pandas as pd
import matplotlib.pyplot as plt

def plot_coverage(df, output_path):
    # we'll use the percentage columns
    df = df.sort_values('BRANCH_pct', ascending=True)
    names = df['name']
    inst = df['INSTRUCTION_pct']
    branch = df['BRANCH_pct']

    y = range(len(df))
    height = 0.4

    fig, ax = plt.subplots(figsize=(10, 6))
    ax.barh([i - height/2 for i in y], inst, height=height, label='Instruction Coverage')
    ax.barh([i + height/2 for i in y], branch, height=height, label='Branch Coverage')

    ax.set_yticks(y)
    ax.set_yticklabels(names)
    ax.set_xlabel('Coverage %')
    ax.set_title('Coverage Rates by Strategy')
    ax.legend()

    plt.tight_layout()
    plt.savefig(output_path)
    print(f"Plot saved to {output_path}")

def main():
    parser = argparse.ArgumentParser(description="Plot coverage rates from CSV")
    parser.add_argument('input_csv', help="path to input CSV file")
    parser.add_argument('output_img', help="path to save the output plot (e.g. plot.png)")
    args = parser.parse_args()

    df = pd.read_csv(args.input_csv)
    plot_coverage(df, args.output_img)

if __name__ == '__main__':
    main()
