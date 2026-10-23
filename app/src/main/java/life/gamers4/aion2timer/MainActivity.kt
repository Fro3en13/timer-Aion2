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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
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

val SERVER_ZONE: ZoneOffset = ZoneOffset.ofHours(2)
val COLOR_TIMER_YELLOW = Color(0xFFFFB74D)

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
    val iconResId: Int? = null,
    val calculateNextEpochMillis: () -> Long
)

object ScheduleRepository {
    val events = listOf(
        // Разлом континуума (привязан файл rift_continuum.png)
        GameSchedule(
            id = "rift_continuum",
            title = "Разлом континуума",
            category = EventCategory.RIFTS,
            scheduleDesc = "каждые 3 ч, начиная с 02:00",
            iconResId = R.drawable.rift_continuum
        ) {
            getNextIntervalTime(2, 3, 0)
        },
        // События
        GameSchedule("event_shigo", "Фестиваль шиго", EventCategory.EVENTS, "каждый час в :00") {
            getNextHourlyTime(0)
        },
        GameSchedule("event_invasion", "Вторжение из измерения", EventCategory.EVENTS, "каждый час в :30") {
            getNextHourlyTime(30)
        },
        // Дозорщик Кайра
        GameSchedule(
            id = "boss_kaira_watcher",
            title = "Дозорщик Кайра",
            category = EventCategory.BOSSES,
            scheduleDesc = "каждые 3 ч, начиная с 01:00",
            iconResId = R.drawable.boss_kaira
        ) {
            getNextIntervalTime(1, 3, 0)
        },
        GameSchedule("abyss_artifacts", "Захват артефактов", EventCategory.ABYSS, "пн, чт, сб в 21:00") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 0)
        },
        // Исполнитель Тамаса
        GameSchedule(
            id = "boss_tamasa",
            title = "Исполнитель Тамаса",
            category = EventCategory.BOSSES,
            scheduleDesc = "пн, чт, сб в 21:30",
            iconResId = R.drawable.boss_tamasa
        ) {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        // Исполнитель Агро
        GameSchedule(
            id = "boss_agro",
            title = "Исполнитель Агро",
            category = EventCategory.BOSSES,
            scheduleDesc = "пн, чт, сб в 21:30",
            iconResId = R.drawable.boss_agro
        ) {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        GameSchedule("boss_kaira_executor", "Исполнитель Кайра", EventCategory.BOSSES, "пн, чт, сб в 21:30") {
            getNextWeeklyTime(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        // Лорд-хранитель Нахма
        GameSchedule(
            id = "boss_nahma",
            title = "Лорд-хранитель Нахма",
            category = EventCategory.BOSSES,
            scheduleDesc = "вс, пт в 21:00",
            iconResId = R.drawable.boss_nahma
        ) {
            getNextWeeklyTime(listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 21, 0)
        },
        GameSchedule(
            id = "boss_nahma_enraged",
            title = "Разъяренный лорд-хранитель Нахма",
            category = EventCategory.BOSSES,
            scheduleDesc = "вс, пт в 21:00",
            iconResId = R.drawable.boss_nahma
        ) {
            getNextWeeklyTime(listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 21, 0)
        },
        // События
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
        try {
            val title = intent.getStringExtra("EXTRA_TITLE") ?: "Aion 2 Событие"
            val leadMin = intent.getIntExtra("EXTRA_LEAD", 5)
            val iconResId = intent.getIntExtra("EXTRA_ICON_RES", 0)
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

            if (iconResId != 0) {
                val bmp = BitmapFactory.decodeResource(context.resources, iconResId)
                if (bmp != null && bmp.width > 1) {
                    builder.setLargeIcon(bmp)
                }
            }

            nm.notify(title.hashCode(), builder.build())
        } catch (_: Throwable) {}
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aion2_prefs", Context.MODE_PRIVATE) }

    var tab by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf(EventCategory.ALL) }
    var leadMin by remember { mutableIntStateOf(prefs.getInt("lead_min", 5)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val activeAlarms = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ScheduleRepository.events.forEach { ev ->
                put(ev.id, prefs.getBoolean("alarm_${ev.id}", false))
            }
        }
    }

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
                    // Верхняя панель: Сервер слева, логотип Aion 2 справа
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Сервер — EU (UTC+2)",
                            color = Color(0xFF90CAF9),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        SafeIcon(
                            resId = R.drawable.ic_logo,
                            contentDescription = "Aion 2 Логотип",
                            modifier = Modifier
                                .size(50.dp)
                                .padding(end = 4.dp)
                        )
                    }

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
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) COLOR_TIMER_YELLOW else Color(0xFF2C3242)
                                    ),
                                    modifier = Modifier.clickable {
                                        leadMin = mins
                                        prefs.edit().putInt("lead_min", mins).apply()
                                    }
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
                        items(list, key = { it.id }) { ev ->
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

                                    if (ev.iconResId != null) {
                                        SafeIcon(
                                            resId = ev.iconResId,
                                            contentDescription = ev.title,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .padding(horizontal = 4.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        val newState = !isOn
                                        activeAlarms[ev.id] = newState
                                        prefs.edit().putBoolean("alarm_${ev.id}", newState).apply()

                                        try {
                                            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                            val intent = Intent(context, AlarmReceiver::class.java).apply {
                                                putExtra("EXTRA_TITLE", ev.title)
                                                putExtra("EXTRA_LEAD", leadMin)
                                                if (ev.iconResId != null) {
                                                    putExtra("EXTRA_ICON_RES", ev.iconResId)
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
                                                    } catch (_: SecurityException) {
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
                                        } catch (_: Throwable) {}
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
fun SafeIcon(resId: Int, contentDescription: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(resId) {
        try {
            val bmp = BitmapFactory.decodeResource(context.resources, resId)
            if (bmp != null && bmp.width > 1) bmp.asImageBitmap() else null
        } catch (_: Throwable) {
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
            items(ScheduleRepository.events, key = { it.id }) { ev ->
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
                            textAlign = TextAlign.End
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
