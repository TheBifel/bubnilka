package com.thebifel.bubnilka.debug

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File
import java.lang.ref.WeakReference

/** Debug bridge used by adb to inspect the IME window, which UIAutomator omits. */
object UiInspector {
    private var rootReference = WeakReference<View>(null)

    fun attach(root: View) {
        rootReference = WeakReference(root)
    }

    fun detach(root: View?) {
        if (rootReference.get() === root) rootReference.clear()
    }

    fun writeDump(context: Context): File {
        val output = File(context.filesDir, FILE_NAME)
        val root = rootReference.get()
        output.writeText(
            if (root == null) {
                "# Bubnilka IME tree\nstatus=not_attached\n"
            } else {
                buildString {
                    appendLine("# Bubnilka IME tree")
                    appendLine("# depth bounds center size flags class id text description")
                    appendView(root, depth = 0)
                }
            },
        )
        return output
    }

    private fun StringBuilder.appendView(view: View, depth: Int) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        val flags = buildList {
            if (view.isShown) add("visible") else add("hidden")
            if (view.isEnabled) add("enabled") else add("disabled")
            if (view.isClickable) add("clickable")
            if (view.isSelected) add("selected")
            if (view.hasFocus()) add("focused")
        }.joinToString(",")
        val id = runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("-")
        val text = (view as? TextView)?.text?.toString().orEmpty().oneLine()
        val description = view.contentDescription?.toString().orEmpty().oneLine()

        append("  ".repeat(depth))
        append("[$left,$top][$right,$bottom] ")
        append("center=${(left + right) / 2},${(top + bottom) / 2} ")
        append("size=${view.width}x${view.height} ")
        append("flags=$flags ")
        append("class=${view.javaClass.simpleName} id=$id")
        if (text.isNotBlank()) append(" text=\"").append(text).append('"')
        if (description.isNotBlank()) append(" desc=\"").append(description).append('"')
        appendLine()

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) appendView(view.getChildAt(index), depth + 1)
        }
    }

    private fun String.oneLine(): String = replace(Regex("\\s+"), " ").replace("\"", "'").trim()

    const val ACTION_DUMP = "com.thebifel.bubnilka.debug.DUMP_IME_UI"
    const val FILE_NAME = "ime-ui-tree.txt"
}
