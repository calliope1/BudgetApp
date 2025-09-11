"""Standalone utilities file for data processing tasks."""

import os
import json

def move_and_reformat(data_path):
    """Moves <PATH>/data.json to <PATH>/expenses/data-<DATE>.json"""

    dir_path = os.path.dirname(data_path)
    new_dir = f"{dir_path}/expenses"
    if not os.path.exists(new_dir):
        os.makedirs(new_dir)

    with open(data_path, 'r') as f:
        data = json.load(f)
    
    for entry in data:
        date = entry["date"]
        
        new_path = f"{new_dir}/data-{date}.json"
        if not os.path.exists(new_path):
            with open(new_path, 'w') as f:
                json.dump([], f)
        
        with open(f"{new_dir}/data-{date}.json", 'r') as f:
            existing_data = json.load(f)
            existing_data.append(entry)
        with open(f"{new_dir}/data-{date}.json", 'w') as f:
            json.dump(existing_data, f, indent=2)

if __name__ == "__main__":
    move_and_reformat("../data.json")