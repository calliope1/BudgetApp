import directories.server_data as sd
import signature.verify_signature as svs
from flask import jsonify

def get_budget():
    data = sd.load_budget_data()
    return jsonify(data), 200

def update_budget(request):
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



    try:
        weekly_budget = float(payload['weekly_budget'])
    except Exception as e:
        return jsonify({'error': 'Invalid payload', 'details': str(e)}), 400

    budget = {"weekly_budget":weekly_budget}

    sd.save_budget_data(budget)
    return jsonify({'status': 'Budget updated', 'budget': weekly_budget}), 200