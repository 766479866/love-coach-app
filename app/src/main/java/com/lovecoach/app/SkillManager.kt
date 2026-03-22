package com.lovecoach.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SkillManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SkillManagerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val skills = mutableListOf<Skill>()

    init {
        loadSkills()
    }

    fun getSkills(): List<Skill> = skills

    fun addSkill(skill: Skill) {
        skills.add(skill)
        saveSkills()
    }

    fun removeSkill(skillId: String) {
        skills.removeIf { it.id == skillId }
        saveSkills()
    }

    fun updateSkill(skill: Skill) {
        val index = skills.indexOfFirst { it.id == skill.id }
        if (index != -1) {
            skills[index] = skill
            saveSkills()
        }
    }

    fun getSkillById(skillId: String): Skill? = skills.find { it.id == skillId }

    fun isOpenClawEnabled(): Boolean {
        return sharedPreferences.getBoolean("openclaw_enabled", true)
    }

    fun setOpenClawEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("openclaw_enabled", enabled).apply()
    }

    fun isCustomSkillEnabled(): Boolean {
        return sharedPreferences.getBoolean("custom_skill_enabled", false)
    }

    fun setCustomSkillEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("custom_skill_enabled", enabled).apply()
    }

    private fun saveSkills() {
        val json = gson.toJson(skills)
        sharedPreferences.edit().putString("skills", json).apply()
    }

    private fun loadSkills() {
        val json = sharedPreferences.getString("skills", "")
        if (json?.isNotEmpty() == true) {
            val type = object : TypeToken<List<Skill>>() {}.type
            val loadedSkills = gson.fromJson<List<Skill>>(json, type)
            skills.addAll(loadedSkills)
        }
    }
}

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val enabled: Boolean
)
