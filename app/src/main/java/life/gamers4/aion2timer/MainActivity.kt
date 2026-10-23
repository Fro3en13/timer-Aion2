package life.gamers4.aion2timer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// Фиксированный часовой пояс сервера EU (UTC+2)
val SERVER_ZONE: ZoneOffset = ZoneOffset.ofHours(2)
val COLOR_TIMER_YELLOW = Color(0xFFFFB74D)

// Валидная оригинальная HD-иконка Дозорщика Кайры
const val ICON_KAIRA_WEBP = "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"

enum class EventCategory(val label: String) {
    ALL("Все"),
    RIFTS("Разломы"),
    BOSSES("Мировые боссы"),
    ABYSS("Abyss"),
    EVENTS("События")
}

data class GameSchedule(
    val id: String,
    val title: String,
    val category: EventCategory,
    val scheduleDesc: String,
    val iconBase64: String? = null,
    val calculateNextEpochMillis: () -> Long
)

object ScheduleRepository {
    val events = listOf(
        GameSchedule("rift_continuum", "Разлом континуума", EventCategory.RIFTS, "каждые 3 ч, начиная с 02:00") {
            getNextIntervalTime(2, 3, 0)
        },
        GameSchedule("event_shigo", "Фестиваль шиго", EventCategory.EVENTS, "каждый час в :00") {
            getNextHourlyTime(0)
        },
        GameSchedule("event_invasion", "Вторжение из измерения", EventCategory.EVENTS, "каждый час в :30") {
            getNextHourlyTime(30)
        },
        // Дозорщик Кайра с чистой иконкой
        GameSchedule(
            id = "boss_kaira_watcher",
            title = "Дозорщик Кайра",
            category = EventCategory.BOSSES,
            scheduleDesc = "каждые 3 ч, начиная с 01:00",
            iconBase64 = ICON_KAIRA_WEBP
        ) {
            getNextIntervalTime(1, 3, 0)
        },
        GameSchedule("abyss_artifacts", "Захват артефактов", EventCategory.ABYSS, "пн, чт, сб в 21:00") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 0)
        },
        GameSchedule("boss_tamasa", "Исполнитель Тамаса", EventCategory.BOSSES, "пн, чт, сб в 21:30") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        GameSchedule("boss_agro", "Исполнитель Агро", EventCategory.BOSSES, "пн, чт, сб в 21:30") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        GameSchedule("boss_kaira_executor", "Исполнитель Кайра", EventCategory.BOSSES, "пн, чт, сб в 21:30") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        GameSchedule("boss_nahma", "Лорд-хранитель Нахма", EventCategory.BOSSES, "вс, пт в 21:00") {
            getNextWeeklyTime(listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 21, 0)
        },
        GameSchedule("boss_nahma_enraged", "Разъяренный лорд-хранитель Нахма", EventCategory.BOSSES, "вс, пт в 21:00") {
            getNextWeeklyTime(listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 21, 0)
        },
        GameSchedule("event_arena", "Арена стратегии", EventCategory.EVENTS, "ежедневно в 11:00, 19:00") {
            getNextDailyTimes(listOf(11 to 0, 19 to 0))
        },
        GameSchedule("event_daily_reset", "Ежедневный сброс", EventCategory.EVENTS, "ежедневно в 03:00") {
            getNextDailyTimes(listOf(3 to 0))
        },
        GameSchedule("event_weekly_reset", "Еженедельный сброс", EventCategory.EVENTS, "ср в 03:00") {
            getNextWeeklyTime(listOf(DayOfWeek.WEDNESDAY), 3, 0)
        }
    )

    private fun getNextIntervalTime(startHour: Int, intervalHours: Int, minute: Int): Long {
        val now = ZonedDateTime.now(SERVER_ZONE)
        var target = now.withMinute(minute).withSecond(0).withNano(0)
        val hours = (startHour until 24 step intervalHours).toList()
        val nextHour = hours.firstOrNull { it > now.hour || (it == now.hour && now.minute < minute) }
        target = if (nextHour != null) target.withHour(nextHour) else target.plusDays(1).withHour(hours.first())
        return target.toInstant().toEpochMilli()
    }

    private fun getNextHourlyTime(minute: Int): Long {
        val now = ZonedDateTime.now(SERVER_ZONE)
        var target = now.withMinute(minute).withSecond(0).withNano(0)
        if (now.minute >= minute) target = target.plusHours(1)
        return target.toInstant().toEpochMilli()
    }

    private fun getNextDailyTimes(times: List<Pair<Int, Int>>): Long {
        val now = ZonedDateTime.now(SERVER_ZONE)
        val candidates = times.map { (h, m) ->
            var dt = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
            if (dt.isBefore(now)) dt = dt.plusDays(1)
            dt
        }.sorted()
        return candidates.first().toInstant().toEpochMilli()
    }

    private fun getNextWeeklyTime(days: List<DayOfWeek>, hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now(SERVER_ZONE)
        val candidates = days.map { day ->
            var dt = now.with(TemporalAdjusters.nextOrSame(day)).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (dt.isBefore(now)) dt = dt.plusWeeks(1)
            dt
        }
        return candidates.minOrNull()!!.toInstant().toEpochMilli()
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Aion 2 Событие"
        val leadMin = intent.getIntExtra("EXTRA_LEAD", 5)
        val iconB64 = intent.getStringExtra("EXTRA_ICON")
        val channelId = "aion2_alert_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Aion 2 Оповещения",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о событиях и боссах"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("Событие начнется через $leadMin мин!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (!iconB64.isNullOrEmpty()) {
            try {
                val clean = iconB64.replace("\\s+".toRegex(), "")
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) builder.setLargeIcon(bmp)
            } catch (_: Exception) {}
        }

        nm.notify(title.hashCode(), builder.build())
    }
}

class MainActivity : ComponentActivity() {
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { AppScreen() }
    }
}

@Composable
fun AppScreen() {
    var tab by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf(EventCategory.ALL) }
    var leadMin by remember { mutableIntStateOf(5) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val activeAlarms = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF131722)) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Timer, "Таймеры") },
                    label = { Text("Таймеры") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.ListAlt, "Полное расписание") },
                    label = { Text("Полное расписание") }
                )
            }
        }
    ) { p ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E1118))
                .padding(p)
        ) {
            if (tab == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Text(
                        text = "Сервер — EU (UTC+2)",
                        color = Color(0xFF90CAF9),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Оповестить за:", color = Color.LightGray, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 3, 5, 10).forEach { mins ->
                                val isSelected = leadMin == mins
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF233044) else Color(0xFF181C26),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) COLOR_TIMER_YELLOW else Color(0xFF2C3242)
                                    ),
                                    modifier = Modifier.clickable { leadMin = mins }
                                ) {
                                    Text(
                                        text = "$mins мин",
                                        color = if (isSelected) COLOR_TIMER_YELLOW else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        items(EventCategory.values()) { c ->
                            FilterChip(
                                selected = c == category,
                                onClick = { category = c },
                                label = { Text(c.label, fontSize = 12.sp) }
                            )
                        }
                    }

                    val list = ScheduleRepository.events.filter { category == EventCategory.ALL || it.category == category }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list) { ev ->
                            val next = ev.calculateNextEpochMillis()
                            val diff = (next - now) / 1000
                            val isOn = activeAlarms[ev.id] == true

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = ev.scheduleDesc,
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                        val timeStr = if (diff <= 0) "● В эфире" else "через " + formatSec(diff)
                                        Text(
                                            text = timeStr,
                                            color = if (diff <= 0) Color(0xFF00E676) else COLOR_TIMER_YELLOW,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    // Отображение иконки босса
                                    if (!ev.iconBase64.isNullOrEmpty()) {
                                        Base64IconImage(
                                            base64Str = ev.iconBase64,
                                            contentDescription = ev.title,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .padding(horizontal = 4.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        val newState = !isOn
                                        activeAlarms[ev.id] = newState
                                        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                                            putExtra("EXTRA_TITLE", ev.title)
                                            putExtra("EXTRA_LEAD", leadMin)
                                            if (ev.iconBase64 != null) {
                                                putExtra("EXTRA_ICON", ev.iconBase64)
                                            }
                                        }
                                        val pi = PendingIntent.getBroadcast(
                                            context,
                                            ev.id.hashCode(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )

                                        if (newState) {
                                            val triggerTime = next - (leadMin * 60 * 1000L)
                                            if (triggerTime > System.currentTimeMillis()) {
                                                try {
                                                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                                                } catch (e: SecurityException) {
                                                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                                                }
                                                Toast.makeText(context, "Оповещение включено: ${ev.title} (за $leadMin мин)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Событие начнется быстрее, чем за $leadMin мин!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            am.cancel(pi)
                                            Toast.makeText(context, "Оповещение выключено: ${ev.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isOn) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                            contentDescription = "Колокольчик",
                                            tint = if (isOn) COLOR_TIMER_YELLOW else Color.Gray,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                FullScheduleScreen()
            }
        }
    }
}

@Composable
fun Base64IconImage(base64Str: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(base64Str) {
        try {
            val clean = base64Str.replace("\\s+".toRegex(), "")
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

@Composable
fun FullScheduleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            text = "Полное расписание (время сервера) · EU · UTC+2",
            color = COLOR_TIMER_YELLOW,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ScheduleRepository.events) { ev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151923)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(ev.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(ev.category.label, color = Color(0xFF64B5F6), fontSize = 11.sp)
                        }
                        Text(
                            text = ev.scheduleDesc,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

fun formatSec(s: Long): String {
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%dh %dm %02ds", h, m, sec) else String.format(Locale.US, "%dm %02ds", m, sec)
}
