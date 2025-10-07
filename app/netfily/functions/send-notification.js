
const admin = require("firebase-admin");

// ! IMPORTANTE: No pegarás el JSON aquí directamente.
// Lo configurarás como una variable de entorno en la interfaz de Netlify.
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);

// Inicializar la app de Firebase solo si no se ha hecho antes
if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

// Este es el manejador principal que Netlify ejecutará
exports.handler = async function(event, context) {
  // Solo permitir solicitudes de tipo POST
  if (event.httpMethod !== "POST") {
    return { statusCode: 405, body: "Method Not Allowed" };
  }

  try {
    // Extraer el título y el cuerpo de la notificación de la solicitud
    const { title, body } = JSON.parse(event.body);

    if (!title || !body) {
      return { statusCode: 400, body: "Solicitud incorrecta: Se requieren 'title' y 'body'." };
    }

    // Construir el mensaje de la notificación
    const message = {
      notification: {
        title: title,
        body: body,
      },
      topic: "all", // Enviar a todos los dispositivos suscritos al tema "all"
    };

    // Enviar el mensaje usando el SDK de Firebase Admin
    const response = await admin.messaging().send(message);
    console.log("Notificación enviada con éxito:", response);

    return {
      statusCode: 200,
      body: JSON.stringify({ message: "Notificación enviada con éxito", response: response }),
    };

  } catch (error) {
    console.error("Error al enviar la notificación:", error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: "Error interno del servidor." }),
    };
  }
};
