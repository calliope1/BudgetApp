import directories.server_data as sd
import signature.verify_signature as svs
from flask import jsonify
import datetime
import hashlib
import random
import string
import expenses.id_factory as eif
from datetime import datetime

def get_expenses():
    data = sd.load_data()
    return jsonify(data), 200

def add_expense(request):
    signature = request.headers.get('X-Signature', '')
    body = request.get_data()  # raw bytes

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
        # validate date
        datetime.strptime(date_str, '%Y-%m-%d')
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    data = sd.load_data()
    
    unique_id = eif.create_id([amount, description, date_str, datetime.now()], data)
    
    data.append({'id': unique_id, 'amount': amount, 'description': description, 'date': date_str})
    sd.save_data(data)
    return jsonify({'status': 'ok'}), 201

def patch_expense(request, expense_id):
    signature = request.headers.get('X-Signature', '')
    body = request.get_data()

    if not signature:
        return jsonify({'error': 'Missing signature header'}), 400
    if not svs.verify_signature(body, signature):
        return jsonify({'error': 'Invalid signature'}), 403

    try:
        payload = request.get_json(force=True)
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    data = sd.load_data()
    
    expense = next((e for e in data if e.get('id') == expense_id), None)

    if expense == None:
        return jsonify({'error': 'Id not found'}), 404

    try:
        amount = float(payload['amount'])
        description = str(payload['description'])
        date_str = str(payload['date'])
        datetime.strptime(date_str, '%Y-%m-%d')
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    expense['amount'] = amount
    expense['description'] = description
    expense['date'] = date_str

    sd.save_data(data)
    return jsonify({'status': 'Expense updated', 'expense': expense}), 200

def delete_expense(request, expense_id):
    signature = request.headers.get('X-Signature', '')
    body = request.get_data()

    if not signature:
        return jsonify({'error': 'Missing signature header'}), 400
    if not svs.verify_signature(body, signature):
        return jsonify({'error': 'Invalid signature'}), 403

    try:
        payload = request.get_json(force=True)
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    data = sd.load_data()
    
    expense = next((e for e in data if e.get('id') == expense_id), None)

    if expense == None:
        return jsonify({'error': 'Id not found'}), 404

    data = [item for item in data if item["id"] != expense_id]

    sd.save_data(data)
    return jsonify({'status': 'Expense deleted', 'expense': expense}), 200