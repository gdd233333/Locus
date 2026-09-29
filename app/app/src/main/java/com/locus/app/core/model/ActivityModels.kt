package com.locus.app.core.model

data class Activity(
    val id: Long = 0,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val category: ActivityCategory,
    val tags: List<String> = emptyList(),
    val isBuiltIn: Boolean = true,
)

enum class ActivityCategory(val displayName: String) {
    EMERGENCY("冲动急救"),
    QUICK("5 分钟"),
    MEDIUM("30 分钟"),
    OUTDOOR("出门走走"),
    CREATIVE("动手创作"),
    INPUT("输入"),
    ENVIRONMENT("环境整理"),
    EXPRESSION("表达"),
}
