#!/usr/bin/env python3
import os
import pandas as pd
import matplotlib.pyplot as plt


def load_all_data(experiment_dir):
    """
    Load BRANCH_pct for each project and strategy in experiment_dir.
    Returns a DataFrame with columns: strategy, project, BRANCH_pct.
    """
    records = []
    for project in os.listdir(experiment_dir):
        proj_path = os.path.join(experiment_dir, project)
        if not os.path.isdir(proj_path):
            continue
        csv_path = os.path.join(proj_path, 'coverage_summary.csv')
        if not os.path.isfile(csv_path):
            print(f"Warning: no coverage_summary.csv for project '{project}', skipping")
            continue
        df = pd.read_csv(csv_path)
        if 'strategy_dir' not in df or 'BRANCH_pct' not in df:
            print(f"Warning: missing required columns in {csv_path}, skipping")
            continue
        temp = df[['strategy_dir', 'BRANCH_pct']].copy()
        temp.columns = ['strategy', 'BRANCH_pct']
        temp['project'] = project
        records.append(temp)
    if not records:
        raise ValueError(f"No valid coverage_summary.csv found under {experiment_dir}")
    return pd.concat(records, ignore_index=True)


def compute_strategy_stats(df):
    """
    Given DataFrame with ['strategy','project','BRANCH_pct'],
    compute mean coverage and project lists per strategy.
    Returns a DataFrame with index=strategy and columns:
      - projects: list of (project, BRANCH_pct)
      - mean_pct: average BRANCH_pct
    Sorted by mean_pct ascending so best strategy appears at the top of the chart.
    """
    stats = []
    for strat, group in df.groupby('strategy'):
        mean_pct = group['BRANCH_pct'].mean()
        proj_list = list(zip(group['project'], group['BRANCH_pct']))
        stats.append({'strategy': strat, 'mean_pct': mean_pct, 'projects': proj_list})
    df_stats = pd.DataFrame(stats).set_index('strategy')
    return df_stats.sort_values('mean_pct')


def plot_strategy_rank(df_stats, output_path):
    """
    Create a dot chart with one horizontal line per strategy,
    dots for each project at the BRANCH_pct value, and
    mean_pct annotated to the right.
    Strategies are sorted by mean_pct, best on top.
    Dots are all blue.
    """
    strategies = df_stats.index.tolist()
    # map strategies to y positions: lowest mean at bottom, highest at top
    y_positions = {s: i for i, s in enumerate(strategies)}
    n = len(strategies)

    fig, ax = plt.subplots(figsize=(10, max(2, 0.5 * n)))

    # draw horizontal baseline for each strategy
    for strat, y in y_positions.items():
        ax.hlines(y, xmin=0, xmax=1, color='lightgray', linewidth=0.5, zorder=0)

    # plot dots in blue
    for strat, row in df_stats.iterrows():
        y = y_positions[strat]
        for proj, pct in row['projects']:
            ax.scatter(pct, y, color='blue', s=30, zorder=1)

        # annotate mean coverage to the right
        ax.text(1.02, y, f"{row['mean_pct']:.2f}", va='center', ha='left', fontsize=8, zorder=2)

    # styling
    ax.set_yticks(list(y_positions.values()))
    ax.set_yticklabels(strategies)
    ax.set_xlabel('Branch Coverage %')
    ax.set_title('Project Performance per Strategy (Branch Coverage)')
    ax.set_xlim(0, 1.0)
    ax.set_ylim(-0.5, n - 0.5)
    for spine in ['right', 'top']:
        ax.spines[spine].set_visible(False)

    plt.tight_layout()
    plt.savefig(output_path)
    plt.close(fig)
    print(f"Strategy ranking chart saved to {output_path}")


def main():
    import argparse
    parser = argparse.ArgumentParser(
        description="Generate strategy-centric ranking dot chart across projects"
    )
    parser.add_argument('experiment_dir', help='Path to experiments root dir')
    parser.add_argument('output_img', help='Path to save the ranking dot chart')
    args = parser.parse_args()

    df = load_all_data(args.experiment_dir)
    stats = compute_strategy_stats(df)
    plot_strategy_rank(stats, args.output_img)


if __name__ == '__main__':
    main()
