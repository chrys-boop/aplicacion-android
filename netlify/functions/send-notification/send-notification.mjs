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
    // *** INICIO DE LA MODIFICACIÓN ***
    // Se extrae el senderId del cuerpo del evento.
    const { title, body, token, topic, senderId } = JSON.parse(event.body);

    // Se crea un payload que incluye tanto la notificación visible como los datos adicionales.
    const messagePayload = {
      notification: {
        title: title || 'Notificación',
        body: body || 'Hay un nuevo mensaje.'
      },
      data: {
        title: title || 'Notificación',
        body: body || 'Hay un nuevo mensaje.'
      }
    };

    // Se añade el senderId a los datos si existe. Esto es lo que usará la app para redirigir.
    if (senderId) {
      messagePayload.data.senderId = senderId;
    }

    if (token) {
      messagePayload.token = token;
    } else if (topic) {
      messagePayload.topic = topic;
    } else {
      return {
        statusCode: 400,
        headers: headers,
        body: JSON.stringify({ error: "Se requiere un 'token' o un 'topic'." })
      };
    }

    await admin.messaging().send(messagePayload);
    // *** FIN DE LA MODIFICACIÓN ***

    return {
      statusCode: 200,
      headers: headers,
      body: JSON.stringify({ message: "Notificación procesada con éxito." })
    };

  } catch (error) {
    console.error("Error al procesar la notificación:", error);
    return {
      statusCode: 500,
      headers: headers,
      body: JSON.stringify({ error: error.message })
    };
  }
};

