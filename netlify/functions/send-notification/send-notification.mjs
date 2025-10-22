import admin from 'firebase-admin';

let firebaseApp;
try {
  if (!admin.apps.length) {
    const serviceAccountString = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (!serviceAccountString) throw new Error("FIREBASE_SERVICE_ACCOUNT no definida.");
    const serviceAccount = JSON.parse(serviceAccountString);
    firebaseApp = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  } else {
    firebaseApp = admin.app();
  }
} catch (error) {
  console.error("--- ERROR CRÍTICO DE INICIALIZACIÓN ---", error);
}

// Handler principal
export const handler = async (event) => {
  // Define los headers que se usarán en TODAS las respuestas.
  const headers = {
    'Content-Type': 'application/json'
  };

  if (!firebaseApp) {
    return {
      statusCode: 500,
      headers: headers,
      body: JSON.stringify({ error: "Error de configuración del servidor." })
    };
  }

  try {
    const { title, body, token, topic } = JSON.parse(event.body);

    if (token) {
      await admin.messaging().send({ notification: { title, body }, token: token });
    } else if (topic) {
      await admin.messaging().send({ notification: { title, body }, topic: topic });
    } else {
      return {
        statusCode: 400,
        headers: headers,
        body: JSON.stringify({ error: "Se requiere un 'token' o un 'topic'." })
      };
    }

    // Responde con un JSON y el header correcto para indicar éxito.
    return {
      statusCode: 200,
      headers: headers,
      body: JSON.stringify({ message: "Notificación procesada con éxito." })
    };

  } catch (error) {
    return {
      statusCode: 500,
      headers: headers,
      body: JSON.stringify({ error: error.message })
    };
  }
};

