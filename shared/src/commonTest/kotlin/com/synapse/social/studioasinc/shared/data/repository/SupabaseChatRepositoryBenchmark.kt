package com.synapse.social.studioasinc.shared.data.repository

import kotlin.test.Test
import kotlin.system.measureTimeMillis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class SupabaseChatRepositoryBenchmark {

    // Simulated components
    private val otherParticipants = (1..100).map { "user_$it" }
    private val content = "This is a test message to be encrypted for many participants"
    private val contentBytes = content.encodeToByteArray()

    private fun mockEncryptMessage(userId: String): EncryptedMessage {
        // Simulate some computational work
        // In real world, this is CPU intensive
        return EncryptedMessage(1, "encrypted_body_for_$userId", 123)
    }

    private fun mockEnsureSession(userId: String) {
        // Simulate network/IO work or synchronization
        // In real world, this could take significant time if it hits the network
    }

    @Test
    fun benchmarkSequentialEncryption() {
        println("Starting Sequential Benchmark with ${otherParticipants.size} participants...")

        val time = measureTimeMillis {
            val payloadMap = mutableMapOf<String, JsonElement>()
            for (otherUserId in otherParticipants) {
                mockEnsureSession(otherUserId)
                val encryptedForReceiver = mockEncryptMessage(otherUserId)
                payloadMap[otherUserId] = Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
            }

            val jsonPayload = buildJsonObject {
                put("content", content)
            }.toString()
            payloadMap["currentUserId"] = JsonPrimitive(jsonPayload)

            val payloads = JsonObject(payloadMap)
            val encryptedContent = payloads.toString()
        }

        println("Sequential Encryption took: ${time}ms")
    }

    @Test
    fun benchmarkParallelEncryption() = runTest {
        println("Starting Parallel Benchmark with ${otherParticipants.size} participants...")

        val time = measureTimeMillis {
            val payloads = coroutineScope {
                val encryptedPayloads = otherParticipants.map { otherUserId ->
                    async {
                        mockEnsureSession(otherUserId)
                        val encryptedForReceiver = mockEncryptMessage(otherUserId)
                        otherUserId to Json.encodeToJsonElement(EncryptedMessage.serializer(), encryptedForReceiver)
                    }
                }.awaitAll()

                buildJsonObject {
                    encryptedPayloads.forEach { (userId, element) ->
                        put(userId, element)
                    }
                    val jsonPayload = buildJsonObject {
                        put("content", content)
                    }.toString()
                    put("currentUserId", jsonPayload)
                }
            }
        }

        println("Parallel Encryption took: ${time}ms")
    }
}
