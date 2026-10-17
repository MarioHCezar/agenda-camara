package com.concurso.camara

import android.content.Context
import android.app.NotificationManager
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

enum class TimePeriod(val label: String, val shortTitle: String) {
    MORNING("Manhã (05h às 11h59)", "Manhã"),
    AFTERNOON("Tarde (12h às 17h59)", "Tarde"),
    EVENING("Noite (18h às 21h59)", "Noite"),
    LATE_NIGHT("Madrugada / Corujão (22h às 04h59)", "Corujão")
}

enum class UserGender(val label: String) {
    FEMININE("Feminino"),
    MASCULINE("Masculino"),
    UNSPECIFIED("Prefiro não informar")
}

enum class CoachStyle(val label: String) {
    MOTIVATIONAL("Apenas motivacional"),
    DEMANDING("Extremamente exigente")
}

data class UserProfile(
    val name: String = "",
    val gender: UserGender = UserGender.UNSPECIFIED,
    val coachStyle: CoachStyle = CoachStyle.MOTIVATIONAL,
    val dailyCheckInGoal: Int = 3,
    val dailyStudyGoalHours: Double = 4.0,
    val desiredPosition: String = ""
)

object UserProfileRepository {
    private const val PREFS_NAME = "agenda_camara_prefs"
    private const val KEY_NAME = "user_name"
    private const val KEY_GENDER = "user_gender"
    private const val KEY_COACH_STYLE = "user_coach_style"
    private const val KEY_DAILY_CHECK_IN_GOAL = "daily_check_in_goal"
    private const val KEY_DAILY_STUDY_GOAL_HOURS = "daily_study_goal_hours"
    private const val KEY_DESIRED_POSITION = "desired_position"

    fun getProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gender = runCatching {
            UserGender.valueOf(prefs.getString(KEY_GENDER, null) ?: "UNSPECIFIED")
        }.getOrDefault(UserGender.UNSPECIFIED)
        val coachStyle = runCatching {
            CoachStyle.valueOf(prefs.getString(KEY_COACH_STYLE, null) ?: CoachStyle.MOTIVATIONAL.name)
        }.getOrDefault(CoachStyle.MOTIVATIONAL)
        return UserProfile(
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            gender = gender,
            coachStyle = coachStyle,
            dailyCheckInGoal = prefs.getInt(KEY_DAILY_CHECK_IN_GOAL, 3).coerceIn(1, 10),
            dailyStudyGoalHours = prefs.getString(KEY_DAILY_STUDY_GOAL_HOURS, "4.0")
                ?.toDoubleOrNull()?.coerceIn(1.0, 12.0) ?: 4.0,
            desiredPosition = prefs.getString(KEY_DESIRED_POSITION, "").orEmpty()
        )
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val cleanedProfile = profile.copy(name = profile.name.trim())
        val previousProfile = getProfile(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, cleanedProfile.name)
            .putString(KEY_GENDER, cleanedProfile.gender.name)
            .putString(KEY_COACH_STYLE, cleanedProfile.coachStyle.name)
            .putInt(KEY_DAILY_CHECK_IN_GOAL, cleanedProfile.dailyCheckInGoal.coerceIn(1, 10))
            .putString(KEY_DAILY_STUDY_GOAL_HOURS, cleanedProfile.dailyStudyGoalHours.coerceIn(1.0, 12.0).toString())
            .putString(KEY_DESIRED_POSITION, cleanedProfile.desiredPosition.trim())
            .apply()

        if (previousProfile != cleanedProfile) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
    }

    fun personalizeMessage(context: Context, message: String): String {
        val profile = getProfile(context)
        var personalized = message

        if (profile.gender == UserGender.FEMININE) {
            val feminineForms = listOf(
                "futuro servidor" to "futura servidora",
                "futuro Analista" to "futura Analista",
                "aprovados" to "aprovadas",
                "aprovado" to "aprovada",
                "guerreiro" to "guerreira",
                "concurseiro" to "concurseira",
                "doutor" to "doutora"
            )
            feminineForms.forEach { (masculine, feminine) ->
                personalized = Regex("\\b${Regex.escape(masculine)}\\b", RegexOption.IGNORE_CASE)
                    .replace(personalized) { match ->
                        if (match.value.first().isUpperCase()) {
                            feminine.replaceFirstChar { it.uppercase() }
                        } else {
                            feminine
                        }
                    }
            }
        }

        val name = profile.name.trim()
        if (name.isNotEmpty()) {
            val nameReference = Regex(
                "(?<![\\p{L}\\p{N}])(?:\\{nome\\}|Mario(?:\\s+Bonito)?)(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            )
            val exactName = Regex(
                "(?<![\\p{L}\\p{N}])${Regex.escape(name)}(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            )
            if (nameReference.containsMatchIn(personalized)) {
                personalized = nameReference.replace(personalized, name)
            } else if (!exactName.containsMatchIn(personalized)) {
                personalized = "$name, ${personalized.replaceFirstChar { it.lowercase() }}"
            }
        }

        return personalized
    }
}

data class StudyCheckIn(
    val timestampMillis: Long,
    val minutesStudied: Int
) {
    fun localDate(): LocalDate = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

data class DailyCheckInSummary(
    val date: LocalDate,
    val checkInCount: Int,
    val minutesStudied: Int
)

data class StudyProgress(
    val checkInsToday: Int,
    val streakDays: Int,
    val minutesStudiedToday: Int,
    val missedDays: Int,
    val daysUntilExam: Long,
    val hoursRequiredToday: Double,
    val hoursRemainingToday: Double
)

object StudyCheckInRepository {
    val examDate: LocalDate = LocalDate.of(2027, 1, 17)
    private const val PREFS_NAME = "agenda_camara_prefs"
    private const val KEY_CHECK_INS = "study_check_ins_v1"
    private const val MAX_RECORDS = 2000

    fun recordCheckIn(context: Context, minutesStudied: Int) {
        val records = readRecords(context).toMutableList()
        records.add(StudyCheckIn(System.currentTimeMillis(), minutesStudied.coerceIn(1, 720)))
        val array = JSONArray()
        records.takeLast(MAX_RECORDS).forEach { record ->
            array.put(JSONObject().apply {
                put("timestamp", record.timestampMillis)
                put("minutes", record.minutesStudied)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHECK_INS, array.toString())
            .apply()
    }

    fun getProgress(context: Context, profile: UserProfile = UserProfileRepository.getProfile(context)): StudyProgress {
        val today = LocalDate.now()
        val recordsByDate = readRecords(context).groupBy { it.localDate() }
        val todayRecords = recordsByDate[today].orEmpty()
        val checkedDates = recordsByDate.keys
        var streak = 0
        var streakDate = if (today in checkedDates) today else today.minusDays(1)
        while (streakDate in checkedDates) {
            streak++
            streakDate = streakDate.minusDays(1)
        }

        var missedDays = 0
        var catchUpDebtHours = 0.0
        var day = checkedDates.minOrNull() ?: today
        val dailyGoalHours = profile.dailyStudyGoalHours.coerceIn(1.0, 12.0)
        while (day.isBefore(today)) {
            val dayRecords = recordsByDate[day].orEmpty()
            if (dayRecords.isEmpty()) {
                missedDays++
                catchUpDebtHours += dailyGoalHours
            } else {
                val studiedHours = dayRecords.sumOf { it.minutesStudied } / 60.0
                catchUpDebtHours = (catchUpDebtHours - (studiedHours - dailyGoalHours).coerceAtLeast(0.0))
                    .coerceAtLeast(0.0)
            }
            day = day.plusDays(1)
        }

        val daysUntilExam = ChronoUnit.DAYS.between(today, examDate).coerceAtLeast(0)
        val daysAvailable = daysUntilExam + 1
        val requiredHours = if (today.isAfter(examDate)) {
            0.0
        } else {
            dailyGoalHours + catchUpDebtHours / daysAvailable
        }
        val studiedTodayHours = todayRecords.sumOf { it.minutesStudied } / 60.0

        return StudyProgress(
            checkInsToday = todayRecords.size,
            streakDays = streak,
            minutesStudiedToday = todayRecords.sumOf { it.minutesStudied },
            missedDays = missedDays,
            daysUntilExam = daysUntilExam,
            hoursRequiredToday = requiredHours,
            hoursRemainingToday = (requiredHours - studiedTodayHours).coerceAtLeast(0.0)
        )
    }

    fun getHoursSinceLastCheckIn(context: Context): Long {
        val lastCheckIn = readRecords(context).maxOfOrNull { it.timestampMillis } ?: return 0L
        return ((System.currentTimeMillis() - lastCheckIn) / (1000L * 60L * 60L)).coerceAtLeast(0L)
    }

    fun getRecentHistory(context: Context, days: Int = 7): List<DailyCheckInSummary> {
        val recordsByDate = readRecords(context).groupBy { it.localDate() }
        return (0 until days.coerceIn(1, 30)).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            val records = recordsByDate[date].orEmpty()
            DailyCheckInSummary(date, records.size, records.sumOf { it.minutesStudied })
        }
    }

    private fun readRecords(context: Context): List<StudyCheckIn> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CHECK_INS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val record = array.getJSONObject(index)
                StudyCheckIn(record.getLong("timestamp"), record.getInt("minutes"))
            }.filter { it.minutesStudied in 1..720 }
        }.getOrDefault(emptyList())
    }
}

data class StudyAlert(
    val id: Int,
    val dayOfWeek: Int,
    val hour: Int,
    val minute: Int,
    val subject: String,
    val customMessage: String = "",
    val targetUrl: String = AlertRepository.QCONCURSOS_URL,
    val enabled: Boolean = true
)

object AlertRepository {
    private const val PREFS_NAME = "agenda_camara_prefs"
    private const val KEY_ALERTS = "saved_alerts_v3"
    private const val KEY_MESSAGES_PREFIX = "period_messages_v3_"

    const val MAX_MESSAGES_PER_PERIOD = 200
    const val QCONCURSOS_URL = "https://www.qconcursos.com/?from_omniauth=true&provider=google_oauth2"

    private val defaultMorningMessages = listOf(
        "Bom dia, futuro servidor! O café já tá passado? Bora pro QConcursos!",
        "Acorda, {nome}! A Câmara dos Deputados espera por você!",
        "Alvorada legislativa! Cada questão resolvida hoje deixa Brasília mais perto.",
        "Levanta, sacode a poeira e abre o simulado! Manhã de aprovado começa no foco.",
        "Bom dia! O cachorrinho da posse passou para lembrar que essa vaga já é sua!",
        "Hora de brilhar, futuro Analista! Toque aqui para abrir sua bateria matinal no QConcursos.",
        "Manhã fresca, mente afiada! Vamos amassar 20 questões antes do almoço?",
        "Acorda, doutor! O Regimento Interno não vai se gabaritar sozinho!"
    )

    private val defaultAfternoonMessages = listOf(
        "Bateu o sono pós-almoço? Água gelada no rosto e clique aqui pro simulado!",
        "Boa tarde, {nome}! Tarde produtiva é sinônimo de nome no Diário Oficial.",
        "Segura o ritmo! Metade do dia já foi, garanta o seu bloco no QConcursos.",
        "Respira fundo, ajeita a postura na cadeira e vamos gabaritar essa bateria!",
        "A concorrência tá cochilando, mas o futuro servidor tá resolvendo questões. Pra cima!",
        "Foco total! Mais uma horinha de simulado para carimbar sua posse na Câmara.",
        "Tarde de campeão: clique na notificação e mande ver nas questões da banca!"
    )

    private val defaultEveningMessages = listOf(
        "Boa noite, guerreiro! O cansaço passa, mas o contracheque da Câmara é para sempre!",
        "Último gás do dia, {nome}! Fecha essa meta no QConcursos com chave de ouro.",
        "Noite de estudos: é aqui que os aprovados no Legislativo fazem a diferença!",
        "Troque 30 minutos de redes sociais por mais 15 questões resolvidas agora!",
        "Você já venceu o expediente, agora é a hora de construir a sua aprovação!",
        "O cachorrinho da posse está te olhando... Não vá dormir sem fazer seu simulado!",
        "Mais um bloco concluído hoje é menos peso na revisão de sábado. Bora pro QConcursos!"
    )

    private val defaultLateNightMessages = listOf(
        "Corujão Legislativo ativado! Hora daquelas questões estratégicas no silêncio da noite.",
        "Revisão rápida antes do descanso: o cérebro consolida enquanto você dorme!",
        "Falta pouco para fechar o dia! Faça 10 questões rápidas no QConcursos e durma em paz.",
        "Missão cumprida na madrugada? Orgulho define, futuro servidor!"
    )

    private val defaultAlerts = listOf(
        StudyAlert(1, 0, 6, 30, "Constitucional & Regimento Interno", "", QCONCURSOS_URL, true),
        StudyAlert(2, 0, 12, 30, "Simulado Rápido Pós-Almoço", "", QCONCURSOS_URL, true),
        StudyAlert(3, 0, 19, 30, "Direito Administrativo & Processo Legislativo", "", QCONCURSOS_URL, true),
        StudyAlert(4, 0, 21, 30, "Bateria Noturna de Questões", "", QCONCURSOS_URL, true)
    )

    fun getPeriodForHour(hour: Int): TimePeriod {
        return when (hour) {
            in 5..11 -> TimePeriod.MORNING
            in 12..17 -> TimePeriod.AFTERNOON
            in 18..21 -> TimePeriod.EVENING
            else -> TimePeriod.LATE_NIGHT
        }
    }

    fun getCurrentMascotMood(context: Context, idleUiPenaltySteps: Int = 0): MascotMood {
        val hours = StudyCheckInRepository.getHoursSinceLastCheckIn(context)
        val baseIndex = when {
            hours < 2 -> 0
            hours < 4 -> 1
            hours < 8 -> 2
            hours < 14 -> 3
            hours < 20 -> 4
            hours < 28 -> 5
            else -> 6
        }
        val finalIndex = (baseIndex + idleUiPenaltySteps).coerceIn(0, MascotMood.entries.lastIndex)
        return MascotMood.entries[finalIndex]
    }

    fun getAlerts(context: Context): List<StudyAlert> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ALERTS, null) ?: return defaultAlerts
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<StudyAlert>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StudyAlert(
                        id = obj.getInt("id"),
                        dayOfWeek = obj.optInt("dayOfWeek", 0),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        subject = obj.getString("subject"),
                        customMessage = obj.optString("customMessage", ""),
                        targetUrl = obj.optString("targetUrl", QCONCURSOS_URL).ifBlank { QCONCURSOS_URL },
                        enabled = obj.optBoolean("enabled", true)
                    )
                )
            }
            list.sortedWith(compareBy({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            defaultAlerts
        }
    }

    fun saveAlerts(context: Context, alerts: List<StudyAlert>) {
        val array = JSONArray()
        alerts.forEach { alert ->
            val obj = JSONObject().apply {
                put("id", alert.id)
                put("dayOfWeek", alert.dayOfWeek)
                put("hour", alert.hour)
                put("minute", alert.minute)
                put("subject", alert.subject)
                put("customMessage", alert.customMessage)
                put("targetUrl", alert.targetUrl.ifBlank { QCONCURSOS_URL })
                put("enabled", alert.enabled)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALERTS, array.toString())
            .apply()
    }

    fun getMessagesForPeriod(context: Context, period: TimePeriod): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_MESSAGES_PREFIX + period.name
        val json = prefs.getString(key, null)
        if (json == null) {
            val defaults = getDefaultMessages(period)
            saveMessagesForPeriod(context, period, defaults)
            return defaults
        }
        return try {
            val array = JSONArray(json)
            val list = List(array.length()) { array.getString(it) }
            list.ifEmpty { getDefaultMessages(period) }
        } catch (e: Exception) {
            getDefaultMessages(period)
        }
    }

    fun saveMessagesForPeriod(context: Context, period: TimePeriod, messages: List<String>) {
        val limited = messages.take(MAX_MESSAGES_PER_PERIOD)
        val array = JSONArray(limited)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MESSAGES_PREFIX + period.name, array.toString())
            .apply()
    }

    fun updateMessageAtIndex(context: Context, period: TimePeriod, index: Int, updatedText: String): List<String> {
        val current = getMessagesForPeriod(context, period).toMutableList()
        if (index in current.indices && updatedText.isNotBlank()) {
            current[index] = updatedText.trim()
            saveMessagesForPeriod(context, period, current)
        }
        return current
    }

    fun resetPeriodMessagesToDefault(context: Context, period: TimePeriod): List<String> {
        val defaults = getDefaultMessages(period)
        saveMessagesForPeriod(context, period, defaults)
        return defaults
    }

    private fun getDefaultMessages(period: TimePeriod): List<String> {
        return when (period) {
            TimePeriod.MORNING -> defaultMorningMessages
            TimePeriod.AFTERNOON -> defaultAfternoonMessages
            TimePeriod.EVENING -> defaultEveningMessages
            TimePeriod.LATE_NIGHT -> defaultLateNightMessages
        }
    }

    fun getRandomNotificationMessage(context: Context, hour: Int, customMessage: String): String {
        val period = getPeriodForHour(hour)
        val pool = getMessagesForPeriod(context, period).toMutableList()
        if (customMessage.isNotBlank()) {
            pool.add(customMessage)
            pool.add(customMessage)
        }

        val fallback = UserProfileRepository.personalizeMessage(
            context,
            pool.getOrElse(Random.nextInt(pool.size)) { "Bora estudar, futuro servidor!" }
        )

        return MascotIntelligence.generateMessage(
            context = context,
            mood = getCurrentMascotMood(context),
            subject = customMessage.ifBlank { period.label },
            hoursWithoutStudy = StudyCheckInRepository.getHoursSinceLastCheckIn(context),
            fallback = fallback
        )
    }
}
