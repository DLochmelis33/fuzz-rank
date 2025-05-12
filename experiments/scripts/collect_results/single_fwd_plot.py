#!/usr/bin/env python3
import os
import pandas as pd
import matplotlib.pyplot as plt
import colorsys


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


def parse_strategy_colors(df):
    """
    Assign a base hue to each known strategy prefix and adjust brightness by suffix (top-K value).
    Returns:
      - colors: list of RGB tuples corresponding to each row in df
      - prefix_to_rgb: mapping of prefix -> base RGB (mid brightness)
      - suffixes: sorted list of unique suffix strings
    """
    # known prefixes
    known_prefixes = [
        'RandomStrategy',
        'SimpleStrategy',
        'SimpleWithSkippingStrategy',
        'MinCoverStrategy',
        'MinCoverWeightedStrategy'
    ]
    # split prefix and suffix
    parts = df['strategy_dir'].str.rsplit('_', n=1)
    prefixes = parts.str[0]
    suffixes = parts.str[1]

    # choose base colors for prefixes from tab10
    base_cmap = plt.get_cmap('tab10')
    prefix_to_rgb = {
        pref: base_cmap(i % base_cmap.N)[:3]
        for i, pref in enumerate(known_prefixes)
    }

    # sorted suffix values
    unique_suffixes = sorted(suffixes.unique(), key=lambda x: float(x))

    # brightness range for shading
    min_v, max_v = 0.5, 1.0

    # build colors list
    colors = []
    for pref, suf in zip(prefixes, suffixes):
        base_rgb = prefix_to_rgb.get(pref, (0.5, 0.5, 0.5))
        # convert to HSV
        h, s, v0 = colorsys.rgb_to_hsv(*base_rgb)
        # normalized position of this suffix
        idx = unique_suffixes.index(suf)
        t = idx / (len(unique_suffixes) - 1) if len(unique_suffixes) > 1 else 0.5
        # compute brightness
        v = min_v + t * (max_v - min_v)
        # back to RGB
        rgb = colorsys.hsv_to_rgb(h, s, v)
        colors.append(rgb)

    return colors, prefix_to_rgb, unique_suffixes


def plot_all_projects(experiment_dir: str, output_path: str):
    """
    Generate a combined dot chart of branch coverage for all projects.
    Each project is a horizontal line; dots are strategies colored by prefix hue and suffix brightness.
    Best strategy per project is annotated at the end of the line.
    Legend shows strategy types (prefix colors) and top-K shading (suffix values).
    """
    # load and prepare data
    df = load_all_data(experiment_dir)
    df['suffix'] = df['strategy_dir'].str.rsplit('_', n=1).str[1]
    colors, prefix_to_rgb, suffixes = parse_strategy_colors(df)

    # map projects to y positions
    projects = sorted(df['project'].unique())
    y_positions = {proj: idx for idx, proj in enumerate(projects)}
    n_projects = len(projects)

    fig, ax = plt.subplots(figsize=(10, max(2, 0.5 * n_projects)))

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

    # legend for strategy prefixes
    from matplotlib.lines import Line2D
    prefix_handles = []
    for pref, rgb in prefix_to_rgb.items():
        prefix_handles.append(Line2D([0], [0], marker='o', color=rgb,
                                     linestyle='None', markersize=8, label=pref))
    l1 = ax.legend(handles=prefix_handles, title='Strategy Type', bbox_to_anchor=(1.02, 1), loc='upper left')
    ax.add_artist(l1)

    # legend for top-K suffix shading
    suffix_handles = []
    # pick a neutral prefix for demonstration (e.g. SimpleStrategy)
    demo_rgb = prefix_to_rgb.get('SimpleStrategy', (0.5, 0.5, 0.5))
    h_demo, s_demo, _ = colorsys.rgb_to_hsv(*demo_rgb)
    for suf in suffixes:
        idx = suffixes.index(suf)
        t = idx / (len(suffixes) - 1) if len(suffixes) > 1 else 0.5
        v = 0.5 + t * 0.5
        shade_rgb = colorsys.hsv_to_rgb(h_demo, s_demo, v)
        suffix_handles.append(Line2D([0], [0], marker='o', color=shade_rgb,
                                     linestyle='None', markersize=6, label=suf))
    ax.legend(handles=suffix_handles, title='Top-K Value', bbox_to_anchor=(1.02, 0.5), loc='upper left')

    plt.tight_layout()
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
