package com.udc.collection.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 PIN hasher.
 *
 * Why: SHA-256 is a fast hash — an attacker with the DataStore file can brute-force a 4-digit
 * PIN in milliseconds. PBKDF2 with 120 000 iterations makes each guess ~1 second on modern
 * hardware, making offline brute force impractical.
 *
 * Storage format: Base64( salt[16] ++ hash[32] )  — 64 bytes total → ~88 base64 chars.
 * Legacy SHA-256 format:  64 hex characters — detected by the regex below.
 */
object PbkdfHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** Returns a Base64-encoded salt+hash string suitable for storage. */
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hashBytes = derive(pin, salt)
        return Base64.encodeToString(salt + hashBytes, Base64.NO_WRAP)
    }

    /**
     * Verifies a PIN against a stored hash.
     *
     * Transparently handles legacy SHA-256 hashes (64 hex chars) so existing users
     * are not locked out after the upgrade. Returns the new PBKDF2 hash if migration
     * occurred, or null if the format is already PBKDF2.
     */
    fun verify(pin: String, stored: String): VerifyResult {
        return when {
            isLegacySha256(stored) -> {
                val expected = pin.sha256Legacy()
                if (expected == stored) {
                    // Correct legacy PIN → return upgraded hash so caller can persist it
                    VerifyResult.Match(upgradedHash = hash(pin))
                } else {
                    VerifyResult.Mismatch
                }
            }
            else -> {
                val combined = runCatching { Base64.decode(stored, Base64.NO_WRAP) }
                    .getOrNull() ?: return VerifyResult.Mismatch
                if (combined.size < SALT_BYTES + 1) return VerifyResult.Mismatch
                val salt = combined.copyOfRange(0, SALT_BYTES)
                val expectedHash = combined.copyOfRange(SALT_BYTES, combined.size)
                val actualHash = derive(pin, salt)
                if (actualHash.contentEquals(expectedHash)) VerifyResult.Match(upgradedHash = null)
                else VerifyResult.Mismatch
            }
        }
    }

    sealed class VerifyResult {
        /** PIN matched. [upgradedHash] is non-null if the stored hash was legacy and needs to be re-saved. */
        data class Match(val upgradedHash: String?) : VerifyResult()
        object Mismatch : VerifyResult()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    /** SHA-256 hex — used only to detect and migrate legacy hashes. */
    private fun String.sha256Legacy(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun isLegacySha256(stored: String): Boolean =
        stored.matches(Regex("[0-9a-fA-F]{64}"))
}
