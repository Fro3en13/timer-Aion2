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
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class ServerRegion(val title: String, val offset: ZoneOffset) {
    EU("EU (UTC+2)", ZoneOffset.ofHours(2)),
    NA_EAST("NA East (UTC-4)", ZoneOffset.ofHours(-4)),
    NA_WEST("NA West (UTC-7)", ZoneOffset.ofHours(-7)),
    SA("SA (UTC-3)", ZoneOffset.ofHours(-3)),
    JP("JP (UTC+9)", ZoneOffset.ofHours(9)),
    TW("TW (UTC+8)", ZoneOffset.ofHours(8)),
    KR("KR (UTC+9)", ZoneOffset.ofHours(9))
}

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
    val calculateNextEpochMillis: (region: ServerRegion) -> Long
)

object ScheduleRepository {
    val events = listOf(
        GameSchedule("rift_continuum", "Разлом континуума", EventCategory.RIFTS, "каждые 3 ч, с 02:00") { region ->
            getNextIntervalTime(region, 2, 3, 0)
        },
        GameSchedule("boss_kaira", "Дозорщик Кайра", EventCategory.BOSSES, "каждые 3 ч, с 01:00") { region ->
            getNextIntervalTime(region, 1, 3, 0)
        },
        GameSchedule("boss_tamasa", "Исполнитель Тамаса", EventCategory.BOSSES, "пн, чт, сб в 21:30") { region ->
            getNextWeeklyTime(region, listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 30)
        },
        GameSchedule("boss_nahma", "Лорд-хранитель Нахма", EventCategory.BOSSES, "вс, пт в 21:00") { region ->
            getNextWeeklyTime(region, listOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 21, 0)
        },
        GameSchedule("abyss_artifacts", "Захват артефактов", EventCategory.ABYSS, "пн, чт, сб в 21:00") { region ->
            getNextWeeklyTime(region, listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), 21, 0)
        },
        GameSchedule("event_shigo", "Фестиваль шиго", EventCategory.EVENTS, "каждый час в :00") { region ->
            getNextHourlyTime(region, 0)
        },
        GameSchedule("event_invasion", "Вторжение из измерения", EventCategory.EVENTS, "каждый час в :30") { region ->
            getNextHourlyTime(region, 30)
        },
        GameSchedule("event_arena", "Арена стратегии", EventCategory.EVENTS, "ежедневно в 11:00, 19:00") { region ->
            getNextDailyTimes(region, listOf(11 to 0, 19 to 0))
        }
    )

    private fun getNextIntervalTime(region: ServerRegion, startHour: Int, intervalHours: Int, minute: Int): Long {
        val now = ZonedDateTime.now(region.offset)
        var target = now.withMinute(minute).withSecond(0).withNano(0)
        val hours = (startHour until 24 step intervalHours).toList()
        val nextHour = hours.firstOrNull { it > now.hour || (it == now.hour && now.minute < minute) }
        target = if (nextHour != null) target.withHour(nextHour) else target.plusDays(1).withHour(hours.first())
        return target.toInstant().toEpochMilli()
    }

    private fun getNextHourlyTime(region: ServerRegion, minute: Int): Long {
        val now = ZonedDateTime.now(region.offset)
        var target = now.withMinute(minute).withSecond(0).withNano(0)
        if (now.minute >= minute) target = target.plusHours(1)
        return target.toInstant().toEpochMilli()
    }

    private fun getNextDailyTimes(region: ServerRegion, times: List<Pair<Int, Int>>): Long {
        val now = ZonedDateTime.now(region.offset)
        val sortedCandidates = times.map { (h, m) ->
            var dt = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
            if (dt.isBefore(now)) dt = dt.plusDays(1)
            dt
        }.sorted()
        return sortedCandidates.first().toInstant().toEpochMilli()
    }

    private fun getNextWeeklyTime(region: ServerRegion, days: List<DayOfWeek>, hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now(region.offset)
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
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Aion 2"
        val msg = intent.getStringExtra("EXTRA_MSG") ?: "Событие скоро начнется!"
        val channelId = "aion2_ch"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Aion 2 Напоминания", NotificationManager.IMPORTANCE_HIGH))
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚔️ $title")
            .setContentText(msg)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(title.hashCode(), notification)
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
    var region by remember { mutableStateOf(ServerRegion.EU) }
    var category by remember { mutableStateOf(EventCategory.ALL) }
    var leadMin by remember { mutableIntStateOf(5) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

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
                    icon = { Icon(Icons.Default.Language, "Сайт") },
                    label = { Text("Сайт и Карта") }
                )
            }
        }
    ) { p ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1118)).padding(p)) {
            if (tab == 0) {
                val context = LocalContext.current
                val alarms = remember { mutableStateMapOf<String, Boolean>() }

                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Text("Сервер:", color = Color.Gray, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(ServerRegion.values()) { r ->
                            FilterChip(selected = r == region, onClick = { region = r }, label = { Text(r.title, fontSize = 11.sp) })
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Оповестить за:", color = Color.LightGray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 3, 5, 10).forEach { m ->
                                SuggestionChip(
                                    onClick = { leadMin = m },
                                    label = { Text("$m мин", color = if (leadMin == m) Color.Cyan else Color.White) }
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(EventCategory.values()) { c ->
                            FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c.label) })
                        }
                    }

                    val list = ScheduleRepository.events.filter { category == EventCategory.ALL || it.category == category }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(list) { ev ->
                            val next = ev.calculateNextEpochMillis(region)
                            val diff = (next - now) / 1000
                            val on = alarms[ev.id] == true

                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C))) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(ev.scheduleDesc, color = Color.Gray, fontSize = 12.sp)
                                        val timeStr = if (diff <= 0) "● В эфире" else "через " + formatSec(diff)
                                        Text(timeStr, color = if (diff <= 0) Color.Green else Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    IconButton(onClick = {
                                        val state = !on
                                        alarms[ev.id] = state
                                        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                                            putExtra("EXTRA_TITLE", ev.title)
                                            putExtra("EXTRA_MSG", "Начнется через $leadMin мин!")
                                        }
                                        val pi = PendingIntent.getBroadcast(context, ev.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                        if (state) {
                                            val trigger = next - (leadMin * 60 * 1000)
                                            if (trigger > System.currentTimeMillis()) {
                                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                                            }
                                        } else {
                                            am.cancel(pi)
                                        }
                                    }) {
                                        Icon(if (on) Icons.Default.NotificationsActive else Icons.Default.Notifications, null, tint = if (on) Color.Cyan else Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                AndroidView(factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl("https://gamers4.life/aion-2/database/ru/events/")
                    }
                }, modifier = Modifier.fillMaxSize())
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
