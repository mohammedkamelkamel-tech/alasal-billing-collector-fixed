sed -i 's/runBlocking { val response = processSyncRequest(request); writer.println(response) }/kotlinx.coroutines.runBlocking { val response = processSyncRequest(request); writer.println(response) }/g' app/src/main/java/com/example/service/WifiSyncManager.kt
sed -i '/writer.println(response)/d' app/src/main/java/com/example/service/WifiSyncManager.kt
sed -i '/val request = reader.readLine()/a\
                    if (request != null) {\n                        kotlinx.coroutines.runBlocking { \n                            val response = processSyncRequest(request)\n                            writer.println(response)\n                        }\n                    }' app/src/main/java/com/example/service/WifiSyncManager.kt
