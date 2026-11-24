package com.example.alarmapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** The Alarms | To-do switch under each home screen's title. */
@Composable
fun HomeTabs(todoSelected: Boolean, onAlarms: () -> Unit, onTodo: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("Alarms", !todoSelected, onAlarms)
        Pill("To-do", todoSelected, onTodo)
    }
}

@Composable
fun TodoHome(
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    onAlarmFor: (TodoCategory, TodoTask?) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val repo = remember { TodoRepository(context) }
    var categories by remember { mutableStateOf(repo.categories()) }
    var tasks by remember { mutableStateOf(repo.tasks()) }
    var showNew by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<TodoCategory?>(null) }

    fun refresh() {
        categories = repo.categories()
        tasks = repo.tasks()
    }

    // Picks up lists and ticks that arrive from connected apps.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            val c = repo.categories()
            val t = repo.tasks()
            if (c != categories) categories = c
            if (t != tasks) tasks = t
        }
    }

    Scaffold(
        containerColor = scheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New category", fontWeight = FontWeight.Medium) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("To-do", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = scheme.onSurfaceVariant)
                    }
                }
            }
            item { HomeTabs(todoSelected = true, onAlarms = onOpenAlarms, onTodo = {}) }

            if (categories.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No categories yet", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Make a category like \"Morning routine\", add tasks, then set an alarm that shows it. Apps you connect can add categories here too.",
                            fontSize = 14.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(categories, key = { it.id }) { cat ->
                    CategoryCard(
                        category = cat,
                        tasks = tasks.filter { it.categoryId == cat.id },
                        onToggle = { t ->
                            TodoSync.setDone(context, t, !t.done)
                            refresh()
                        },
                        onAddTask = { title ->
                            repo.saveTask(TodoTask(0, cat.id, title))
                            refresh()
                        },
                        onDeleteTask = { t ->
                            TodoSync.deleteTaskLocal(context, t.id)
                            refresh()
                        },
                        onAlarm = { t -> onAlarmFor(cat, t) },
                        onRename = { renaming = cat },
                        onDelete = {
                            TodoSync.deleteCategoryLocal(context, cat.id)
                            refresh()
                        },
                    )
                }
            }
        }
    }

    if (showNew) {
        NameDialog(
            title = "New category",
            initial = "",
            hint = "e.g. Morning routine",
            onDismiss = { showNew = false },
            onConfirm = { name ->
                repo.saveCategory(TodoCategory(0, name))
                refresh()
                showNew = false
            },
        )
    }
    renaming?.let { c ->
        NameDialog(
            title = "Rename category",
            initial = c.name,
            hint = "Category name",
            onDismiss = { renaming = null },
            onConfirm = { name ->
                repo.saveCategory(c.copy(name = name))
                refresh()
                renaming = null
            },
        )
    }
}

@Composable
private fun CategoryCard(
    category: TodoCategory,
    tasks: List<TodoTask>,
    onToggle: (TodoTask) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (TodoTask) -> Unit,
    onAlarm: (TodoTask?) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember(category.id) { mutableStateOf("") }
    val doneCount = tasks.count { it.done }

    fun submit() {
        if (draft.isNotBlank()) {
            onAddTask(draft.trim())
            draft = ""
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceVariant)
            .padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).then(if (category.sourceApp == null) Modifier.clickable(onClick = onRename) else Modifier)
            ) {
                Text(category.name, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (tasks.isEmpty()) "No tasks yet" else "$doneCount of ${tasks.size} done",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                )
                category.sourceApp?.let { Text("via $it", fontSize = 12.sp, color = scheme.primary) }
            }
            IconButton(onClick = { onAlarm(null) }) {
                Icon(Icons.Default.Notifications, contentDescription = "Set an alarm that shows this category", tint = scheme.onSurfaceVariant)
            }
            if (category.sourceApp == null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = scheme.onSurfaceVariant)
                }
            }
        }

        tasks.forEach { task ->
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(task) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = task.done, onCheckedChange = { onToggle(task) })
                Text(
                    task.title,
                    modifier = Modifier.weight(1f).alpha(if (task.done) 0.5f else 1f),
                    fontSize = 16.sp,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                )
                IconButton(onClick = { onAlarm(task) }) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Set an alarm for this task",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (task.sourceApp == null) {
                    IconButton(onClick = { onDeleteTask(task) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(200) },
                placeholder = { Text("Add a task") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            IconButton(onClick = { submit() }) {
                Icon(Icons.Default.Add, contentDescription = "Add task", tint = scheme.primary)
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, hint: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                placeholder = { Text(hint) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
