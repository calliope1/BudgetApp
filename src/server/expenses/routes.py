import directories.server_data as sd
import signature.verify_signature as svs
from flask import jsonify
import datetime
import expenses.id_factory as eif
import datetime as dt
from datetime import datetime
from config.conf import ENCRYPT

# GET
def get_expenses(request, this_week = False):
    """Get all expenses from the data directory with the arguments' restrictions.

    Parameters
    ----------
    request : Flask.request
        Request from webserver
    this_week : bool
        Default = False
        If True, ignores all arguments and returns expenses from this calendar week (commencing Monday)

    Arguments
    ---------
    'datestr' type here means str in YYYY-MM-DD format
    start_date : datestr
        Only expenses from start_date or newer
    end_date : datestr
        Only expenses from end_date or older
    week_commencing : datestr
        Only expenses from within 7 days of week_commencing
    week_containing : datestr
        Only expenses from the calendar week (Monday start) containing week_containing
        Does nothing if week_commencing is provided

    Returns
    -------
    Concatenated JSON of all expenses from the dates matching the arguments, plus exit code

    TODO
    ----
    Input validation and error responses for malformed inputs
    """

    if this_week:
        start_date = None
        end_date = None
        today = dt.date.today()
        week_commencing = today - dt.timedelta(today.weekday())
    else:
        start_date_req = request.args.get('start_date')
        end_date_req = request.args.get('end_date')
        week_commencing_req = request.args.get('week_commencing')
        week_containing_req = request.args.get('week_containing')

        start_date = dt.date.fromisoformat(start_date_req) if start_date_req else None
        end_date = dt.date.fromisoformat(end_date_req) if end_date_req else None
        #week_commencing = dt.date.fromisoformat(week_commencing_req) if week_commencing_req else None
        #week_containing = dt.date.fromisoformat(week_containing_req) if week_containing_req else None

        if week_commencing_req:
            week_commencing = dt.date.fromisoformat(week_commencing_req)
        elif week_containing_req:
            week_commencing = dt.date.fromisoformat(week_containing_req)
            weekday = week_commencing.weekday()
            week_commencing = week_commencing - dt.timedelta(weekday)
        else:
            week_commencing = None

    files = sd.files_exist()
    wanted_files = []

    for file in files:
        file_date = sd.get_date(file)
        if start_date != None and file_date < start_date:
            continue
        if end_date != None and file_date > start_date:
            continue
        if week_commencing == None or week_commencing <= file_date < week_commencing + dt.timedelta(7):
            wanted_files.append(file)

    data = sd.load_files(wanted_files)

    return jsonify(data), 200

# POST
def add_expense(request):
    """Post expense to the server

    Parameters
    ----------
    request : Flask.request

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
    body = request.get_data()  # raw bytes

    if ENCRYPT:
        signature = request.headers.get('X-Signature', '')
        if not signature:
            return jsonify({'error': 'Missing signature header'}), 400
        if not svs.verify_signature(body, signature):
            return jsonify({'error': 'Invalid signature'}), 403

    try:
        payload = request.get_json(force=True)
        # basic validation
        amount = float(payload['amount'])
        description = str(payload['description'])
        date_str = str(payload['date'])  # expected YYYY-MM-DD
        datetime.strptime(date_str, '%Y-%m-%d') # Test format
        dt_date = dt.date.fromisoformat(date_str)
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    full_data = sd.load_data()
    date_data = sd.load_date_data(dt_date)
    
    unique_id = eif.create_id([amount, description, date_str, datetime.now()], full_data)
    
    expense = {'id': unique_id, 'amount': amount, 'description': description, 'date': date_str}

    full_data.append(expense)
    date_data.append(expense)
    sd.save_data(full_data)
    sd.save_date_data(date_data, dt_date)
    return jsonify({'status': 'ok', 'expense': expense}), 201

# PATCH
def patch_expense(request, expense_id):
    body = request.get_data()
    
    if ENCRYPT:
        signature = request.headers.get('X-Signature', '')
        if not signature:
            return jsonify({'error': 'Missing signature header'}), 400
        if not svs.verify_signature(body, signature):
            return jsonify({'error': 'Invalid signature'}), 403

    try:
        payload = request.get_json(force=True)
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    # Require id to find original expense date
    full_data = sd.load_data()
    
    expense = next((e for e in full_data if e.get('id') == expense_id), None)

    if expense == None:
        return jsonify({'error': 'Id not found in full data'}), 404

    try:
        original_date = dt.date.fromisoformat(expense['date'])
    except Exception as e:
        return jsonify({'error': 'Malformed expense date data', 'details': str(e), 'expense': expense}), 500

    date_data = sd.load_date_data(original_date)

    try:
        new_amount = float(payload['amount'])
        new_description = str(payload['description'])
        new_date_str = str(payload['date'])
        datetime.strptime(new_date_str, '%Y-%m-%d')
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    expense['amount'] = new_amount
    expense['description'] = new_description
    expense['date'] = new_date_str

    # If date has changed, remove from old date data and load new date data
    if new_date_str != expense['date']:
        date_data = [item for item in date_data if item["id"] != expense_id]
        sd.save_date_data(date_data, original_date)
        date_data = sd.load_date_data(dt.date.fromisoformat(new_date_str))
        date_data.append(expense)
    else:
        daily_expense = next((e for e in date_data if e.get('id') == expense_id), None)
        daily_expense['amount'] = new_amount
        daily_expense['description'] = new_description
        daily_expense['date'] = new_date_str

    sd.save_data(full_data)
    sd.save_date_data(date_data, dt.date.fromisoformat(new_date_str))
    return jsonify({'status': 'Expense updated', 'expense': expense}), 200

# DELETE
def delete_expense(request, expense_id):
    signature = request.headers.get('X-Signature', '')
    body = request.get_data()

    if not signature:
        return jsonify({'error': 'Missing signature header'}), 400
    if not svs.verify_signature(body, signature):
        return jsonify({'error': 'Invalid signature'}), 403

    try:
        payload = request.get_json(force=True)
        delete_id = str(payload['id'])
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    if delete_id != expense_id:
        return jsonify({'error': 'Argument misaligned', 'details': f'expense id must match payload "id". {delete_id} supplied to address {expense_id}'}), 400

    data = sd.load_data()
    expense = next((e for e in data if e.get('id') == expense_id), None)

    try:
        expense_date = dt.date.fromisoformat(expense['date'])
    except Exception as e:
        return jsonify({'error': 'Malformed expense date data', 'details': str(e), 'expense': expense}), 500

    daily_data = sd.load_date_data(expense_date)
    expense_daily = next((e for e in daily_data if e.get('id') == expense_id), None)

    # If you wanted to you could check if the two expenses are identical here

    if expense == None and expense_daily == None:
        return jsonify({'error': 'Id not found'}), 406
    elif expense == None:
        return jsonify({'error': 'Id only found in daily data, not full data'}), 406
    elif expense_daily == None:
        return jsonify({'error': 'Id only found in full data, not daily data'}), 406

    # Add the deleted item to data/deleted for safety
    deleted_data = sd.load_deleted()
    deleted_data.append(expense)
    sd.save_deleted(deleted_data)

    data = [item for item in data if item["id"] != expense_id]
    daily_data = [item for item in daily_data if item["id"] != expense_id]

    sd.save_data(data)
    sd.save_date_data(daily_data, expense_date)
    return jsonify({'status': 'Expense deleted', 'expense': expense}), 200