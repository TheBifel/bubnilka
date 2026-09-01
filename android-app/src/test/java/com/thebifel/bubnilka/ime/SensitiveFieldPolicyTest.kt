package com.thebifel.bubnilka.ime

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveFieldPolicyTest {
    @Test
    fun blocksTextPasswordVariations() {
        assertTrue(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertTrue(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    }

    @Test
    fun blocksNumericPasswordButAllowsOrdinaryNumber() {
        assertTrue(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL))
    }

    @Test
    fun allowsOrdinaryTextAndEmail() {
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_TEXT))
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
    }

    @Test
    fun classificationIgnoresNonVariationFlags() {
        val passwordWithFlags = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val ordinaryWithFlags = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        assertTrue(SensitiveFieldPolicy.isSensitive(passwordWithFlags))
        assertFalse(SensitiveFieldPolicy.isSensitive(ordinaryWithFlags))
    }

    @Test
    fun doesNotTreatPhoneDateOrNullInputsAsPasswords() {
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_PHONE))
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_CLASS_DATETIME))
        assertFalse(SensitiveFieldPolicy.isSensitive(InputType.TYPE_NULL))
    }
}
