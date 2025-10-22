import admin from 'firebase-admin';

// Esta sección lee las variables de entorno de Netlify
let firebaseApp;
try {
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
    firebaseApp = admin.app();
  }
} catch (error) {
  console.error("--- ERROR CRÍTICO DURANTE LA INICIALIZACIÓN DE FIREBASE ---", error);
}

// Handler principal
export const handler = async (event) => {
  // Comprueba si la inicialización falló
  if (!firebaseApp) {
    console.error("La app de Firebase no está disponible. Revisa las credenciales en Netlify.");
    return { statusCode: 500, body: "Error de configuración del servidor." };
  }

  // Lógica de envío
  try {
    const { title, body, token, topic } = JSON.parse(event.body);

    if (token) {
      // Envío directo a un dispositivo
      await admin.messaging().send({ notification: { title, body }, token: token });
    } else if (topic) {
      // Envío masivo a un tema
      await admin.messaging().send({ notification: { title, body }, topic: topic });
    }

    return { statusCode: 200, body: "Notificación procesada." };

  } catch (error) {
    return { statusCode: 500, body: error.message };
  }
};




