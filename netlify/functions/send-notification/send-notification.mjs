import admin from 'firebase-admin';

let firebaseApp;

try {
  // Solo inicializar si no se ha hecho antes
  if (!admin.apps.length) {
    console.log("Intentando inicializar Firebase Admin SDK...");

    // Lee la variable de entorno que contiene TODO el JSON
    const serviceAccountString = process.env.FIREBASE_SERVICE_ACCOUNT;

    if (!serviceAccountString) {
      throw new Error("La variable de entorno FIREBASE_SERVICE_ACCOUNT no está definida.");
    }

    // Convierte el string a un objeto JSON
    const serviceAccount = JSON.parse(serviceAccountString);

    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
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
  if (!firebaseApp) {
    console.error("La app de Firebase no está disponible. Revisa la variable FIREBASE_SERVICE_ACCOUNT en Netlify.");
    return { statusCode: 500, body: "Error de configuración del servidor." };
  }

  // Lógica de envío
  try {
    const { title, body, token, topic } = JSON.parse(event.body);

    if (token) {
      // Envío directo
      await admin.messaging().send({ notification: { title, body }, token: token });
    } else if (topic) {
      // Envío a tema
      await admin.messaging().send({ notification: { title, body }, topic: topic });
    }

    return { statusCode: 200, body: "Notificación procesada." };

  } catch (error) {
    return { statusCode: 500, body: error.message };
  }
};
