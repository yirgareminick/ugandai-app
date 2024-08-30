

package com.ugandai.chatgptbot.chat.data.api

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.client.OpenAI
import com.ugandai.chatgptbot.chat.data.Conversation
import com.ugandai.chatgptbot.chat.data.Message
import com.ugandai.chatgptbot.chat.data.MessageStatus
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(BetaOpenAI::class)
class OpenAIRepository(private val openAI: OpenAI) {

    @Throws(NoChoiceAvailableException::class)
    suspend fun sendChatRequest(
        conversation: Conversation,
        userInput: String // Accept the user input as a parameter
    ): Message {
        val instructions = "- You are an assistant helping farmers in rural Uganda make better decisions about planting crops\n" +
                "- Only refer to the information provided in the files; crops.json, buyangaWeather.json, mbaleWeather.json, namutumbaWeather.json\n"

        var contentString: String

        // Execute network operation on IO thread
        contentString = withContext(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:8000/chats")
                val con = url.openConnection() as HttpURLConnection

                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json; utf-8")
                con.setRequestProperty("Accept", "application/json")
                con.doOutput = true

                // Create the JSON input string with dynamic user input
                val jsonInputString = """{"sender": "user", "content": "$userInput"}"""
                DataOutputStream(con.outputStream).use { out ->
                    out.writeBytes(jsonInputString)
                    out.flush()
                }

                BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8)).use { reader ->
                    val content = StringBuilder()
                    var inputLine: String?
                    while (reader.readLine().also { inputLine = it } != null) {
                        content.append(inputLine)
                    }
                    val responseString = content.toString()

                    val jsonObject = JSONObject(responseString)
                    jsonObject.getString("content")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }

        return Message(
            text = contentString,
            isFromUser = false,  // Set this to false to indicate it's from the AI, not the user
            messageStatus = MessageStatus.Sent
        )
    }

    private fun Conversation.toChatMessages() = this.list
        .filterNot { it.messageStatus == MessageStatus.Error }
        .map {
            ChatMessage(
                content = it.text,
                role = if (it.isFromUser) ChatRole.User else ChatRole.Assistant
            )
        }
}

class NoChoiceAvailableException : Exception()