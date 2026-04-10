package com.synapse.social.studioasinc.shared.data

actual object PlatformUtils {
    actual fun sha1(input: String): String = input.hashCode().toString()
    actual fun sha256(input: String): String = input.hashCode().toString()
    actual fun sha256(input: ByteArray): String = input.contentHashCode().toString()
    actual fun hmacSha256(key: ByteArray, data: String): ByteArray = data.encodeToByteArray()
}
