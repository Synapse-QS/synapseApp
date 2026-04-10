package com.synapse.social.studioasinc.shared.util

import kotlin.random.Random

actual object UUIDUtils {
    private val charPool : List<Char> = ('a'..'z') + ('0'..'9')

    actual fun randomUUID(): String {
        return (1..32)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")
    }
}
