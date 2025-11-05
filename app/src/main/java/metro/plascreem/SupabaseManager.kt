package metro.plascreem

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// --- INTERFACES PARA LOS CALLBACKS (Listeners) ---
interface SupabaseUploadListener {
    fun onSuccess(publicUrl: String)
    fun onFailure(message: String)
}

interface SupabaseDownloadListener {
    fun onSuccess()
    fun onFailure(message: String)
}

interface SupabaseDeleteListener {
    fun onSuccess()
    fun onFailure(message: String)
}

interface SupabaseListListener {
    fun onSuccess(files: List<String>)
    fun onFailure(message: String)
}

object SupabaseManager {

    // WORKAROUND FINAL: Claves hardcodeadas para resolver el problema de entorno de compilación.
    private const val SUPABASE_URL = "https://sxdejifprccilvdgsmhi.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN4ZGVqaWZwcmNjaWx2ZGdzbWhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIxNzk2MjEsImV4cCI6MjA3Nzc1NTYyMX0.nadXZyHoLhx3H0AClvyJzrVcBOJaTv4f7DdaKGaVbb4"

    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Storage)
        }
    }

    // --- FUNCIÓN DE DESCARGA ---
    @JvmStatic
    fun downloadFile(bucketName: String, pathInBucket: String, destinationFile: File, listener: SupabaseDownloadListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileBytes: ByteArray = supabaseClient.storage.from(bucketName).downloadPublic(pathInBucket)

                FileOutputStream(destinationFile).use { outputStream ->
                    outputStream.write(fileBytes)
                }

                withContext(Dispatchers.Main) { listener.onSuccess() }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    val errorMessage = e.message ?: "Error desconocido durante la descarga."
                    if (errorMessage.contains("Body buffer is closed") || errorMessage.contains("Not Found")) {
                        listener.onFailure("404: Archivo no encontrado.")
                    } else {
                        listener.onFailure(errorMessage)
                    }
                }
            }
        }
    }

    // --- FUNCIÓN DE SUBIDA ---
    @JvmStatic
    fun uploadFile(context: Context, fileUri: Uri, storagePath: String, listener: SupabaseUploadListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) { listener.onFailure("No se pudo abrir el archivo desde la URI.") }
                    return@launch
                }
                val fileBytes = inputStream.readBytes()
                inputStream.close()

                val bucketName = storagePath.substringBefore('/')
                val pathInBucket = storagePath.substringAfter('/')

                supabaseClient.storage.from(bucketName).upload(path = pathInBucket, data = fileBytes, upsert = true)
                val publicUrl = supabaseClient.storage.from(bucketName).publicUrl(pathInBucket)

                withContext(Dispatchers.Main) { listener.onSuccess(publicUrl) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    listener.onFailure(e.message ?: "Error desconocido durante la subida.")
                }
            }
        }
    }

    // --- OTRAS FUNCIONES ---
    @JvmStatic
    fun deleteFile(storagePath: String, listener: SupabaseDeleteListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bucketName = storagePath.substringBefore('/')
                val pathInBucket = storagePath.substringAfter('/')

                supabaseClient.storage.from(bucketName).delete(pathInBucket)
                withContext(Dispatchers.Main) { listener.onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    listener.onFailure(e.message ?: "Error desconocido al eliminar.")
                }
            }
        }
    }

    @JvmStatic
    fun listFiles(folderPath: String, listener: SupabaseListListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bucketName = folderPath.substringBefore('/')
                val pathInBucket = folderPath.substringAfter('/')

                val files = supabaseClient.storage.from(bucketName).list(pathInBucket)
                val fileNames = files.map { it.name }

                withContext(Dispatchers.Main) { listener.onSuccess(fileNames) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    listener.onFailure(e.message ?: "Error desconocido al listar archivos.")
                }
            }
        }
    }
}
