package com.lovecoach.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lovecoach.app.databinding.ActivityMainBinding
import com.lovecoach.app.databinding.DialogChatNameBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private var selectedChatId: String? = null
    private var currentImageUri: Uri? = null

    // Camera variables
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Services
    private lateinit var analysisService: AnalysisService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var chatManager: ChatManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize services
        analysisService = AnalysisService(this)
        sharedPreferences = getSharedPreferences("LoveCoachPrefs", MODE_PRIVATE)
        chatManager = ChatManager(this)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup chat list
        setupChatList()

        // Setup button listeners
        setupButtonListeners()
    }

    private fun setupChatList() {
        chatAdapter = ChatAdapter(mutableListOf()) {
            selectedChatId = it.id
            // Load chat history and update UI
            loadChatHistory(it.id)
            Toast.makeText(this, "Selected chat: ${it.name}", Toast.LENGTH_SHORT).show()
        }

        binding.chatList.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Load chats from ChatManager
        val chats = chatManager.getChats()
        if (chats.isEmpty()) {
            // Add sample chat data if no chats exist
            val sampleChats = listOf(
                ChatData("1", "小美", "最近怎么样？", Date().time),
                ChatData("2", "小丽", "明天一起吃饭吗？", Date().time - 86400000),
                ChatData("3", "小芳", "周末有时间吗？", Date().time - 172800000)
            )
            sampleChats.forEach { chatManager.addChat(it) }
        }
        chatAdapter.addChats(chatManager.getChats())
    }

    private fun loadChatHistory(chatId: String) {
        // Load chat history from ChatManager
        val history = chatManager.getChatHistory(chatId)
        // Load previous analysis if exists
        val analysis = chatManager.getChatAnalysis(chatId)
        if (analysis != null) {
            binding.emotionAnalysis.text = "情绪：${analysis.emotion}"
            binding.contextAnalysis.text = "上下文：${analysis.context}"
            binding.suggestedReply.text = analysis.reply
        }
    }

    private fun setupButtonListeners() {
        // New chat button
        binding.btnNewChat.setOnClickListener {
            showNewChatDialog()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Upload screenshot button
        binding.btnUploadScreenshot.setOnClickListener {
            if (checkStoragePermission()) {
                openImagePicker()
            }
        }

        // Take screenshot button
        binding.btnTakeScreenshot.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            }
        }

        // Analyze button
        binding.btnAnalyze.setOnClickListener {
            if (selectedChatId == null) {
                Toast.makeText(this, getString(R.string.msg_no_chat_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentImageUri == null) {
                Toast.makeText(this, getString(R.string.msg_no_screenshot), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Start analysis
            analyzeChat()
        }

        // Copy reply button
        binding.btnCopyReply.setOnClickListener {
            val reply = binding.suggestedReply.text.toString()
            if (reply != "--") {
                copyToClipboard(reply)
                Toast.makeText(this, "回复已复制", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNewChatDialog() {
        val dialogBinding = DialogChatNameBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_chat_name))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.dialog_btn_ok)) { _, _ ->
                val chatName = dialogBinding.etChatName.text.toString().trim()
                if (chatName.isNotEmpty()) {
                    val newChat = ChatData(
                        UUID.randomUUID().toString(),
                        chatName,
                        "新聊天",
                        Date().time
                    )
                    chatManager.addChat(newChat)
                    chatAdapter.addChat(newChat)
                    selectedChatId = newChat.id
                }
            }
            .setNegativeButton(getString(R.string.dialog_btn_cancel), null)
            .create()
        dialog.show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun openCamera() {
        val previewView = PreviewView(this)
        val dialog = AlertDialog.Builder(this)
            .setView(previewView)
            .setPositiveButton("拍摄") { _, _ ->
                takePhoto()
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()

        // Setup camera preview
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({ 
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    currentImageUri = Uri.fromFile(photoFile)
                    Toast.makeText(this@MainActivity, "截图已保存", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "拍摄失败", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun analyzeChat() {
        // Show loading
        binding.btnAnalyze.text = getString(R.string.msg_analyzing)
        binding.btnAnalyze.isEnabled = false

        // Get API config
        val apiKey = sharedPreferences.getString("api_key", "")
        val modelUrl = sharedPreferences.getString("model_url", "")

        if (apiKey.isNullOrEmpty() || modelUrl.isNullOrEmpty()) {
            runOnUiThread { 
                binding.btnAnalyze.text = getString(R.string.btn_analyze)
                binding.btnAnalyze.isEnabled = true
                Toast.makeText(this, getString(R.string.msg_api_config_error), Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Start analysis
        Thread { 
            try {
                // Extract text from image
                val text = analysisService.extractTextFromImage(currentImageUri!!)
                
                // Analyze chat
                val result = analysisService.analyzeChat(text, apiKey, modelUrl)

                runOnUiThread { 
                    // Update UI with analysis results
                    binding.emotionAnalysis.text = "情绪：${result.emotion}"
                    binding.contextAnalysis.text = "上下文：${result.context}"
                    binding.suggestedReply.text = result.reply

                    // Save analysis result to ChatManager
                    chatManager.saveChatAnalysis(selectedChatId!!, result)

                    // Reset button
                    binding.btnAnalyze.text = getString(R.string.btn_analyze)
                    binding.btnAnalyze.isEnabled = true
                    Toast.makeText(this, getString(R.string.msg_analysis_complete), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { 
                    binding.btnAnalyze.text = getString(R.string.btn_analyze)
                    binding.btnAnalyze.isEnabled = true
                    Toast.makeText(this, getString(R.string.msg_analysis_error), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("回复", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun checkStoragePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
            return false
        }
        return true
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, getString(R.string.permission_camera_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            data?.data?.let {uri ->
                currentImageUri = uri
                Toast.makeText(this, "截图已选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_STORAGE_PERMISSION = 2
        private const val REQUEST_CAMERA_PERMISSION = 3
    }
}
