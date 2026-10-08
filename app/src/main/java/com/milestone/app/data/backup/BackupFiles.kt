package com.milestone.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where a backup file lives on its way in or out of the app.
 *
 * Export writes into the cache rather than anywhere permanent: the file is a
 * handoff to whatever the user picks in the share sheet — Drive, email, their own
 * Files app — and once it is there, this copy is litter. Android is free to clear
 * the cache whenever it likes, which is exactly the lifetime we want.
 *
 * Reading goes through the content resolver rather than a file path, because the
 * document picker hands back a URI that may not be a file at all.
 */
@Singleton
class BackupFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Writes [json] to a shareable file and returns a URI other apps can read. */
    suspend fun write(fileName: String, json: String): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, BACKUP_DIR).apply { mkdirs() }

        // Only ever one export sitting around: the previous one has either been
        // shared already or been abandoned, and either way it is stale.
        directory.listFiles()?.forEach { it.delete() }

        val file = File(directory, fileName)
        file.writeText(json)
        FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)
    }

    /** Reads a file the user picked. */
    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().decodeToString()
        } ?: throw IllegalStateException("That file could not be opened.")
    }

    private companion object {
        const val BACKUP_DIR = "backups"
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    }
}
