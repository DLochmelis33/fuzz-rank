#!/usr/bin/env python3
import os
import pandas as pd
import matplotlib.pyplot as plt


def load_all_data(experiment_dir):
    """
    Load BRANCH_pct and strategy_dir for each project in experiment_dir.
    Returns a DataFrame with columns: project, strategy_dir, BRANCH_pct.
    """
    records = []
    for project_name in os.listdir(experiment_dir):
        project_path = os.path.join(experiment_dir, project_name)
        if not os.path.isdir(project_path):
            continue

        csv_file = os.path.join(project_path, 'coverage_summary.csv')
        if not os.path.isfile(csv_file):
            print(f"Warning: '{csv_file}' not found, skipping project '{project_name}'")
            continue

        df = pd.read_csv(csv_file)
        if 'strategy_dir' not in df or 'BRANCH_pct' not in df:
            print(f"Warning: missing columns in {csv_file}, skipping")
            continue

        df = df[['strategy_dir', 'BRANCH_pct']].copy()
        df['project'] = project_name
        records.append(df)

    if not records:
        raise ValueError(f"No valid coverage_summary.csv files found in {experiment_dir}")
    return pd.concat(records, ignore_index=True)


def assign_prefix_colors(df):
    """
    Assign a distinct color to each strategy prefix:
    RandomStrategy, SimpleStrategy, SimpleWithSkippingStrategy,
    MinCoverStrategy, MinCoverWeightedStrategy.
    Returns:
      - colors: list of RGB tuples for each row in df
      - prefix_to_rgb: mapping of prefix -> RGB tuple
    """
    prefixes = df['strategy_dir'].str.rsplit(pat='_', n=1).str[0]
    known = [
        'RandomStrategy', 'SimpleStrategy', 'SimpleWithSkippingStrategy',
        'MinCoverStrategy', 'MinCoverWeightedStrategy'
    ]
    cmap = plt.get_cmap('tab10')
    prefix_to_rgb = {pref: cmap(i % cmap.N)[:3] for i, pref in enumerate(known)}
    colors = [prefix_to_rgb.get(pref, (0.5, 0.5, 0.5)) for pref in prefixes]
    return colors, prefix_to_rgb


def plot_all_projects(experiment_dir: str, output_path: str):
    """
    Generate a combined dot chart of branch coverage for all projects.
    Each project is a horizontal line; dots are colored by strategy type only.
    Best strategy per project is annotated at the end of the line.
    Legend shows strategy types, placed above the plot for clarity.
    """
    df = load_all_data(experiment_dir)
    df['suffix'] = df['strategy_dir'].str.rsplit(pat='_', n=1).str[1]
    colors, prefix_to_rgb = assign_prefix_colors(df)

    # map projects to y positions
    projects = sorted(df['project'].unique())
    y_positions = {proj: idx for idx, proj in enumerate(projects)}
    n_projects = len(projects)

    # wider figure and extra top margin
    fig, ax = plt.subplots(figsize=(14, max(2, 0.5 * n_projects)))

    # draw horizontal guide lines
    for y in y_positions.values():
        ax.hlines(y, xmin=0, xmax=1, color='lightgray', linestyle='--', linewidth=0.5, zorder=0)

    # scatter all points
    xs = df['BRANCH_pct']
    ys = df['project'].map(y_positions)
    ax.scatter(xs, ys, color=colors, s=50, zorder=1)

    # annotate best strategy per project
    best = df.loc[df.groupby('project')['BRANCH_pct'].idxmax()]
    for _, row in best.iterrows():
        y = y_positions[row['project']]
        x = row['BRANCH_pct']
        ax.text(x + 0.02 * (1 - x), y, row['strategy_dir'], va='center', ha='left', fontsize=8, zorder=2)

    # formatting
    ax.set_yticks(list(y_positions.values()))
    ax.set_yticklabels(projects)
    ax.set_xlabel('Branch Coverage %')
    ax.set_title('Branch Coverage by Strategy Across Projects')
    ax.set_xlim(0, 1)
    ax.set_ylim(-0.5, n_projects - 0.5)
    for spine in ['left', 'right', 'top']:
        ax.spines[spine].set_visible(False)

    # legend for strategy prefixes above plot
    from matplotlib.lines import Line2D
    handles = [Line2D([0], [0], marker='o', color=rgb, linestyle='None', markersize=6, label=pref)
               for pref, rgb in prefix_to_rgb.items()]
    ax.legend(handles=handles, title='Strategy Type', loc='upper center',
              bbox_to_anchor=(0.5, 1.15), ncol=len(handles), frameon=False)

    # adjust layout to accommodate legend
    fig.tight_layout(rect=[0, 0, 1, 0.90])
    plt.savefig(output_path)
    plt.close(fig)
    print(f"Combined dot chart saved to {output_path}")


def main():
    import argparse
    parser = argparse.ArgumentParser(
        description="Generate combined branch coverage dot chart for all projects"
    )
    parser.add_argument('experiment_dir',
                        help="Directory containing project subdirectories with coverage_summary.csv")
    parser.add_argument('output_img',
                        help="Path to save the combined dot chart (e.g., all_projects_dot.png)")
    args = parser.parse_args()

    plot_all_projects(args.experiment_dir, args.output_img)


if __name__ == '__main__':
    main()
