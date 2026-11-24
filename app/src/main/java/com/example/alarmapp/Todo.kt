package com.example.alarmapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A group of tasks, like "Morning routine". sourceApp is set when a connected app created it. */
data class TodoCategory(val id: Int, val name: String, val sourceApp: String? = null) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("sourceApp", sourceApp ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject) = TodoCategory(
            id = o.getInt("id"),
            name = o.getString("name"),
            sourceApp = if (o.isNull("sourceApp")) null else o.getString("sourceApp"),
        )
    }
}

data class TodoTask(
    val id: Int,
    val categoryId: Int,
    val title: String,
    val done: Boolean = false, // only ever changed by the user ticking the task, never by an alarm
    val sourceApp: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("categoryId", categoryId)
        .put("title", title)
        .put("done", done)
        .put("sourceApp", sourceApp ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject) = TodoTask(
            id = o.getInt("id"),
            categoryId = o.getInt("categoryId"),
            title = o.getString("title"),
            done = o.getBoolean("done"),
            sourceApp = if (o.isNull("sourceApp")) null else o.getString("sourceApp"),
        )
    }
}

/** Local storage, so lists and ticks work offline and on the lock screen. */
class TodoRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("todo", Context.MODE_PRIVATE)

    fun categories(): List<TodoCategory> = read("categories").map { TodoCategory.fromJson(it) }
    fun tasks(): List<TodoTask> = read("tasks").map { TodoTask.fromJson(it) }
    fun tasksIn(categoryId: Int): List<TodoTask> = tasks().filter { it.categoryId == categoryId }
    fun category(id: Int): TodoCategory? = categories().find { it.id == id }
    fun task(id: Int): TodoTask? = tasks().find { it.id == id }

    /** Pass id = 0 to create. Returns the saved category with its id. */
    fun saveCategory(category: TodoCategory): TodoCategory {
        val saved = if (category.id == 0) category.copy(id = nextId("next_category_id")) else category
        val list = categories()
        val updated = if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
        write("categories", updated.map { it.toJson() })
        return saved
    }

    fun saveTask(task: TodoTask): TodoTask {
        val saved = if (task.id == 0) task.copy(id = nextId("next_task_id")) else task
        val list = tasks()
        val updated = if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
        write("tasks", updated.map { it.toJson() })
        return saved
    }

    fun setDone(taskId: Int, done: Boolean) {
        write("tasks", tasks().map { if (it.id == taskId) it.copy(done = done) else it }.map { it.toJson() })
    }

    fun deleteTask(id: Int) = write("tasks", tasks().filter { it.id != id }.map { it.toJson() })

    fun deleteCategory(id: Int) {
        write("categories", categories().filter { it.id != id }.map { it.toJson() })
        write("tasks", tasks().filter { it.categoryId != id }.map { it.toJson() })
    }

    private fun read(key: String): List<JSONObject> {
        val arr = JSONArray(prefs.getString(key, "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun write(key: String, items: List<JSONObject>) {
        prefs.edit().putString(key, JSONArray(items).toString()).apply()
    }

    private fun nextId(key: String): Int {
        val n = prefs.getInt(key, 1)
        prefs.edit().putInt(key, n + 1).apply()
        return n
    }
}
