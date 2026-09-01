package com.thebifel.bubnilka.core.model

enum class StylePreset(
    val id: String,
    val title: String,
    val instruction: String,
    val isDefault: Boolean = false,
) {
    STRUCTURED(
        id = "structured",
        title = "Структуровано",
        instruction = "Стисло впорядкуй думки. Використовуй короткі абзаци або список, лише коли це доречно.",
        isDefault = true,
    ),
    CONCISE(
        id = "concise",
        title = "Коротко",
        instruction = "Скороти текст до суті, прибери повтори й слова-паразити.",
    ),
    PROFESSIONAL(
        id = "professional",
        title = "Ділово",
        instruction = "Переформулюй у ясному, ввічливому та професійному тоні.",
    ),
    FRIENDLY(
        id = "friendly",
        title = "Дружньо",
        instruction = "Переформулюй тепло, природно й невимушено, не змінюючи зміст.",
    ),
    VIKING(
        id = "viking",
        title = "Вікінг",
        instruction = "Передай той самий зміст як енергійне послання вікінга з легкою доречною стилізацією.",
    ),
    ;

    companion object {
        val default: StylePreset = entries.single { it.isDefault }

        fun fromId(id: String): StylePreset? = entries.firstOrNull { it.id == id }
    }
}
