package com.udc.collection.util

import com.udc.collection.domain.model.DiscountType

object ValidationUtils {

    fun validatePatientName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Patient name is required"
        if (trimmed.length < 2) return "Name must be at least 2 characters"
        return null
    }

    fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return null
        val digits = phone.filter { it.isDigit() }
        if (digits.length != 10) return "Must be exactly 10 digits"
        return null
    }

    fun validateAge(age: String): String? {
        if (age.isBlank()) return null
        val n = age.toIntOrNull() ?: return "Must be a whole number"
        if (n < 0 || n > 120) return "Must be between 0 and 120"
        return null
    }

    fun validateDiscount(
        value: String,
        discountType: DiscountType,
        subtotal: Double
    ): String? {
        if (value.isBlank()) return null
        val d = value.toDoubleOrNull() ?: return "Invalid number"
        if (d < 0) return "Cannot be negative"
        return when (discountType) {
            DiscountType.PERCENTAGE -> if (d > 100) "Cannot exceed 100%" else null
            DiscountType.FLAT -> if (d > subtotal) "Cannot exceed subtotal (${subtotal.formatCurrency()})" else null
            DiscountType.NONE -> null
        }
    }

    fun validateAmountReceived(amount: String, grandTotal: Double): String? {
        if (amount.isBlank()) return null
        val v = amount.toDoubleOrNull() ?: return "Invalid number"
        if (v < 0) return "Cannot be negative"
        if (v > grandTotal) return "Cannot exceed grand total (${grandTotal.formatCurrency()})"
        return null
    }

    fun validateTestName(name: String, existingNames: List<String>, currentId: Long = 0L): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Test name is required"
        val duplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) }
        if (duplicate) return "A test with this name already exists"
        return null
    }

    fun validateTestPrice(price: String): String? {
        if (price.isBlank()) return "Price is required"
        val p = price.toDoubleOrNull() ?: return "Invalid price"
        if (p < 0) return "Price cannot be negative"
        return null
    }

    fun validatePackageName(name: String, existingNames: List<String>): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Package name is required"
        if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
            return "A package with this name already exists"
        }
        return null
    }

    fun validatePin(pin: String): String? {
        if (pin.length < 4) return "PIN must be at least 4 digits"
        if (pin.length > 8) return "PIN cannot exceed 8 digits"
        if (!pin.all { it.isDigit() }) return "PIN must contain only digits"
        return null
    }
}
