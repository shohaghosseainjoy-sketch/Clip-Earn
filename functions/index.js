const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendUpdateNotification = functions.firestore
    .document('app_config/version')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();

        if (newData.latestVersionCode <= oldData.latestVersionCode) return;

        const message = {
            notification: {
                title: '🎉 ClapEarn Update Available!',
                body: `Version ${newData.latestVersion} is ready. Tap to update now!`
            },
            data: {
                type: 'app_update',
                version: newData.latestVersion,
                apkUrl: newData.apkDownloadUrl
            },
            topic: 'all_users'
        };

        await admin.messaging().send(message);
        console.log('Update notification sent to all users');
    });
