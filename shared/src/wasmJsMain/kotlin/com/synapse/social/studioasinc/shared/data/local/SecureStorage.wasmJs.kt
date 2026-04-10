package com.synapse.social.studioasinc.shared.data.local

class WasmJsSecureStorage : SecureStorage {
    private val prefs = mutableMapOf<String, String>()
    override fun save(key: String, value: String) { prefs[key] = value }
    override fun getString(key: String): String? = prefs[key]
    override fun clear(key: String) { prefs.remove(key) }
}
