package com.thebifel.bubnilka.core.model

const val MAX_STYLE_ID_LENGTH = 100
const val MAX_STYLE_TITLE_LENGTH = 60
const val MAX_STYLE_INSTRUCTION_LENGTH = 4_000

data class StylePreset(
    val id: String,
    val title: String,
    val instruction: String,
    val isDefault: Boolean = false,
) {
    init {
        require(id.isNotBlank() && id.length <= MAX_STYLE_ID_LENGTH)
        require(title.isNotBlank() && title.length <= MAX_STYLE_TITLE_LENGTH)
        require(instruction.isNotBlank() && instruction.length <= MAX_STYLE_INSTRUCTION_LENGTH)
    }

    companion object {
        val STRUCTURED = StylePreset(
            id = "structured",
            title = "Структуровано",
            instruction = "Зберігай лексику, тон, характерні вислови та манеру мовлення автора всюди, де це не шкодить ясності. Не транслітеруй явні іншомовні слова, назви, терміни або фрагменти коду: записуй їх мовою оригіналу. Вирізай слова-паразити, беззмістовні повтори, незавершене бурмотіння та сторонні коментарі, які не стосуються посилу. Виправ пунктуацію, поділи думки на доречні абзаци, а для явних переліків додай маркери або нумерацію. Не скорочуй змістовні деталі й не змінюй намір автора.",
            isDefault = true,
        )
        val CONCISE = StylePreset(
            id = "concise",
            title = "Коротко",
            instruction = "Скороти текст до суті, прибери повтори й слова-паразити.",
        )
        val PROFESSIONAL = StylePreset(
            id = "professional",
            title = "Ділово",
            instruction = "Переформулюй у ясному, ввічливому та професійному тоні.",
        )
        val FRIENDLY = StylePreset(
            id = "friendly",
            title = "Дружньо",
            instruction = "Переформулюй тепло, природно й невимушено, не змінюючи зміст.",
        )
        val VIKING = StylePreset(
            id = "viking",
            title = "Вікінг",
            instruction = "Передай той самий зміст як енергійне послання вікінга з легкою доречною стилізацією.",
        )

        val entries: List<StylePreset> = listOf(STRUCTURED, CONCISE, PROFESSIONAL, FRIENDLY, VIKING)
        val default: StylePreset = STRUCTURED

        fun fromId(id: String): StylePreset? = entries.firstOrNull { it.id == id }
    }
}
