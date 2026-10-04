package app.upwake.ring

import kotlin.math.max
import kotlin.math.min

/** Backup mission: copy this many paragraphs. */
const val WRITING_COUNT = 4

/** Original paragraphs. Long enough that typing four of them fully wakes you up. */
val PARAGRAPHS = listOf(
    "The first hour of the day decides the rest of it. If I spend it scrolling, I hand my attention to strangers before I have even asked myself what I want. Today I am choosing to start with my own plans instead.",
    "Discipline is not about being hard on myself. It is about keeping a promise I made to myself last night, when I was thinking clearly. The person who set this alarm wanted something better for me, and I am going to listen to him.",
    "Every great thing I want to build will be made of ordinary mornings like this one. Nobody sees these mornings and nobody claps for them, but they add up quietly until one day the results are impossible to ignore.",
    "Comfort feels good for five minutes and costs me the whole day. Getting up now is uncomfortable, but it gives me time, energy and the quiet feeling of being in control. I would rather have that feeling than ten more minutes of sleep.",
    "I am not the tired thoughts I have right after waking up. Those thoughts always say stay in bed, skip it, do it tomorrow. They are loud but they are not wise. In ten minutes I will be glad I ignored them.",
    "Small wins in the morning create momentum. Making my bed, drinking a glass of water and stepping into some light are simple things, but they tell my brain that today is a day where I do what I said I would do.",
    "My phone will still be there later. The messages, the videos and the news can wait. What cannot wait is the time I have this morning to think, move my body and decide what kind of day I am about to have.",
    "Some people would give anything for another ordinary morning. I woke up today with a body that works, a mind that can learn and a day full of choices. The least I can do is not waste the beginning of it.",
    "Motivation is unreliable, so I do not wait for it. I act first and the feeling usually follows. Right now I do not feel like getting up, which is exactly why getting up matters. This is where character is built.",
    "If I keep doing what I did yesterday, I will keep getting what I got yesterday. Change does not happen in one dramatic moment. It happens in small decisions like this one, made again and again when nobody is watching.",
    "Sleep was the reward for yesterday. Today is a new chance to earn tonight's rest. I want to go to bed this evening tired from effort, not from staring at a screen, and that story starts with the next few minutes.",
    "The best version of me is not a different person. It is me, making slightly better choices, slightly more often. Getting out of bed on time is one of those choices, and I am making it right now as I type this.",
)

private fun words(s: String): List<String> =
    s.lowercase().replace(Regex("[^a-z0-9' ]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }

private fun editDistance(a: List<String>, b: List<String>): Int {
    var prev = IntArray(b.size + 1) { it }
    for (i in 1..a.size) {
        val cur = IntArray(b.size + 1)
        cur[0] = i
        for (j in 1..b.size) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
        }
        prev = cur
    }
    return prev[b.size]
}

/** Matches if almost every word is right (about 1 slip per 20 words allowed). */
fun typedMatches(typed: String, target: String): Boolean {
    val a = words(typed)
    val b = words(target)
    if (b.isEmpty() || a.size < b.size - 1) return false
    return editDistance(a, b) <= max(1, b.size / 20)
}

fun typedProgress(typed: String, target: String): Float {
    val total = words(target).size
    if (total == 0) return 0f
    return (words(typed).size.toFloat() / total).coerceIn(0f, 1f)
}
