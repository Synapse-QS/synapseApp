     1	package com.synapse.social.studioasinc.shared.data.datasource
     2
     3	import com.synapse.social.studioasinc.shared.util.UUIDUtils
     4	import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
     5	import com.synapse.social.studioasinc.shared.data.dto.chat.MessageReactionDto
     6	import io.github.aakira.napier.Napier
     7	import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
     8	import io.github.jan.supabase.auth.auth
     9	import io.github.jan.supabase.postgrest.query.filter.FilterOperator
    10	import io.github.jan.supabase.realtime.channel
    11	import io.github.jan.supabase.realtime.decodeRecord
    12	import io.github.jan.supabase.realtime.postgresChangeFlow
    13	import io.github.jan.supabase.realtime.PostgresAction
    14	import io.github.jan.supabase.realtime.RealtimeChannel
    15
    16	import io.github.jan.supabase.realtime.realtime
    17	import io.github.jan.supabase.realtime.*
    18	import kotlinx.coroutines.Dispatchers
    19	import kotlinx.coroutines.IO
    20	import kotlinx.coroutines.channels.awaitClose
    21	import kotlinx.coroutines.flow.Flow
    22	import kotlinx.coroutines.flow.callbackFlow
    23	import kotlinx.coroutines.flow.map
    24	import kotlinx.coroutines.launch
    25	import kotlinx.coroutines.withContext
    26	import kotlinx.serialization.json.buildJsonObject
    27	import kotlinx.serialization.json.put
    28	import kotlinx.serialization.json.booleanOrNull
    29	import kotlinx.serialization.json.contentOrNull
    30	import kotlinx.serialization.json.jsonPrimitive
    31	import kotlin.coroutines.cancellation.CancellationException
    32
    33	internal class ChatRealtimeDataSource(private val client: SupabaseClientLib) {
    34
    35	    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id
    36
    37	    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) =
    38	        withContext(Dispatchers.IO) {
    39	            try {
    40	                val currentUserId = getCurrentUserId() ?: return@withContext
    41	                val channel = client.realtime.channel("chat-\$chatId")
    42
    43	                if (channel.status.value != io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED) {
    44	                    try {
    45	                        Napier.d("Subscribing to typing channel: chat-\$chatId")
    46	                        channel.subscribe(blockUntilSubscribed = true)
    47	                    } catch (e: Exception) {
    48	                        Napier.e("Error subscribing to typing channel", e)
    49	                    }
    50	                }
    51	                channel.track(buildJsonObject {
    52	                    put("user_id", currentUserId)
    53	                    put("is_typing", isTyping)
    54	                })
    55	            } catch (e: Exception) {
    56	                Napier.e("Error broadcasting typing status", e)
    57	            }
    58	        }
    59
    60	    fun subscribeToMessages(chatId: String): Flow<MessageDto> = callbackFlow {
    61	        val channelId = "chat-messages-\$chatId-\${UUIDUtils.randomUUID()}"
    62	        Napier.d("Creating channel: \$channelId")
    63	        val channel = client.realtime.channel(channelId)
    64	        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
    65	            table = "messages"
    66	            filter("chat_id", FilterOperator.EQ, chatId)
    67	        }
    68
    69	        val collector = launch {
    70	            kotlinx.coroutines.yield()
    71	            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
    72	                trySend(message)
    73	            }
    74	        }
    75
    76	        launch(Dispatchers.IO) {
    77	            kotlinx.coroutines.yield()
    78	            try {
    79	                Napier.d("Subscribing to channel: \$channelId")
    80	                channel.subscribe()
    81	            } catch (e: Exception) {
    82	                if (e !is CancellationException) {
    83	                    Napier.e("Failed to subscribe to chat", e)
    84	                    close(e)
    85	                }
    86	            }
    87	        }
    88
    89	        awaitClose {
    90	            Napier.d("Closing channel: \$channelId")
    91	            collector.cancel()
    92	            launch {
    93	            kotlinx.coroutines.yield()
    94	                try {
    95	                    channel.unsubscribe()
    96	                    client.realtime.removeChannel(channel)
    97	                } catch (e: Exception) {
    98	                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
    99	                }
   100	            }
   101	        }
   102	    }
   103
   104	    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> = callbackFlow {
   105	        val channelId = "inbox-updates-\${UUIDUtils.randomUUID()}"
   106	        Napier.d("Creating channel: \$channelId")
   107	        val channel = client.realtime.channel(channelId)
   108	        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
   109	            table = "messages"
   110	        }
   111
   112	        val collector = launch {
   113	            kotlinx.coroutines.yield()
   114	            flow.collect { action ->
   115	                try {
   116	                    val message = action.decodeRecord<MessageDto>()
   117	                    trySend(message)
   118	                } catch (e: Exception) {
   119	                    Napier.e("Error decoding real-time message in inbox", e)
   120	                }
   121	            }
   122	        }
   123
   124	        launch(Dispatchers.IO) {
   125	            kotlinx.coroutines.yield()
   126	            try {
   127	                Napier.d("Subscribing to channel: \$channelId")
   128	                channel.subscribe()
   129	            } catch (e: Exception) {
   130	                Napier.e("Failed to subscribe to inbox channel", e)
   131	                close(e)
   132	            }
   133	        }
   134
   135	        awaitClose {
   136	            Napier.d("Closing channel: \$channelId")
   137	            collector.cancel()
   138	            launch {
   139	            kotlinx.coroutines.yield()
   140	                try {
   141	                    channel.unsubscribe()
   142	                    client.realtime.removeChannel(channel)
   143	                } catch (e: Exception) {
   144	                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
   145	                }
   146	            }
   147	        }
   148	    }
   149
   150	    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> = callbackFlow {
   151	        val channelId = "chat-\$chatId-\${UUIDUtils.randomUUID()}"
   152	        Napier.d("Creating channel: \$channelId")
   153	        val channel = client.realtime.channel(channelId)
   154
   155	        val collector = launch {
   156	            kotlinx.coroutines.yield()
   157	            channel.presenceChangeFlow().collect { presenceChange ->
   158	                presenceChange.joins.values.forEach { presence ->
   159	                    try {
   160	                        val state = presence.state
   161	                        val userId = state["user_id"]?.jsonPrimitive?.contentOrNull
   162	                        val isTyping = state["is_typing"]?.jsonPrimitive?.booleanOrNull
   163
   164	                        if (userId != null && isTyping != null) {
   165	                            trySend(mapOf("user_id" to userId, "is_typing" to isTyping))
   166	                        }
   167	                    } catch (e: Exception) {
   168	                        Napier.e("Error decoding presence state", e)
   169	                    }
   170	                }
   171
   172	                presenceChange.leaves.values.forEach { presence ->
   173	                    try {
   174	                        val state = presence.state
   175	                        val userId = state["user_id"]?.jsonPrimitive?.contentOrNull
   176
   177	                        if (userId != null) {
   178	                            trySend(mapOf("user_id" to userId, "is_typing" to false))
   179	                        }
   180	                    } catch (e: Exception) {
   181	                        Napier.e("Error decoding presence leave state", e)
   182	                    }
   183	                }
   184	            }
   185	        }
   186
   187	        launch(Dispatchers.IO) {
   188	            kotlinx.coroutines.yield()
   189	            try {
   190	                Napier.d("Subscribing to channel: \$channelId")
   191	                channel.subscribe()
   192	            } catch (e: Exception) {
   193	                if (e !is CancellationException) {
   194	                    Napier.e("Failed to subscribe to chat presence", e)
   195	                    close(e)
   196	                }
   197	            }
   198	        }
   199
   200	        awaitClose {
   201	            Napier.d("Closing channel: \$channelId")
   202	            collector.cancel()
   203	            launch {
   204	            kotlinx.coroutines.yield()
   205	                try {
   206	                    channel.unsubscribe()
   207	                    client.realtime.removeChannel(channel)
   208	                } catch (e: Exception) {
   209	                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
   210	                }
   211	            }
   212	        }
   213	    }
   214
   215	    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> = callbackFlow {
   216	        val channelId = "read-receipts-\$chatId-\${UUIDUtils.randomUUID()}"
   217	        Napier.d("Creating channel: \$channelId")
   218	        val channel = client.realtime.channel(channelId)
   219	        val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
   220	            table = "messages"
   221	            filter("chat_id", FilterOperator.EQ, chatId)
   222	        }
   223
   224	        val collector = launch {
   225	            kotlinx.coroutines.yield()
   226	            flow.map { it.decodeRecord<MessageDto>() }.collect { message ->
   227	                trySend(message)
   228	            }
   229	        }
   230
   231	        launch(Dispatchers.IO) {
   232	            kotlinx.coroutines.yield()
   233	            try {
   234	                Napier.d("Subscribing to channel: \$channelId")
   235	                channel.subscribe()
   236	            } catch (e: Exception) {
   237	                if (e !is CancellationException) {
   238	                    Napier.e("Failed to subscribe to read receipts", e)
   239	                    close(e)
   240	                }
   241	            }
   242	        }
   243
   244	        awaitClose {
   245	            Napier.d("Closing channel: \$channelId")
   246	            collector.cancel()
   247	            launch {
   248	            kotlinx.coroutines.yield()
   249	                try {
   250	                    channel.unsubscribe()
   251	                    client.realtime.removeChannel(channel)
   252	                } catch (e: Exception) {
   253	                    Napier.w("Failed to unsubscribe/remove channel: \$channelId", e)
   254	                }
   255	            }
   256	        }
   257	    }
   258
   259	    fun subscribeToMessageReactions(): Flow<MessageReactionDto> = callbackFlow {
   260	        val channelId = "message-reactions-\${UUIDUtils.randomUUID()}"
   261	        val channel = client.realtime.channel(channelId)
   262	        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
   263	            table = "message_reactions"
   264	        }
   265
   266	        val collector = launch {
   267	            kotlinx.coroutines.yield()
   268	            flow.collect { action ->
   269	                when (action) {
   270	                    is PostgresAction.Insert -> try { trySend(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
   271	                    is PostgresAction.Update -> try { trySend(action.decodeRecord<MessageReactionDto>()) } catch(e: Exception) {}
   272	                    is PostgresAction.Delete -> try { trySend(action.decodeOldRecord<MessageReactionDto>()) } catch(e: Exception) {}
   273	                    else -> {}
   274	                }
   275	            }
   276	        }
   277
   278	        launch(Dispatchers.IO) {
   279	            kotlinx.coroutines.yield()
   280	            try {
   281	                channel.subscribe()
   282	            } catch (e: Exception) {
   283	                close(e)
   284	            }
   285	        }
   286
   287	        awaitClose {
   288	            collector.cancel()
   289	            launch {
   290	            kotlinx.coroutines.yield()
   291	                channel.unsubscribe()
   292	                client.realtime.removeChannel(channel)
   293	            }
   294	        }
   295	    }
   296	}
