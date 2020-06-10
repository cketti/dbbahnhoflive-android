package de.deutschebahn.bahnhoflive.model.parking

import de.deutschebahn.bahnhoflive.util.nonBlankOrNull
import java.util.*

class Duration(
    val raw: String,
    exceptionHandler: (Exception) -> Unit = {}
) {
    enum class TimeUnit {
        MIN,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR;

        val key = name.toLowerCase(Locale.ROOT)
    }

    companion object {
        const val TAG_DISCOUNT = "Discount"
        const val TAG_VENDING_MACHINE = "VendingMachine"
        const val TAG_LONG_TERM = "LongTerm"
        const val TAG_RESERVATION = "Reservation"

        val ENTIRE_REGEX =
            Regex("(\\d+)(${TimeUnit.values().joinToString("|", transform = { it.key })})s?(.*)")
        val TAGS_REGEX = Regex("$TAG_DISCOUNT|$TAG_VENDING_MACHINE|$TAG_LONG_TERM|$TAG_RESERVATION")
    }

    val amount: Int?
    val timeUnit: TimeUnit?

    val discount: Boolean
    var vendingMachine: Boolean
    var longTerm: Boolean
    var reservation: Boolean

    val unrecognizedTags: String?

    init {
        var amount: Int? = null
        var timeUnit: TimeUnit? = null
        var discount = false
        var vendingMachine = false
        var longTerm = false
        var reservation = false
        var unrecognizedTags: String? = null

        try {
            ENTIRE_REGEX.matchEntire(raw)?.run {

                amount = groupValues[1].toInt()
                timeUnit = groupValues[2].let { durationUnit ->
                    TimeUnit.values().find {
                        it.key == durationUnit
                    }
                }

                unrecognizedTags = TAGS_REGEX.replace(groupValues[3]) { matchResult ->
                    when (matchResult.value) {
                        TAG_DISCOUNT -> discount = true
                        TAG_VENDING_MACHINE -> vendingMachine = true
                        TAG_LONG_TERM -> longTerm = true
                        TAG_RESERVATION -> reservation = true
                    }
                    ""
                }.nonBlankOrNull()

            }
        } catch (e: Exception) {
            exceptionHandler(e)
        }

        this.amount = amount
        this.timeUnit = timeUnit

        this.discount = discount
        this.vendingMachine = vendingMachine
        this.longTerm = longTerm
        this.reservation = reservation

        this.unrecognizedTags = unrecognizedTags
    }

    val valid by lazy { amount != null && timeUnit != null }

    override fun toString() = "Duration(" + if (valid) {
        "$amount$timeUnit ($raw)"
    } else {
        raw
    } + ")"
}