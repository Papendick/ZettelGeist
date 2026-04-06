package de.zettelgeist.app.util

object MarkdownParser {

    private val WIKILINK_REGEX = Regex("""\[\[([^\]]+)]]""")
    private val TAG_REGEX = Regex("""(?:^|\s)#([a-zäöüß][a-zäöüß0-9_-]*)""", RegexOption.IGNORE_CASE)

    fun extractWikilinks(content: String): List<String> {
        return WIKILINK_REGEX.findAll(content).map { it.groupValues[1].trim() }.distinct().toList()
    }

    fun extractTags(content: String): List<String> {
        return TAG_REGEX.findAll(content).map { it.groupValues[1].lowercase() }.distinct().toList()
    }

    fun contentHash(content: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun sanitizeFileName(title: String): String {
        return title
            .replace(Regex("[^a-zA-Z0-9äöüÄÖÜß _-]"), "")
            .replace(" ", "_")
            .take(100)
            .ifEmpty { "untitled" }
    }
}
