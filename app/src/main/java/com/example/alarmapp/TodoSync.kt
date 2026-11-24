package com.example.alarmapp

import android.content.Context
import org.json.JSONObject

/**
 * Keeps categories and tasks created by connected apps in step with the server.
 * Server items are stored locally with id = SERVER_ID_OFFSET + serverId, like alarms.
 * Ticks the user makes are sent back to the server (and queued while offline).
 */
object TodoSync {
    private const val PENDING = "pending_task_done"
    private val lock = Any()

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("todo_sync", Context.MODE_PRIVATE)

    private fun pending(c: Context): JSONObject = JSONObject(prefs(c).getString(PENDING, "{}")!!)

    /** The user ticked or unticked a task. Saved at once; connected apps hear about it as soon as the server is reachable. */
    fun setDone(context: Context, task: TodoTask, done: Boolean) {
        TodoRepository(context).setDone(task.id, done)
        if (task.id >= AlarmSync.SERVER_ID_OFFSET) {
            synchronized(lock) {
                val p = pending(context)
                p.put((task.id - AlarmSync.SERVER_ID_OFFSET).toString(), done)
                prefs(context).edit().putString(PENDING, p.toString()).apply()
            }
            val app = context.applicationContext
            Thread { runCatching { flushPending(app) } }.start()
        }
    }

    /** Sends queued ticks; anything that fails stays queued for next time. */
    fun flushPending(context: Context) {
        val snapshot = synchronized(lock) { pending(context) }
        val sent = mutableListOf<Pair<String, Boolean>>()
        for (key in snapshot.keys().asSequence().toList()) {
            val value = snapshot.getBoolean(key)
            try {
                ApiClient.sendTaskDone(context, key.toInt(), value)
                sent.add(key to value)
            } catch (e: Exception) {
                break
            }
        }
        synchronized(lock) {
            val p = pending(context)
            sent.forEach { (key, value) -> if (p.has(key) && p.getBoolean(key) == value) p.remove(key) }
            prefs(context).edit().putString(PENDING, p.toString()).apply()
        }
    }

    /** Blocking. Mirrors the server's categories and tasks for this phone. */
    fun sync(context: Context) {
        flushPending(context)
        val data = ApiClient.fetchTodos(context)
        val cats = data.getJSONArray("categories")
        val tasks = data.getJSONArray("tasks")
        val repo = TodoRepository(context)
        val off = AlarmSync.SERVER_ID_OFFSET

        val remoteCatIds = (0 until cats.length()).map { off + cats.getJSONObject(it).getInt("id") }.toSet()
        val remoteTaskIds = (0 until tasks.length()).map { off + tasks.getJSONObject(it).getInt("id") }.toSet()

        repo.tasks().filter { it.id >= off && it.id !in remoteTaskIds }.forEach { deleteTaskLocal(context, it.id) }
        repo.categories().filter { it.id >= off && it.id !in remoteCatIds }.forEach { deleteCategoryLocal(context, it.id) }

        for (i in 0 until cats.length()) upsertCategory(context, cats.getJSONObject(i))
        for (i in 0 until tasks.length()) upsertTask(context, tasks.getJSONObject(i))
    }

    fun upsertCategory(context: Context, o: JSONObject) {
        TodoRepository(context).saveCategory(
            TodoCategory(
                id = AlarmSync.SERVER_ID_OFFSET + o.getInt("id"),
                name = o.getString("name"),
                sourceApp = o.optString("source_app").ifBlank { null },
            )
        )
    }

    fun upsertTask(context: Context, o: JSONObject) {
        val serverId = o.getInt("id")
        val id = AlarmSync.SERVER_ID_OFFSET + serverId
        val repo = TodoRepository(context)
        val existing = repo.task(id)
        val hasPendingTick = synchronized(lock) { pending(context).has(serverId.toString()) }
        // A tick the user made that hasn't reached the server yet wins over the server's older value.
        val done = if (hasPendingTick && existing != null) existing.done else o.getBoolean("done")
        repo.saveTask(
            TodoTask(
                id = id,
                categoryId = AlarmSync.SERVER_ID_OFFSET + o.getInt("category_id"),
                title = o.getString("title"),
                done = done,
                sourceApp = o.optString("source_app").ifBlank { null },
            )
        )
    }

    /** Deleting a category removes its tasks and unlinks alarms that showed it. */
    fun deleteCategoryLocal(context: Context, id: Int) {
        TodoRepository(context).deleteCategory(id)
        val alarms = AlarmRepository(context)
        alarms.getAll().filter { it.categoryId == id }.forEach { alarms.save(it.copy(categoryId = null, taskId = null)) }
    }

    fun deleteTaskLocal(context: Context, id: Int) {
        TodoRepository(context).deleteTask(id)
        val alarms = AlarmRepository(context)
        alarms.getAll().filter { it.taskId == id }.forEach { alarms.save(it.copy(taskId = null)) }
    }
}
