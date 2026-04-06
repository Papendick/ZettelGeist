package de.zettelgeist.app.data.vault

import android.content.Context
import java.io.File

class VaultManager(context: Context) {

    private val vaultDir: File = File(context.filesDir, "vault")

    init {
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
    }

    fun readNote(filePath: String): String {
        val file = File(vaultDir, filePath)
        return if (file.exists()) file.readText() else ""
    }

    fun writeNote(filePath: String, content: String) {
        val file = File(vaultDir, filePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun deleteNote(filePath: String) {
        val file = File(vaultDir, filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    fun noteExists(filePath: String): Boolean {
        return File(vaultDir, filePath).exists()
    }

    fun getVaultPath(): String = vaultDir.absolutePath
}
