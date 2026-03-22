package com.lovecoach.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lovecoach.app.databinding.ActivitySettingsBinding
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var skillManager: SkillManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize SharedPreferences and SkillManager
        sharedPreferences = getSharedPreferences("LoveCoachPrefs", MODE_PRIVATE)
        skillManager = SkillManager(this)

        // Load saved settings
        loadSettings()

        // Setup button listeners
        setupButtonListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadSettings() {
        // Load API settings
        val apiKey = sharedPreferences.getString("api_key", "")
        val modelUrl = sharedPreferences.getString("model_url", "")
        binding.etApiKey.setText(apiKey)
        binding.etModelUrl.setText(modelUrl)

        // Load feature settings from SkillManager
        val openClawEnabled = skillManager.isOpenClawEnabled()
        val customSkillEnabled = skillManager.isCustomSkillEnabled()
        binding.switchOpenclaw.isChecked = openClawEnabled
        binding.switchCustomSkill.isChecked = customSkillEnabled
    }

    private fun setupButtonListeners() {
        // Save API config button
        binding.btnSaveApiConfig.setOnClickListener {
            saveApiConfig()
        }

        // OpenClaw switch
        binding.switchOpenclaw.setOnCheckedChangeListener { _, isChecked ->
            skillManager.setOpenClawEnabled(isChecked)
        }

        // Custom skill switch
        binding.switchCustomSkill.setOnCheckedChangeListener { _, isChecked ->
            skillManager.setCustomSkillEnabled(isChecked)
        }
    }

    private fun saveApiConfig() {
        val apiKey = binding.etApiKey.text.toString().trim()
        val modelUrl = binding.etModelUrl.text.toString().trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请输入API密钥", Toast.LENGTH_SHORT).show()
            return
        }

        if (modelUrl.isEmpty()) {
            Toast.makeText(this, "请输入模型API地址", Toast.LENGTH_SHORT).show()
            return
        }

        // Save to SharedPreferences
        sharedPreferences.edit()
            .putString("api_key", apiKey)
            .putString("model_url", modelUrl)
            .apply()

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
    }
}
