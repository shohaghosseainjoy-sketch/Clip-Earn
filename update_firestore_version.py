import firebase_admin
from firebase_admin import credentials, firestore
import os
import json
from datetime import datetime

service_account_info = json.loads(os.environ['FIREBASE_SERVICE_ACCOUNT'])
cred = credentials.Certificate(service_account_info)
firebase_admin.initialize_app(cred)

db = firestore.client()
version = os.environ['VERSION_NAME']
apk_url = os.environ['APK_URL']

version_parts = version.split('.')
version_code = int(version_parts[0]) * 10000 + \
               int(version_parts[1]) * 100 + \
               int(version_parts[2])

db.collection('app_config').document('version').set({
    'latestVersion': version,
    'latestVersionCode': version_code,
    'apkDownloadUrl': apk_url,
    'releaseNotes': 'New update available with improvements!',
    'forceUpdate': False,
    'minVersionCode': 1,
    'updatedAt': datetime.now().isoformat()
})

print(f"Firestore updated: version {version}, code {version_code}")
