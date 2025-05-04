import argparse
import json
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(
        description="Count entryPoints per strategy in all JSON files in a directory."
    )
    parser.add_argument("input_dir", type=Path, help="Directory containing .json strategy files")
    parser.add_argument("output_file", type=Path, help="File to write the list of counts to")
    args = parser.parse_args()

    counts = []
    # iterate all .json files in the given directory
    for json_path in sorted(args.input_dir.iterdir()):
        if json_path.suffix.lower() != ".json":
            continue
        with json_path.open() as f:
            data = json.load(f)  # expect a list of strategy objects :contentReference[oaicite:0]{index=0}:contentReference[oaicite:1]{index=1}
        # for each strategy object, count its entryPoints
        for strat in data:
            eps = strat.get("entryPoints", [])
            counts.append(len(eps))

    # write one count per line
    with args.output_file.open("w") as out:
        for c in counts:
            out.write(f"{c}\n")

if __name__ == "__main__":
    main()
