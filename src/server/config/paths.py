"""Paths"""

ROOT = "server" # Or root to app.py

DATA_DIR = f"{ROOT}/data"
SCHEMATA_DIR = f"{ROOT}/schemata"

DATA_PREFIX = "data"
BUDGET_PREFIX = "budget"

DATA_PATH = f"{DATA_DIR}/{DATA_PREFIX}.json"
BUDGET_PATH = f"{DATA_DIR}/{BUDGET_PREFIX}.json"

DATA_SCHEMA = f"{SCHEMATA_DIR}/{DATA_PREFIX}.schema.json"
BUDGET_SCHEMA = f"{SCHEMATA_DIR}/{BUDGET_PREFIX}.schema.json"

DATA_DEFAULT = []
BUDGET_DEFAULT = {"weekly_budget":110.0}

DELETED_DIR = "deleted"
DELETED_DATA_PATH = f"{DATA_DIR}/{DELETED_DIR}/{DATA_PREFIX}.deleted.json"