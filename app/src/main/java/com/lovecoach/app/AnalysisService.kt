package com.lovecoach.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalysisService(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = textRecognizer.process(image).await()
        return@withContext result.text
    }

    suspend fun analyzeChat(text: String, apiKey: String, modelUrl: String): ChatAnalysisResult = withContext(Dispatchers.IO) {
        // Create API service
        val apiService = ApiService.create(modelUrl)

        // Prepare request
        val messages = listOf(
            Message(
                role = "system",
                content = "你是一位情场高手，擅长分析男女聊天内容并给出最适合的回复。请分析以下聊天内容，包括女生的情绪、上下文含义，并给出一个能够激发女生和我谈恋爱欲望的回复。分析要详细，回复要自然、有趣、有吸引力。"
            ),
            Message(
                role = "user",
                content = text
            )
        )

        val request = ChatAnalysisRequest(
            model = "gpt-3.5-turbo",
            messages = messages
        )

        // Call API
        val response = apiService.analyzeChat("Bearer $apiKey", request)

        // Parse response
        val analysisText = response.choices[0].message.content
        return@withContext parseAnalysisResult(analysisText)
    }

    private fun parseAnalysisResult(text: String): ChatAnalysisResult {
        // Simple parsing logic - in a real app, you would use more sophisticated parsing
        val lines = text.split("\n")
        var emotion = ""
        var context = ""
        var reply = ""

        var currentSection = ""
        for (line in lines) {
            when {
                line.contains("情绪：") -> {
                    currentSection = "emotion"
                    emotion = line.substringAfter("情绪：").trim()
                }
                line.contains("上下文：") -> {
                    currentSection = "context"
                    context = line.substringAfter("上下文：").trim()
                }
                line.contains("回复：") -> {
                    currentSection = "reply"
                    reply = line.substringAfter("回复：").trim()
                }
                currentSection == "emotion" && line.isNotEmpty() -> {
                    emotion += " " + line.trim()
                }
                currentSection == "context" && line.isNotEmpty() -> {
                    context += " " + line.trim()
                }
                currentSection == "reply" && line.isNotEmpty() -> {
                    reply += " " + line.trim()
                }
            }
        }

        return ChatAnalysisResult(emotion, context, reply)
    }
}

data class ChatAnalysisResult(
    val emotion: String,
    val context: String,
    val reply: String
)
