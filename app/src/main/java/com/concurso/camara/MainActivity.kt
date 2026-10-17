package com.concurso.camara

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AlarmScheduler.scheduleAll(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        AlarmScheduler.scheduleAll(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFF1D4ED8),
                    background = Color(0xFFF1F5F9)
                )
            ) {
                AgendaCamaraScreen()
            }
        }
    }
}

fun openQConcursos(context: Context, url: String = AlertRepository.QCONCURSOS_URL) {
    QConcursosLauncher.open(context, url.ifBlank { AlertRepository.QCONCURSOS_URL })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaCamaraScreen() {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf(AlertRepository.getAlerts(context)) }
    var userProfile by remember { mutableStateOf(UserProfileRepository.getProfile(context)) }
    var selectedTab by remember {
        mutableIntStateOf(if (userProfile.name.isBlank()) 3 else 0)
    }
    var editingAlert by remember { mutableStateOf<StudyAlert?>(null) }
    var showNewAlertDialog by remember { mutableStateOf(false) }

    var manualPreviewMood by remember { mutableStateOf<MascotMood?>(null) }
    var hoursWithoutStudy by remember { mutableLongStateOf(StudyCheckInRepository.getHoursSinceLastCheckIn(context)) }
    var studyProgress by remember { mutableStateOf(StudyCheckInRepository.getProgress(context, userProfile)) }
    var showCheckInDialog by remember { mutableStateOf(false) }
    var checkInMinutes by remember { mutableStateOf("60") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            hoursWithoutStudy = StudyCheckInRepository.getHoursSinceLastCheckIn(context)
            studyProgress = StudyCheckInRepository.getProgress(context, userProfile)
        }
    }

    LaunchedEffect(userProfile.dailyCheckInGoal, userProfile.dailyStudyGoalHours) {
        studyProgress = StudyCheckInRepository.getProgress(context, userProfile)
    }

    val currentMood = manualPreviewMood ?: AlertRepository.getCurrentMascotMood(context)
    var dynamicMascotLine by remember(currentMood, hoursWithoutStudy, userProfile, studyProgress) {
        mutableStateOf(MascotIntelligence.previewMessage(context, currentMood, hoursWithoutStudy))
    }

    LaunchedEffect(currentMood, hoursWithoutStudy, userProfile, studyProgress) {
        dynamicMascotLine = withContext(Dispatchers.IO) {
            MascotIntelligence.generateMessage(
                context = context,
                mood = currentMood,
                subject = "estudo",
                hoursWithoutStudy = hoursWithoutStudy,
                fallback = MascotIntelligence.previewMessage(context, currentMood, hoursWithoutStudy)
            )
        }
    }

    val animatedBannerColor by animateColorAsState(
        targetValue = Color(currentMood.bgColorHex),
        label = "moodColor"
    )

    Scaffold(
        topBar = {
            Surface(
                color = animatedBannerColor,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MascotFaceCanvas(
                            mood = currentMood,
                            size = 82.dp,
                            modifier = Modifier.clickable {
                                val nextOrdinal = (currentMood.ordinal + 1) % MascotMood.entries.size
                                manualPreviewMood = MascotMood.entries[nextOrdinal]
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentMood.title,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dynamicMascotLine,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                manualPreviewMood = null
                                openQConcursos(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Abrir QConcursos Agora", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                checkInMinutes = "60"
                                showCheckInDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Já Estudei!", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showNewAlertDialog = true },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Novo Horário")
                    Spacer(Modifier.width(6.dp))
                    Text("Novo Horário", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (userProfile.name.isBlank()) {
                TabRow(selectedTabIndex = 0) {
                    Tab(
                        selected = true,
                        onClick = {},
                        text = { Text("Seu Perfil", fontWeight = FontWeight.Bold) }
                    )
                }
                UserProfileTab(
                    initialProfile = userProfile,
                    required = true,
                    onSave = { updatedProfile ->
                        UserProfileRepository.saveProfile(context, updatedProfile)
                        userProfile = updatedProfile
                        selectedTab = 0
                    },
                    onCancel = {}
                )
            } else {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Horários & Matérias", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Progresso", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Frases", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Perfil", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                AlertsTab(
                    alerts = alerts,
                    onToggle = { toggled ->
                        val updatedList = alerts.map {
                            if (it.id == toggled.id) it.copy(enabled = !it.enabled) else it
                        }
                        alerts = updatedList
                        AlertRepository.saveAlerts(context, updatedList)
                        AlarmScheduler.scheduleAll(context)
                    },
                    onEdit = { editingAlert = it },
                    onDelete = { toDelete ->
                        AlarmScheduler.cancelAlert(context, toDelete.id)
                        val updatedList = alerts.filter { it.id != toDelete.id }
                        alerts = updatedList
                        AlertRepository.saveAlerts(context, updatedList)
                    },
                    onOpenSimulado = { alert ->
                        manualPreviewMood = MascotMood.COOL
                        openQConcursos(context, alert.targetUrl)
                    },
                    onTestNotification = { alert ->
                        val testIntent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("ALERT_ID", alert.id)
                            putExtra("SUBJECT", alert.subject)
                            putExtra("HOUR", alert.hour)
                            putExtra("MINUTE", alert.minute)
                            putExtra("CUSTOM_MESSAGE", alert.customMessage)
                            putExtra("TARGET_URL", alert.targetUrl)
                        }
                        context.sendBroadcast(testIntent)
                        Toast.makeText(context, "Notificação enviada! Expanda para ler ou use Ir para QConcursos.", Toast.LENGTH_SHORT).show()
                    }
                )
                } else if (selectedTab == 1) {
                    StudyProgressTab(profile = userProfile, progress = studyProgress)
                } else if (selectedTab == 2) {
                    MessagesByPeriodTab()
                } else if (selectedTab == 3) {
                    UserProfileTab(
                        initialProfile = userProfile,
                        required = false,
                        onSave = { updatedProfile ->
                            UserProfileRepository.saveProfile(context, updatedProfile)
                            userProfile = updatedProfile
                            Toast.makeText(context, "Perfil salvo!", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = { selectedTab = 0 }
                    )
                }
            }
        }
    }

    if (showCheckInDialog) {
        AlertDialog(
            onDismissRequest = { showCheckInDialog = false },
            title = { Text("Registrar check-in", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = checkInMinutes,
                    onValueChange = { checkInMinutes = it.filter(Char::isDigit).take(3) },
                    label = { Text("Minutos estudados") },
                    supportingText = { Text("Informe de 1 a 720 minutos para esta sessão.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = checkInMinutes.toIntOrNull()
                        if (minutes == null || minutes !in 1..720) {
                            Toast.makeText(context, "Informe uma duração entre 1 e 720 minutos.", Toast.LENGTH_SHORT).show()
                        } else {
                            StudyCheckInRepository.recordCheckIn(context, minutes)
                            studyProgress = StudyCheckInRepository.getProgress(context, userProfile)
                            hoursWithoutStudy = 0L
                            manualPreviewMood = MascotMood.HAPPY
                            showCheckInDialog = false
                            Toast.makeText(context, "Check-in registrado. Boa sessão!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckInDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (editingAlert != null) {
        AlertEditorDialog(
            initialAlert = editingAlert!!,
            onDismiss = { editingAlert = null },
            onSave = { updated ->
                val newList = alerts.map { if (it.id == updated.id) updated else it }
                    .sortedWith(compareBy({ it.hour }, { it.minute }))
                alerts = newList
                AlertRepository.saveAlerts(context, newList)
                AlarmScheduler.scheduleAll(context)
                editingAlert = null
            }
        )
    }

    if (showNewAlertDialog) {
        val nextId = (alerts.maxOfOrNull { it.id } ?: 0) + 1
        AlertEditorDialog(
            initialAlert = StudyAlert(nextId, 0, 8, 0, "Nova Matéria", "", AlertRepository.QCONCURSOS_URL, true),
            onDismiss = { showNewAlertDialog = false },
            onSave = { created ->
                val newList = (alerts + created).sortedWith(compareBy({ it.hour }, { it.minute }))
                alerts = newList
                AlertRepository.saveAlerts(context, newList)
                AlarmScheduler.scheduleAll(context)
                showNewAlertDialog = false
            }
        )
    }
}

@Composable
fun AlertsTab(
    alerts: List<StudyAlert>,
    onToggle: (StudyAlert) -> Unit,
    onEdit: (StudyAlert) -> Unit,
    onDelete: (StudyAlert) -> Unit,
    onOpenSimulado: (StudyAlert) -> Unit,
    onTestNotification: (StudyAlert) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(alerts, key = { it.id }) { alert ->
            val period = AlertRepository.getPeriodForHour(alert.hour)
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(alert) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", alert.hour, alert.minute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = period.label,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = alert.enabled,
                            onCheckedChange = { onToggle(alert) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alert.subject,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    if (alert.customMessage.isNotBlank()) {
                        Text(
                            text = "💬 Frase extra: \"${alert.customMessage}\"",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onOpenSimulado(alert) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Simulado QConcursos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            IconButton(onClick = { onTestNotification(alert) }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Testar Notificação", tint = Color(0xFF10B981))
                            }
                            IconButton(onClick = { onEdit(alert) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF475569))
                            }
                            IconButton(onClick = { onDelete(alert) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesByPeriodTab() {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MORNING) }
    var messages by remember(selectedPeriod) {
        mutableStateOf(AlertRepository.getMessagesForPeriod(context, selectedPeriod))
    }
    var newMessage by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Frases do Turno (${messages.size}/${AlertRepository.MAX_MESSAGES_PER_PERIOD})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            TextButton(
                onClick = {
                    messages = AlertRepository.resetPeriodMessagesToDefault(context, selectedPeriod)
                    Toast.makeText(context, "Frases padrão restauradas!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restaurar Padrão", fontSize = 12.sp)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == selectedPeriod,
                    onClick = { selectedPeriod = period },
                    label = { Text(period.shortTitle, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = selectedPeriod.label,
            fontSize = 12.sp,
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                label = { Text("Adicionar nova frase ao array") },
                placeholder = { Text("Ex: Acorda, Mario Bonito!") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newMessage.isNotBlank()) {
                        if (messages.size >= AlertRepository.MAX_MESSAGES_PER_PERIOD) {
                            Toast.makeText(context, "Limite de ${AlertRepository.MAX_MESSAGES_PER_PERIOD} frases atingido!", Toast.LENGTH_SHORT).show()
                        } else {
                            val updated = messages + newMessage.trim()
                            messages = updated
                            AlertRepository.saveMessagesForPeriod(context, selectedPeriod, updated)
                            newMessage = ""
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(messages) { index, msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingIndex = index
                            editingText = msg
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${index + 1}. $msg",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(
                            onClick = {
                                editingIndex = index
                                editingText = msg
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar frase", tint = Color(0xFF3B82F6))
                        }
                        IconButton(
                            onClick = {
                                if (messages.size > 1) {
                                    val updated = messages.toMutableList().also { it.removeAt(index) }
                                    messages = updated
                                    AlertRepository.saveMessagesForPeriod(context, selectedPeriod, updated)
                                } else {
                                    Toast.makeText(context, "Mantenha ao menos 1 frase neste turno!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    if (editingIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Editar Frase #${editingIndex!! + 1}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    label = { Text("Texto da notificação") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        messages = AlertRepository.updateMessageAtIndex(
                            context,
                            selectedPeriod,
                            editingIndex!!,
                            editingText
                        )
                        editingIndex = null
                    }
                ) {
                    Text("Salvar Alteração")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun UserProfileTab(
    initialProfile: UserProfile,
    required: Boolean,
    onSave: (UserProfile) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember(initialProfile) { mutableStateOf(initialProfile.name) }
    var gender by remember(initialProfile) { mutableStateOf(initialProfile.gender) }
    var coachStyle by remember(initialProfile) { mutableStateOf(initialProfile.coachStyle) }
    var dailyCheckInGoal by remember(initialProfile) { mutableStateOf(initialProfile.dailyCheckInGoal.toString()) }
    var dailyStudyGoalHours by remember(initialProfile) { mutableStateOf(initialProfile.dailyStudyGoalHours.toString()) }
    var desiredPosition by remember(initialProfile) { mutableStateOf(initialProfile.desiredPosition) }
    val context = LocalContext.current

    fun profileToSave(): UserProfile = UserProfile(
        name = name.trim(),
        gender = gender,
        coachStyle = coachStyle,
        dailyCheckInGoal = dailyCheckInGoal.toIntOrNull()?.coerceIn(1, 10) ?: 3,
        dailyStudyGoalHours = dailyStudyGoalHours.replace(',', '.').toDoubleOrNull()?.coerceIn(1.0, 12.0) ?: 4.0,
        desiredPosition = desiredPosition.trim()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (required) "Como podemos chamar você?" else "Seu perfil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = desiredPosition,
            onValueChange = { desiredPosition = it.take(100) },
            label = { Text("Cargo desejado") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Gênero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        UserGender.entries.forEach { option ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (gender == option) Color(0xFFE8F5E9) else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { gender = option }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = gender == option,
                        onClick = { gender = option }
                    )
                    Text(option.label, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        Text("Estilo do coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        CoachStyle.entries.forEach { option ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (coachStyle == option) Color(0xFFE8F5E9) else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { coachStyle = option }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = coachStyle == option,
                        onClick = { coachStyle = option }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(option.label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (option == CoachStyle.DEMANDING) {
                                "Cobrança direta, com respeito e foco no prazo da prova."
                            } else {
                                "Incentivo leve, com metas possíveis e sem pressão."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = dailyCheckInGoal,
            onValueChange = { dailyCheckInGoal = it.filter(Char::isDigit).take(2) },
            label = { Text("Check-ins desejados por dia (1 a 10)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dailyStudyGoalHours,
            onValueChange = { dailyStudyGoalHours = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(5) },
            label = { Text("Meta de estudo por dia, em horas (1 a 12)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Cada dia sem check-in acrescenta sua meta diária à compensação, redistribuída até 17/01/2027.",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
        Text("Histórico de check-ins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        StudyCheckInRepository.getRecentHistory(context).forEach { day ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${day.date.dayOfMonth.toString().padStart(2, '0')}/${day.date.monthValue.toString().padStart(2, '0')}")
                    Text("${day.checkInCount} check-ins • ${day.minutesStudied / 60}h ${day.minutesStudied % 60}min")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!required) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
            }
            Button(
                onClick = { onSave(profileToSave()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Salvar perfil")
            }
        }
        Text(
            text = "Desenvolvido por Mario Henrique Cezar • versão 2.1",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.fillMaxWidth()
        )
    }

}

@Composable
private fun StudyProgressTab(profile: UserProfile, progress: StudyProgress) {
    val context = LocalContext.current
    val history = StudyCheckInRepository.getRecentHistory(context)
    val hoursSinceCheckIn = StudyCheckInRepository.getHoursSinceLastCheckIn(context)
    val streakLabel = if (progress.streakDays == 1) "dia" else "dias"
    val remainingHours = "%.1f".format(Locale.getDefault(), progress.hoursRemainingToday)
    val targetHours = "%.1f".format(Locale.getDefault(), progress.hoursRequiredToday)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Seu progresso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${progress.streakDays} $streakLabel de ofensiva", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${progress.checkInsToday} de ${profile.dailyCheckInGoal} check-ins hoje")
                Text("${progress.minutesStudiedToday / 60}h ${progress.minutesStudiedToday % 60}min estudados hoje")
            }
        }
        Text(
            text = when {
                hoursSinceCheckIn == 0L -> "Seu último check-in foi há menos de uma hora."
                hoursSinceCheckIn < 24 -> "Seu último check-in foi há $hoursSinceCheckIn horas."
                else -> "Seu último check-in foi há ${hoursSinceCheckIn / 24} dias."
            },
            color = Color(0xFF475569)
        )
        Text("Meta de hoje: $targetHours h • faltam $remainingHours h", fontWeight = FontWeight.SemiBold)
        Text("${progress.missedDays} dias sem check-in para compensar até 17/01/2027 (${progress.daysUntilExam} dias restantes).", fontSize = 13.sp)
        Text("Últimos 7 dias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        history.forEach { day ->
            val completed = day.checkInCount > 0
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (completed) Color(0xFFE8F5E9) else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("pt-BR"))} ${day.date.dayOfMonth}/${day.date.monthValue}")
                    Text("${day.checkInCount} check-ins • ${day.minutesStudied / 60}h ${day.minutesStudied % 60}min")
                }
            }
        }
    }
}

@Composable
fun AlertEditorDialog(
    initialAlert: StudyAlert,
    onDismiss: () -> Unit,
    onSave: (StudyAlert) -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(initialAlert.hour) }
    var minute by remember { mutableIntStateOf(initialAlert.minute) }
    var subject by remember { mutableStateOf(initialAlert.subject) }
    var customMessage by remember { mutableStateOf(initialAlert.customMessage) }
    var targetUrl by remember { mutableStateOf(initialAlert.targetUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Matéria e Simulado", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                hour = selectedHour
                                minute = selectedMinute
                            },
                            hour,
                            minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "⏰ Horário: %02d:%02d (Toque p/ mudar)", hour, minute),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Turno de sorteio: ${AlertRepository.getPeriodForHour(hour).label}",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Matéria / Atividade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Frase exclusiva desta matéria (opcional)") },
                    placeholder = { Text("Deixe vazio p/ usar só o sorteio do turno") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text("Link do Simulado (QConcursos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialAlert.copy(
                            hour = hour,
                            minute = minute,
                            subject = subject.ifBlank { "Estudos Câmara" },
                            customMessage = customMessage.trim(),
                            targetUrl = targetUrl.trim().ifBlank { AlertRepository.QCONCURSOS_URL }
                        )
                    )
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
