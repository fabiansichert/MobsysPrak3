from flask import Flask, jsonify, send_file
import firebase_admin
import json
import os
from datetime import datetime
from firebase_admin import credentials, firestore, auth

import time
app_start_time = time.time()

app = Flask(__name__)

if not firebase_admin._apps:
    cred = credentials.Certificate("mobsysprak3-8c0f9-firebase-adminsdk-fbsvc-347a88a270.json")
    firebase_admin.initialize_app(cred)

db = firestore.client()

@app.route("/")
def index():
    return jsonify({"message": "Backend läuft!"})

@app.route("/messages")
def get_messages():
    messages_ref = db.collection("chats").document("global").collection("messages").stream()
    messages = [doc.to_dict() for doc in messages_ref]
    return jsonify(messages)


# Monitoring für Cloud-Ressourcen & Server-Anwendung

@app.route("/monitoring")
def monitoring():
    try:
        user_list = []
        page = auth.list_users()
        while page:
            user_list.extend(page.users)
            page = page.get_next_page()
        user_count = len(user_list)
    except Exception as e:
        user_count = f"Fehler: {e}"

    try:
        messages_ref = db.collection("chats").document("global").collection("messages").stream()
        message_count = sum(1 for _ in messages_ref)
    except Exception as e:
        message_count = f"Fehler: {e}"

    status = "OK" if user_count != 0 else "Keine User gefunden"

    return jsonify({
        "status": status,
        "user_count": user_count,
        "message_count": message_count
    })



# Backupmanagement DB
def convert_timestamp(doc_dict):
    for key, value in doc_dict.items():
        if hasattr(value, "isoformat"):
            doc_dict[key] = value.isoformat()
    return doc_dict

@app.route("/backup")
def backup():
    messages_ref = db.collection("chats").document("global").collection("messages").stream()
    messages = []
    for doc in messages_ref:
        data = doc.to_dict()
        data = convert_timestamp(data)
        messages.append(data)

    backup_data = {"chats": messages}

    backup_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "backups")
    os.makedirs(backup_dir, exist_ok=True)

    filename = f"backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    filepath = os.path.join(backup_dir, filename)

    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(backup_data, f, ensure_ascii=False, indent=2)

    return jsonify({"message": "Backup erstellt", "file": filepath})




