package com.concurso.camara

data class StudyAlert(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val title: String,
    val message: String
)

object AlertCatalog {
    val alerts = listOf(
        StudyAlert(
            id = 1,
            hour = 7,
            minute = 30,
            title = "🦉 Acorda, futuro servidor de R$ 32 mil!",
            message = "Um dia sem estudar custa R$ 32.070,88 na conta. A concorrência já fez 20 questões hoje!"
        ),
        StudyAlert(
            id = 2,
            hour = 10,
            minute = 30,
            title = "⚠️ Cuidado: uma errada anula uma certa!",
            message = "No método Cebraspe, chutar sem estudar é suicídio. Revise Direito Administrativo agora!"
        ),
        StudyAlert(
            id = 3,
            hour = 13,
            minute = 0,
            title = "🍽️ Almoço com gosto de procrastinação?",
            message = "Dava para ter revisado a Lei nº 8.112 ou Regimento. O edital não vai se ler sozinho!"
        ),
        StudyAlert(
            id = 4,
            hour = 16,
            minute = 0,
            title = "👀 Não finja que esqueceu de mim...",
            message = "A prova tem 180 itens e redação técnica de 50 linhas te esperando. Estude IA e Linguística!"
        ),
        StudyAlert(
            id = 5,
            hour = 18,
            minute = 30,
            title = "🔥 Sua ofensiva de estudos está em perigo mortal!",
            message = "Cebraspe adora concurseiro que deixa para amanhã. Sente-se nessa cadeira agora!"
        ),
        StudyAlert(
            id = 6,
            hour = 20,
            minute = 30,
            title = "🥺 Meus alertas não estão funcionando?",
            message = "A posse na Câmara não vai cair do céu. Faça ao menos um simulado ou questão discursiva!"
        ),
        StudyAlert(
            id = 7,
            hour = 22,
            minute = 30,
            title = "🚨 Última chamada antes de virar estatística!",
            message = "Vai dormir com o peso da negligência ou vai revisar 5 páginas de Técnica Legislativa?"
        )
    )
}