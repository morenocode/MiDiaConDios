package com.modu.midiacondios

import java.time.LocalDate

data class Devotional(
    val reference: String,
    val verse: String,
    val reflection: String,
    val prayer: String
)

object DevotionalRepository {
    private val devotionals = listOf(
        Devotional(
            "Filipenses 4:13",
            "Todo lo puedo en Cristo que me fortalece.",
            "No tienes que enfrentar el día confiando solamente en tus propias fuerzas. La fe también es reconocer que puedes pedir ayuda, volver a intentarlo y caminar con esperanza.",
            "Señor, fortalece mi corazón hoy. Ayúdame a avanzar con fe, paciencia y humildad. Amén."
        ),
        Devotional(
            "Salmos 23:1",
            "El Señor es mi pastor; nada me faltará.",
            "Dios conoce tus necesidades incluso antes de que las expreses. Hoy puedes vivir con menos ansiedad y con mayor confianza, haciendo tu parte y dejando en sus manos aquello que no controlas.",
            "Dios mío, guía mis decisiones y enséñame a descansar en tu cuidado. Amén."
        ),
        Devotional(
            "Proverbios 3:5-6",
            "Fíate del Señor de todo tu corazón, y no estribes en tu prudencia.",
            "No siempre tendrás todas las respuestas. La sabiduría también consiste en reconocer tus límites, pedir dirección y elegir el camino correcto aunque todavía no veas todo el resultado.",
            "Señor, dame sabiduría para elegir bien y un corazón dispuesto a seguir tu dirección. Amén."
        ),
        Devotional(
            "Isaías 41:10",
            "No temas, porque yo soy contigo; no desmayes, porque yo soy tu Dios.",
            "El miedo puede aparecer, pero no tiene que dirigir tus decisiones. Recuerda que no estás solo y da hoy un paso pequeño hacia aquello que sabes que debes hacer.",
            "Padre, acompáñame en mis temores y dame valor para seguir adelante. Amén."
        ),
        Devotional(
            "Mateo 6:34",
            "No os congojéis por el día de mañana.",
            "Gran parte de nuestra preocupación nace de vivir problemas que todavía no han ocurrido. Atiende el día de hoy: una conversación, una tarea y una decisión a la vez.",
            "Señor, ayúdame a vivir este día con paz y a confiarte lo que todavía no ha llegado. Amén."
        ),
        Devotional(
            "Salmos 46:1",
            "Dios es nuestro amparo y fortaleza, nuestro pronto auxilio en las tribulaciones.",
            "En los días difíciles, buscar refugio no significa rendirse. Significa encontrar un lugar firme desde el cual recuperar fuerzas y continuar.",
            "Dios, sé mi refugio hoy. Dame serenidad en lo difícil y fuerza para hacer lo correcto. Amén."
        ),
        Devotional(
            "Romanos 12:12",
            "Gozosos en la esperanza; sufridos en la tribulación; constantes en la oración.",
            "La esperanza, la paciencia y la oración se fortalecen con la práctica. No necesitas hacerlo perfecto: necesitas permanecer y volver a empezar cada vez que sea necesario.",
            "Señor, hazme constante en la oración y paciente en los procesos que toman tiempo. Amén."
        ),
        Devotional(
            "Salmos 37:5",
            "Encomienda al Señor tu camino, y espera en él.",
            "Puedes planificar y trabajar con responsabilidad sin cargar con la necesidad de controlarlo todo. Haz lo que corresponde hoy y entrega a Dios el resultado.",
            "Padre, pongo mis planes en tus manos. Corrige mi camino y ayúdame a actuar con integridad. Amén."
        ),
        Devotional(
            "Josué 1:9",
            "Mira que te mando que te esfuerces y seas valiente; no temas ni desmayes.",
            "La valentía no es ausencia de miedo. Es avanzar con propósito a pesar de sentirlo. Identifica hoy ese paso que has estado postergando y comienza.",
            "Dios, dame valentía para enfrentar mis responsabilidades con fe y buen ánimo. Amén."
        ),
        Devotional(
            "Salmos 118:24",
            "Este es el día que hizo el Señor; nos gozaremos y alegraremos en él.",
            "Cada día contiene algo por agradecer, incluso cuando no todo está bien. Entrena tu mirada para reconocer lo bueno sin negar lo difícil.",
            "Gracias, Señor, por este día. Ayúdame a reconocer tus bendiciones y compartir alegría con otros. Amén."
        ),
        Devotional(
            "Gálatas 6:9",
            "No nos cansemos, pues, de hacer bien.",
            "Los frutos importantes casi nunca aparecen de inmediato. Continúa haciendo el bien, cuidando tus hábitos y sirviendo con sinceridad aunque todavía no veas resultados.",
            "Señor, dame perseverancia para no abandonar lo bueno por cansancio o impaciencia. Amén."
        ),
        Devotional(
            "Salmos 34:8",
            "Gustad, y ved que es bueno el Señor.",
            "La fe también se construye recordando experiencias concretas de cuidado, consuelo y provisión. Hoy recuerda una ocasión en la que viste una puerta abrirse o recibiste ayuda a tiempo.",
            "Padre, gracias por tu bondad. Abre mis ojos para reconocerla también hoy. Amén."
        ),
        Devotional(
            "Colosenses 3:23",
            "Y todo lo que hagáis, hacedlo de ánimo, como al Señor.",
            "Las tareas pequeñas también pueden hacerse con propósito. La excelencia no siempre es hacer más, sino hacer bien lo que tienes delante con una actitud correcta.",
            "Señor, ayúdame a trabajar con responsabilidad, respeto y un corazón dispuesto a servir. Amén."
        ),
        Devotional(
            "1 Tesalonicenses 5:16-18",
            "Estad siempre gozosos. Orad sin cesar. Dad gracias en todo.",
            "La gratitud no exige que todo sea perfecto. Puedes agradecer una cosa concreta hoy y convertir ese momento en una oración breve durante tu rutina.",
            "Dios, enséñame a mantener un corazón agradecido y a hablar contigo durante todo el día. Amén."
        )
    )

    fun forDate(date: LocalDate): Devotional {
        val index = Math.floorMod(date.toEpochDay(), devotionals.size.toLong()).toInt()
        return devotionals[index]
    }

    fun today(): Devotional = forDate(LocalDate.now())

    fun all(): List<Devotional> = devotionals
}
