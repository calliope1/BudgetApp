'''
Handles server-side file reading and data validation
'''

import os
import json
import jsonschema
import datetime

DATA_DIR = 'server/data'
SCHEMATA_DIR = 'server/schemata'

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

def date_to_isostring(d):
    """Returns YYYY-MM-DD string from date d"""
    buffer_year = '' + ('0' if d.year < 10 else '') + ('0' if d.year < 100 else '') + ('0' if d.year < 1000 else '')
    buffer_month = '0' if d.month < 10 else ''
    buffer_day = '0' if d.day < 10 else ''
    return f"{buffer_year}{str(d.year)}-{buffer_month}{str(d.month)}-{buffer_day}{str(d.day)}"

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
    """Validate JSONs in data path
    
    Validates the data.json and budget.json files, as well as all expenses/data-<date>.json files.
    If any are found to be corrupt, they are backed up and replaced with DATA_DEFAULT/BUDGET_DEFAULT files.
    If either data.json or budget.json are found to be missing, they are instantiated with DATA_DEFAULT/BUDGET_DEFAULT files.
    """
    validate_storage(DATA_PATH, DATA_SCHEMA, DATA_DEFAULT)
    validate_storage(BUDGET_PATH, BUDGET_SCHEMA, BUDGET_DEFAULT)
    for filename in files_exist():
        validate_storage(f"{DATA_DIR}/expenses/{filename}", DATA_SCHEMA, DATA_DEFAULT)

def load_data():
    """Loads the data file from DATA_PATH, returning as JSON"""
    with open(DATA_PATH, 'r') as f:
        return json.load(f)

def load_date_data(d):
    """Loads data file with date d, making one if it doesn't exist, and returns JSON"""
    if not isinstance(d, datetime.date):
        # Throw an exception
        return
    date_str = date_to_isostring(d)
    file_name = f"{DATA_DIR}/expenses/data-{date_str}.json"
    if not os.path.exists(file_name):
        with open(file_name, 'w') as f:
            json.dump([], f, indent=2)
            return []
    with open(file_name, 'r') as f:
        return json.load(f)

def load_budget_data():
    with open(BUDGET_PATH, 'r') as f:
        return json.load(f)

def save_data(data):
    with open(DATA_PATH, 'w') as f:
        json.dump(data, f, indent=2)

def save_date_data(data, d):
    """Save data to appropriate file from datetime.date d"""
    if not isinstance(d, datetime.date):
        # Throw an exception
        return
    date_str = date_to_isostring(d)
    file_name = f"{DATA_DIR}/expenses/data-{date_str}.json"
    with open(file_name, 'w') as f:
        json.dump(data, f, indent = 2)

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

def files_exist():
    """Returns a list of all file names in the data directory."""
    if not os.path.exists(f"{DATA_DIR}/expenses"):
        return []
    return [f for f in os.listdir(f"{DATA_DIR}/expenses") if os.path.isfile(os.path.join(f"{DATA_DIR}/expenses", f))]

def get_date(file_name: str):
    """Extracts date from a file name formatted as data-<DATE>.json as a datetime.date"""
    if file_name[0:5] != 'data-' or file_name[-5:] != '.json':
        raise ValueError("File name must be formatted as data-<DATE>.json")
    date_str = file_name[5:-5]
    return datetime.date.fromisoformat(date_str)

def load_files(file_names):
    """Loads and combines data from multiple files."""
    combined_data = []
    for file_name in file_names:
        with open(f"{DATA_DIR}/expenses/{file_name}", 'r') as f:
            data = json.load(f)
            combined_data.extend(data)
    return combined_data