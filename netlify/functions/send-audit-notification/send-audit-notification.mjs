
import admin from 'firebase-admin';

// IMPORTANT: Set the FIREBASE_SERVICE_ACCOUNT and FIREBASE_DATABASE_URL environment variables in your Netlify settings.
try {
  if (admin.apps.length === 0) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL // Using environment variable for database URL
    });
  }
} catch (error) {
  console.error('Firebase Admin Initialization Error:', error);
}

const handler = async (event) => {
  // 1. Only allow POST requests
  if (event.httpMethod !== 'POST') {
    return { statusCode: 405, body: 'Method Not Allowed' };
  }

  try {
    // 2. Parse the incoming event data from the app
    const { eventType, fileName, user } = JSON.parse(event.body);

    if (!eventType || !fileName || !user) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "'eventType', 'fileName', and 'user' are required." }),
      };
    }

    // 3. Get supervisor tokens from Firebase Realtime Database
    const db = admin.database();
    const usersRef = db.ref('/users');
    const snapshot = await usersRef.once('value');
    const users = snapshot.val();

    const supervisorTokens = [];
    if (users) {
      for (const key in users) {
        if (users[key].rol === 'Supervisor' && users[key].token) {
          supervisorTokens.push(users[key].token);
        }
      }
    }

    if (supervisorTokens.length === 0) {
      console.log("No supervisors found or they don't have tokens.");
      return {
        statusCode: 200,
        body: JSON.stringify({ message: "No supervisors to notify." }),
      };
    }

    // 4. Construct the notification message based on the event
    let title = '';
    let body = '';

    if (eventType === 'upload') {
      title = 'Nuevo Manual Disponible';
      body = `${user} ha subido el manual: ${fileName}`;
    } else if (eventType === 'download') {
      title = 'Descarga de Manual Registrada';
      body = `${user} ha descargado el manual: ${fileName}`;
    } else {
        return {
          statusCode: 400,
          body: JSON.stringify({ error: `Unknown event type: ${eventType}` }),
        };
    }

    const payload = {
      notification: {
        title: title,
        body: body,
      },
    };

    // 5. Send the notification to the supervisors
    console.log(`Sending notification to ${supervisorTokens.length} supervisors...`);
    const response = await admin.messaging().sendToDevice(supervisorTokens, payload);
    console.log('Successfully sent message:', response);

    // 6. Return a success response
    return {
      statusCode: 200,
      body: JSON.stringify({ message: "Notification sent successfully." }),
    };

  } catch (error) {
    console.error('Error in function handler:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: 'Internal Server Error', details: error.message }),
    };
  }
};

export { handler };
