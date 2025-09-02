'''
Handles server-side file reading and data validation
'''

import os
import json
import jsonschema
import datetime

DATA_DIR = 'data'
SCHEMATA_DIR = 'schemata'

DATA_PREFIX = 'data'
BUDGET_PREFIX = 'budget'

DATA_PATH = f"{DATA_DIR}/{DATA_PREFIX}.json"
BUDGET_PATH = f"{DATA_DIR}/{BUDGET_PREFIX}.json"

DATA_SCHEMA = f"{SCHEMATA_DIR}/{DATA_PREFIX}.schema.json"
BUDGET_SCHEMA = f"{SCHEMATA_DIR}/{BUDGET_PREFIX}.schema.json"

DATA_DEFAULT = []
BUDGET_DEFAULT = {"weekly_budget":110.0}

DELETED_DIR = 'deleted'
DELETED_DATA_PATH = f"{DATA_DIR}/{DELETED_DIR}/{DATA_PREFIX}.deleted.json"

def validate_storage(data_file_path, data_schema_path, default_value):
    if not os.path.exists(data_file_path):
        with open(data_file_path, 'w') as f:
            json.dump(default_value, f)
            print(f"File {data_file_path} did not exist, dumping default value.")
        return
    with open(data_file_path, 'r') as f:
        data = json.load(f)
    with open(data_schema_path, 'r') as f:
        schema = json.load(f)
    try:
        jsonschema.validate(instance = data, schema = schema)
        print(f"Data in {data_file_path} validated.")
    except jsonschema.ValidationError as e:
        print(f"Data invalid. Message: {e.message}. Context: {e.context}")
        date = datetime.date.today()
        print(f"Writing {data_file_path} to {data_file_path}.{str(date)} and refreshing {data_file_path} as default.")
        with open(f'{data_file_path}.{str(date)}.log', 'w') as f:
            json.dump(data, f)
        with open(f'{data_file_path}', 'w') as f:
            json.dump(default_value, f)

def ensure_storage():
    validate_storage(DATA_PATH, DATA_SCHEMA, DATA_DEFAULT)
    validate_storage(BUDGET_PATH, BUDGET_SCHEMA, BUDGET_DEFAULT)

def load_data():
    with open(DATA_PATH, 'r') as f:
        return json.load(f)

def load_budget_data():
    with open(BUDGET_PATH, 'r') as f:
        return json.load(f)

def save_data(data):
    with open(DATA_PATH, 'w') as f:
        json.dump(data, f, indent=2)

def save_budget_data(data):
    with open(BUDGET_PATH, 'w') as f:
        json.dump(data, f, indent=2)

def load_deleted():
    with open(DELETED_DATA_PATH, 'r') as f:
        return json.load(f)

def save_deleted(data):
    with open(DELETED_DATA_PATH, 'w') as f:
        json.dump(data, f, indent=2)

def id_exists(data, key):
    return any(entry["id"] == key for entry in data if "id" in entry)