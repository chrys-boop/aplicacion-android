import admin from 'firebase-admin';

// Lee las credenciales de servicio desde las variables de entorno de Netlify
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);

// Inicializa la app de Firebase Admin solo si no se ha hecho antes
if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

// Este es el manejador principal que Netlify ejecutará
const handler = async (event) => {
  // 1. Verificar que la solicitud sea de tipo POST
  if (event.httpMethod !== 'POST') {
    return {
      statusCode: 405,
      body: 'Method Not Allowed',
      headers: { 'Content-Type': 'application/json' },
    };
  }

  try {
    // 2. Extraer los datos del cuerpo de la solicitud
    const { title, body } = JSON.parse(event.body);

    // Validar que el título y el cuerpo existan
    if (!title || !body) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: "'title' and 'body' are required." }),
        headers: { 'Content-Type': 'application/json' },
      };
    }

    // trigger
    // 3. Construir el mensaje de la notificación
    const message = {
      notification: {
        title: title,
        body: body,
      },
      topic: 'all', // Enviar a todos los dispositivos suscritos al tema "all"
    };

    // 4. Enviar el mensaje
    const response = await admin.messaging().send(message);
    console.log('Notificación enviada con éxito:', response);

    // 5. Devolver una respuesta exitosa
    return {
      statusCode: 200,
      body: JSON.stringify({
        message: 'Notificación enviada con éxito.',
        response: response,
      }),
      headers: { 'Content-Type': 'application/json' },
    };
  } catch (error) {
    console.error('Error al enviar la notificación:', error);
    // 6. Devolver una respuesta de error
    return {
      statusCode: 500,
      body: JSON.stringify({
        error: 'Error interno del servidor al enviar la notificación.',
        details: error.message,
      }),
      headers: { 'Content-Type': 'application/json' },
    };
  }
};

export { handler };
