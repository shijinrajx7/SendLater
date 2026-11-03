package com.sendlater.send

object PhoneUtil {
    /**
     * Turns whatever you typed or picked into digits with country code, as wa.me needs.
     *  "+91 98765 43210" -> 919876543210
     *  "098765 43210"    -> 919876543210   (uses the default country code from Settings)
     *  "9876543210"      -> 919876543210
     */
    fun normalize(raw: String, countryCode: String): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (trimmed.startsWith("+")) return digits
        if (digits.startsWith("00")) return digits.drop(2)
        val local = digits.trimStart('0')
        return if (local.length <= 10 && countryCode.isNotBlank()) countryCode + local else local
    }
}
