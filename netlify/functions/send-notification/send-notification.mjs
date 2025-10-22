import admin from 'firebase-admin';

// --- INICIALIZACIÓN DE FIREBASE ADMIN ---
let firebaseApp;

try {
  // Solo inicializar si no hay apps ya corriendo
  if (!admin.apps.length) {
    console.log("Inicializando Firebase Admin SDK...");
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      }),
    });
    console.log("Firebase Admin SDK inicializado con éxito.");
  } else {
    console.log("Firebase Admin SDK ya estaba inicializado.");
    firebaseApp = admin.app(); // Obtener la app existente
  }
} catch (error) {
  console.error("--- ERROR CRÍTICO DURANTE LA INICIALIZACIÓN DE FIREBASE ---");
  console.error(error);
}

// --- HANDLER PRINCIPAL DE LA FUNCIÓN ---
export const handler = async (event) => {
  if (!firebaseApp) {
    console.error("La app de Firebase no está disponible. Revisa las credenciales (variables de entorno).");
    return { statusCode: 500, body: "Error de configuración del servidor." };
  }

  if (event.httpMethod !== 'POST') {
    return { statusCode: 405, body: 'Método no permitido' };
  }

  try {
    const { title, body, token, topic } = JSON.parse(event.body);
    console.log("Datos recibidos:", { title, body, token, topic });

    if (token) {
      console.log(`Enviando a TOKEN: ${token.substring(0, 20)}...`);
      await admin.messaging().send({ notification: { title, body }, token: token });
      console.log("Éxito: Mensaje a token procesado.");
    } else if (topic) {
      console.log(`Enviando a TOPIC: ${topic}`);
      await admin.messaging().send({ notification: { title, body }, topic: topic });
      console.log("Éxito: Mensaje a topic procesado.");
    } else {
      return { statusCode: 400, body: "Se requiere un 'token' o un 'topic'." };
    }

    return { statusCode: 200, body: "Notificación procesada." };

  } catch (error) {
    console.error('Error durante el envío:', error);
    return { statusCode: 500, body: error.message };
  }
};



