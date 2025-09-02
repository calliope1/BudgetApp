# Flask server
# app.py
from flask import Flask, request
import directories.server_data as sd
import budget.routes as br
import expenses.routes as er
#import random
#import json
#import expenses.id_factory as eif

DATA_FILE = 'data.json'
BUDGET_FILE = 'budget.json'

app = Flask(__name__)

# with open("data/data.json", "r") as f:
#     data = json.load(f)

# data = eif.assign_ids(data, ["amount","date","description"])
# with open("data/data.json", "w") as f:
#     json.dump(data, f, indent=2)

sd.ensure_storage()

@app.route('/expenses', methods=['POST'])
def add_expense():
    return er.add_expense(request)

@app.route('/expenses', methods=['GET'])
def get_expenses():
    return er.get_expenses(request)

#@app.route('/expenses', methods=['PUT'])

@app.route('/expenses/<expense_id>', methods=['PATCH'])
def patch_expense(expense_id):
    return er.patch_expense(request, expense_id)

@app.route('/expenses/<expense_id>', methods=['DELETE'])
def delete_expense(expense_id):
    return er.delete_expense(request, expense_id)

@app.route('/budget', methods=['GET'])
def get_budget():
    return br.get_budget()

@app.route('/budget', methods=['PUT'])
def update_budget():
    return br.update_budget(request)

if __name__ == '__main__':
    # For development only. Use a proper WSGI server for production.
    app.run(host='0.0.0.0', port=5000, debug=True)