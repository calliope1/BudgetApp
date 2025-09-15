# Flask server
# app.py
from operator import methodcaller
from flask import Flask, request
import directories.server_data as sd
import budget.routes as br
import expenses.routes as er
from config.conf import (ENCRYPT, PORT)

app = Flask(__name__)

@app.route('/expenses', methods=['POST'])
def add_expense():
    """Post expense to the server
    
    See expenses.routes.add_expense

    Headers
    -------
    X-Signature
        Encoded signature that must match the provided data after an hmac encoding
        See verify_signature.verify_signature

    Data
    ----
    JSON
        JSON containing at least 'amount', 'description', and 'date' keys
        amount : float
        description : str
        date : str
            In YYYY-MM-DD format
    """

    return er.add_expense(request)

@app.route('/expenses', methods=['GET'])
def get_expenses():
    """Get all expenses, or filter by query parameters

    See expenses.routes.get_expenses

    Arguments
    ---------
    start_date
    end_date
    week_commencing
    week_containing
    """

    return er.get_expenses(request)

@app.route('/expenses/this_week', methods=['GET'])
def get_expenses_this_week():
    """Return all expenses from this calendar week"""
    return er.get_expenses(request, this_week = True)

#@app.route('/expenses', methods=['PUT'])

# Legacy
@app.route('/expenses/<expense_id>', methods=['PATCH'])
def patch_expense_legacy(expense_id):
    return er.patch_expense(request, expense_id)

@app.route('/expenses/id/<expense_id>', methods=['PATCH'])
def patch_expense(expense_id):
    """Patch expense with id <expense_id> with new data

    Parameters
    ----------
    expense_id : str
        id of the expense being patched

    Headers
    -------
    X-Signature
        Must match the patched json data
    """

    return er.patch_expense(request, expense_id)

# Legacy
@app.route('/expenses/<expense_id>', methods=['DELETE'])
def delete_expense_legacy(expense_id):
    return er.delete_expense(request, expense_id)

@app.route('/expenses/id/<expense_id>', methods=['DELETE'])
def delete_expense(expense_id):
    return er.delete_expense(request, expense_id)

@app.route('/budget', methods=['GET'])
def get_budget():
    return br.get_budget()

@app.route('/budget', methods=['PUT'])
def update_budget():
    return br.update_budget(request)

if __name__ == '__main__':
    sd.ensure_storage()

    if not os.path.exists('config/secrets.py'):
        print("secrets.py not initialised. Initialise?")
        response = 'invalid-string'
        while response not in ['y', 'yes', 'n', 'no', '']:
            response = input("[Y]es/[N]o (default no): ").lower()
            if response in ['', 'n', 'no']:
                print("Cannot begin without secrets.py. Exiting.")
                quit()
            elif response in ['y', 'yes']:
                import config.init_secrets as isec
                isec.init_secrets('config/secrets.py')
            else:
                print("Invalid response.")

    if not ENCRYPT:
        print("WARNING: Encryption not required to post. Proceed?")
        response = 'invalid-string'
        while response not in ['y', 'yes', 'n', 'no', '']:
            response = input("[Y]es/[N]o (default no): ").lower()
            if response in ['', 'n', 'no']:
                quit()

    # For development only. Use a proper WSGI server for production.
    app.run(host='0.0.0.0', port=PORT, debug=True)