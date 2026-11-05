package com.paninda.app.ui.home

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.paninda.app.PanindaApplication
import com.paninda.app.R
import com.paninda.app.domain.model.ShopZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime

/** All greeting pools and selection rules live here; editable copy lives in strings_microcopies.xml. */
object Microcopies {
    enum class Period { MORNING, LUNCH, AFTERNOON, EVENING, LATE_NIGHT }
    fun period(hour: Int): Period {
        require(hour in 0..23)
        return when (hour) {
            in 5..10 -> Period.MORNING
            in 11..12 -> Period.LUNCH
            in 13..16 -> Period.AFTERNOON
            in 17..21 -> Period.EVENING
            else -> Period.LATE_NIGHT
        }
    }
    val pools = mapOf(
        Period.MORNING to listOf(R.string.micro_morning_1, R.string.micro_morning_2, R.string.micro_morning_3, R.string.micro_morning_4, R.string.micro_morning_5),
        Period.LUNCH to listOf(R.string.micro_lunch_1, R.string.micro_lunch_2, R.string.micro_lunch_3),
        Period.AFTERNOON to listOf(R.string.micro_afternoon_1, R.string.micro_afternoon_2, R.string.micro_afternoon_3),
        Period.EVENING to listOf(R.string.micro_evening_1, R.string.micro_evening_2, R.string.micro_evening_3, R.string.micro_evening_4, R.string.micro_evening_5),
        Period.LATE_NIGHT to listOf(R.string.micro_late_1, R.string.micro_late_2, R.string.micro_late_3),
    )
}

/** One instance per application launch; navigation/recomposition/recreation never advances it. */
class MicrocopyRotation(private val previous: (Microcopies.Period) -> Int, private val save: (Microcopies.Period, Int) -> Unit) {
    private val selected = mutableMapOf<Microcopies.Period, Int>()
    @Synchronized fun greeting(hour: Int): Int {
        val period = Microcopies.period(hour)
        return selected.getOrPut(period) {
            val pool = Microcopies.pools.getValue(period)
            val next = Math.floorMod(previous(period).toLong() + 1, pool.size.toLong()).toInt()
            save(period, next)
            pool[next]
        }
    }
}

@Composable
fun rememberHomeGreeting(): String {
    val app = LocalContext.current.applicationContext as PanindaApplication
    val owner = LocalLifecycleOwner.current
    val hour by produceState(LocalTime.now(ShopZone).hour, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = LocalTime.now(ShopZone).hour
                delay(60_000)
            }
        }
    }
    return stringResource(app.microcopies.greeting(hour))
}
