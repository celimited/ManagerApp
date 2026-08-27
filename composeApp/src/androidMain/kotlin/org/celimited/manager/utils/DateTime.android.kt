package org.celimited.manager.utils

import java.util.Calendar

actual fun currentMonthNumber(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
actual fun currentYearNumber(): Int = Calendar.getInstance().get(Calendar.YEAR)