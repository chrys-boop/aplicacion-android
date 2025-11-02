import admin from 'firebase-admin';

// Inicialización del SDK de Firebase Admin
let firebaseApp;
try {
  if (!admin.apps.length) {
    const serviceAccountString = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (!serviceAccountString) throw new Error("La variable de entorno FIREBASE_SERVICE_ACCOUNT no está definida.");
    const serviceAccount = JSON.parse(serviceAccountString);
    firebaseApp = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  } else {
    firebaseApp = admin.app();
  }
} catch (error) {
  console.error("--- ERROR CRÍTICO DE INICIALIZACIÓN DE FIREBASE ---", error);
}

// Handler principal de la función de Netlify
export const handler = async (event) => {
  const headers = { 'Content-Type': 'application/json' };

  if (!firebaseApp) {
    return {
      statusCode: 500,
      headers: headers,
      body: JSON.stringify({ error: "Error en la configuración del servidor de Firebase." })
    };
  }

  try {
    // Extraer todos los datos del cuerpo de la solicitud
    const {
        title,
        body,
        token,
        topic,
        senderId,
        isChatMessage,
        senderName,
        recipientId // <-- ¡Corregido! Se lee el recipientId
    } = JSON.parse(event.body);

    let messagePayload;

    // --- LÓGICA CORREGIDA PARA EL PAYLOAD DE FCM ---

    if (isChatMessage) {
      // --- CASO 1: Es una notificación de CHAT ---
      // Se construye un payload de 'solo datos' que incluye TODOS los campos necesarios.
      // La app Android se encargará de crear la notificación visible con respuesta rápida.
      messagePayload = {
        data: {
          isChatMessage: "true",
          senderId: senderId,
          senderName: senderName,
          recipientId: recipientId, // <-- ¡Corregido! Se añade al payload de datos
          title: title,
          body: body
        }
      };

    } else {
      // --- CASO 2: Es una notificación GENERAL ---
      // Crea una notificación estándar visible directamente.
      messagePayload = {
        notification: {
          title: title || 'Notificación',
          body: body || 'Hay un nuevo mensaje.'
        },
        data: {
          title: title || 'Notificación',
          body: body || 'Hay un nuevo mensaje.'
        }
      };
      if (senderId) {
        messagePayload.data.senderId = senderId;
      }
    }

    // Asignar el destinatario (token de un dispositivo o un tema)
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

    // Enviar el mensaje a través de FCM
    await admin.messaging().send(messagePayload);

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



