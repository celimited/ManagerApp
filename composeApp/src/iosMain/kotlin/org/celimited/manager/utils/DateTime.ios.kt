package org.celimited.manager.utils

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate

actual fun currentMonthNumber(): Int =
    NSCalendar.currentCalendar.component(
        platform.Foundation.NSCalendarUnitMonth, NSDate()
    ).toInt()

actual fun currentYearNumber(): Int =
    NSCalendar.currentCalendar.component(
        platform.Foundation.NSCalendarUnitYear, NSDate()
    ).toInt()