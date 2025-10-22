import admin from 'firebase-admin';

// --- INICIALIZACIÓN DE FIREBASE ADMIN ---
try {
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      }),
    });
  }
} catch (error) {
  console.error("Error inicializando Firebase Admin SDK:", error);
}

// --- HANDLER DE DIAGNÓSTICO ---
export const handler = async (event) => {
  // Solo aceptar peticiones POST
  if (event.httpMethod !== 'POST') {
    return { statusCode: 405, body: 'Método no permitido' };
  }

  try {
    const { title, body, token, topic } = JSON.parse(event.body);

    console.log("--- FUNCIÓN DE DIAGNÓSTICO ACTIVA ---");
    console.log("Datos recibidos:", { title, body, token, topic });

    if (!title || !body) {
      return { statusCode: 400, body: 'El título y el cuerpo son requeridos.' };
    }

    // --- LÓGICA DE DIAGNÓSTICO: SOLO SE PERMITE ENVIAR A TOKEN ---
    if (token) {
      // Si se proporciona un token, se envía SOLO a ese token.
      console.log(`==> MODO DIAGNÓSTICO: Enviando a TOKEN específico: ${token.substring(0, 20)}...`);
      await admin.messaging().send({
        notification: { title, body },
        token: token,
      });
      console.log("Éxito: Mensaje a token procesado.");
       return {
        statusCode: 200,
        body: JSON.stringify({ message: "Notificación a TOKEN enviada." }),
      };

    } else {
      // Si NO se proporciona un token, la función se niega a trabajar.
      console.error("EJECUCIÓN BLOQUEADA: Esta función de diagnóstico solo permite envíos a tokens. Se recibió un intento de envío a topic o sin destino.");
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "MODO DIAGNÓSTICO: Solo se permiten envíos a tokens." })
      };
    }

  } catch (error) {
    console.error('Error catastrófico en la función de diagnóstico:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: error.message }),
    };
  }
};


