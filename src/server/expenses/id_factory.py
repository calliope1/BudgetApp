import hashlib
import directories.server_data as sd
import random
import string
import copy

def create_id(values, data):
    combined_string = ""
    for value in values:
        combined_string += str(value) + "|"
    combined_string = combined_string[:-1]

    id_attempt = hashlib.md5(combined_string.encode()).hexdigest()

    while sd.id_exists(data, id_attempt):
        random_suffix = ''.join(random.choices(string.ascii_letters + string.digits, k=5))
        id_attempt = f"{id_attempt}-{random_suffix}"

    return id_attempt

def assign_ids(data, keys):
    out_data = copy.deepcopy(data)
    for datum in out_data:
        if "id" not in datum:
            datum["id"] = "0"
    for datum in out_data:
        if datum["id"] == "0":
            values = [datum[key] for key in keys]
            datum["id"] = create_id(values, data)
    return out_data