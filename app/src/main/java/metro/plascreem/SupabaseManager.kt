package metro.plascreem

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

// Interfaces para los callbacks
interface SupabaseUploadListener {
    fun onSuccess(publicUrl: String)
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

    private const val SUPABASE_URL = "https://wkvmaestrcocxncsoqgxx.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indrdm1hZXN0cmNvY3huY3NvcWd4eCIsImlvyJleHAiOjIwMTI1ODYxODR9.B34b_gB11J3KTMso0_1t3b-3H222TzyVd_zrAxg6Ld0"

    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Storage)
            install(GoTrue)
            httpEngine = Android.create {}
        }
    }

    // --- MÉTODO DE INICIALIZACIÓN PARA FORZAR LA CREACIÓN EN EL HILO PRINCIPAL ---
    @JvmStatic
    fun init() {
        // Esta simple llamada fuerza la inicialización "lazy" del supabaseClient
        supabaseClient.storage
    }

    // --- MÉTODOS PÚBLICOS CON @JvmStatic PARA JAVA ---

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

                supabaseClient.storage.from(bucketName).upload(pathInBucket, fileBytes, upsert = true)
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

