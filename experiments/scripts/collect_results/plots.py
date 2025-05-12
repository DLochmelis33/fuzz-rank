#!/usr/bin/env python3
import os
import pandas as pd
import matplotlib.pyplot as plt


def plot_coverage(df, output_path):
    """
    Given a dataframe with columns 'strategy_dir', 'INSTRUCTION_pct', and 'BRANCH_pct',
    generate and save a horizontal bar chart of instruction vs branch coverage.
    """
    # sort by branch coverage percentage
    df = df.sort_values('BRANCH_pct', ascending=True)
    strategies = df['strategy_dir']
    inst = df['INSTRUCTION_pct']
    branch = df['BRANCH_pct']

    y = range(len(df))
    height = 0.4

    fig, ax = plt.subplots(figsize=(10, 6))
    ax.barh([i - height/2 for i in y], inst, height=height, label='Instruction Coverage')
    ax.barh([i + height/2 for i in y], branch, height=height, label='Branch Coverage')

    ax.set_yticks(y)
    ax.set_yticklabels(strategies)
    ax.set_xlabel('Coverage %')
    ax.set_title('Coverage Rates by Strategy (sorted by Branch)')
    ax.legend()

    plt.tight_layout()
    plt.savefig(output_path)
    print(f"Plot saved to {output_path}")
    plt.close(fig)


def generate_plot(input_csv: str, output_img: str):
    """
    Wrapper function: reads coverage data from input_csv and writes a coverage plot to output_img.
    """
    df = pd.read_csv(input_csv)
    plot_coverage(df, output_img)


def all_strategy_per_project_graphs(experiment_dir: str, graphs_dir: str):
    """
    Iterate over each project directory inside `experiment_dir`, read its
    'coverage_summary.csv', and output a coverage plot into
    `graphs_dir/<project_name>_coverage.png`.

    Removes redundant per-project subfolders and ensures figures are closed to prevent memory warnings.
    """
    # ensure output directory exists
    os.makedirs(graphs_dir, exist_ok=True)

    for project_name in os.listdir(experiment_dir):
        project_path = os.path.join(experiment_dir, project_name)
        if not os.path.isdir(project_path):
            continue

        csv_file = os.path.join(project_path, 'coverage_summary.csv')
        if not os.path.isfile(csv_file):
            print(f"Warning: '{csv_file}' not found for project '{project_name}', skipping.")
            continue

        output_img = os.path.join(graphs_dir, f"{project_name}_coverage.png")
        generate_plot(csv_file, output_img)


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description="Generate coverage graphs per project or for a single CSV"
    )
    parser.add_argument('--input_csv', help="Path to a single CSV file to plot.")
    parser.add_argument('--output_img', help="Path to save the single plot (e.g., plot.png)")
    parser.add_argument('--experiment_dir', help="Directory containing project subdirectories with 'coverage_summary.csv'")
    parser.add_argument('--graphs_dir', help="Directory to write per-project graphs into")
    args = parser.parse_args()

    if args.input_csv and args.output_img:
        generate_plot(args.input_csv, args.output_img)
    elif args.experiment_dir and args.graphs_dir:
        all_strategy_per_project_graphs(args.experiment_dir, args.graphs_dir)
    else:
        parser.error("Must specify either --input_csv and --output_img, or --experiment_dir and --graphs_dir.")


if __name__ == '__main__':
    main()