package com.concurso.camara

import android.content.Context

object MascotIntelligence {
    fun generateMessage(
        context: Context,
        mood: MascotMood,
        subject: String,
        hoursWithoutStudy: Long,
        fallback: String = ""
    ): String {
        val profile = UserProfileRepository.getProfile(context)
        val progress = StudyCheckInRepository.getProgress(context, profile)
        val message = fallback.ifBlank { defaultFallbackMessage(context, mood, hoursWithoutStudy, subject) }
        return UserProfileRepository.personalizeMessage(
            context,
            appendProgressMessage(message, profile, progress, hoursWithoutStudy, subject)
        )
    }

    fun previewMessage(context: Context, mood: MascotMood, hoursWithoutStudy: Long): String {
        return UserProfileRepository.personalizeMessage(
            context,
            defaultFallbackMessage(context, mood, hoursWithoutStudy, "estudo")
        )
    }

    private fun defaultFallbackMessage(
        context: Context,
        mood: MascotMood,
        hoursWithoutStudy: Long,
        subject: String
    ): String {
        val profile = UserProfileRepository.getProfile(context)
        val name = profile.name.ifBlank { "futuro servidor" }
        val focus = subject.ifBlank { "estudar" }
        val longBreak = hoursWithoutStudy >= 8L
        val baseMessage = when (mood) {
            MascotMood.HAPPY -> "$name, que bom ver você por aqui! Que tal resolver três questões de $focus e guardar uma ideia que aprendeu?"
            MascotMood.COOL -> "Vamos no estratégico: tente explicar um ponto de $focus sem consultar o material e depois confira o que faltou."
            MascotMood.SUSPICIOUS -> "Tenho um desafio leve pra você: será que consegue acertar duas questões de $focus antes de olhar a resposta?"
            MascotMood.SAD -> if (longBreak) {
                "Tudo bem retomar aos poucos, $name; escolha uma questão de $focus e vamos recomeçar sem cobrança."
            } else {
                "Se o dia estiver pesado, $name, uma questão de $focus já é um começo válido; vamos juntos?"
            }
            MascotMood.PLEADING -> "Me acompanha em cinco minutinhos? Tente lembrar três pontos de $focus e depois confira suas anotações."
            MascotMood.ANGRY -> "Vamos transformar a intenção em ação: abra $focus e resolva uma questão com atenção agora."
            MascotMood.DESPAIR -> "Faz tempo que você não estuda, $name, mas não precisa compensar tudo hoje; retome com uma questão simples de $focus."
        }

        return baseMessage
    }

    private fun appendProgressMessage(
        message: String,
        profile: UserProfile,
        progress: StudyProgress,
        hoursWithoutStudy: Long,
        subject: String
    ): String {
        val name = profile.name.ifBlank { "Mario" }
        val focus = subject.ifBlank { profile.desiredPosition }.ifBlank { "concurso" }
        val firstSentence = message
            .split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull()
            .orEmpty()
        val conciseMessage = firstSentence.take(135).let { text ->
            if (firstSentence.length > 135) text.substringBeforeLast(' ', text).trimEnd('.', ',', ';', ':') + "…" else text
        }

        if (hoursWithoutStudy >= 24) {
            val days = (hoursWithoutStudy / 24).coerceAtLeast(1)
            return if (profile.coachStyle == CoachStyle.DEMANDING) {
                "$name, você não faz check-in há $days dias. A prova está a ${progress.daysUntilExam} dias: resolva três questões de $focus hoje e comece a compensar."
            } else {
                "$name, você está há $days dias sem check-in. Por favor, se esforce um pouquinho e compense com três questões de $focus."
            }
        }

        val currentHour = java.time.LocalTime.now().hour
        if (currentHour >= 22 && progress.checkInsToday < profile.dailyCheckInGoal) {
            val checkIns = progress.checkInsToday
            return if (profile.coachStyle == CoachStyle.DEMANDING) {
                if (checkIns == 0) {
                    "$name, ainda nenhum check-in e já são ${currentHour}h: mudou de planos ou vai resolver três questões de $focus antes de dormir?"
                } else {
                    "$name, você só fez $checkIns check-in${if (checkIns == 1) "" else "s"} hoje e já são ${currentHour}h; adoeceu ou desistiu do concurso? Feche o dia com três questões de $focus."
                }
            } else if (checkIns == 0) {
                "$name, já são ${currentHour}h e ainda não houve check-in; se hoje ficou corrido, deixe 20 minutos de $focus planejados para amanhã."
            } else {
                "$name, você fez $checkIns check-in${if (checkIns == 1) "" else "s"} hoje e já está tarde; se tiver energia, feche com uma revisão leve de $focus."
            }
        }

        if (progress.hoursRemainingToday <= 0.0 && progress.checkInsToday >= profile.dailyCheckInGoal) {
            return conciseMessage
        }

        return conciseMessage
    }

}
