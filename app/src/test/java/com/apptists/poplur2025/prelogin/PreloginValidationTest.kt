package com.apptists.poplur2025.prelogin

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PreloginValidationTest {
    private val today = LocalDate.of(2026, 9, 8)
    private fun check(birthday: LocalDate?, agreed: Boolean = true) = PreloginValidation.credentials(" person@example.com ", "secret1", "secret1", birthday, agreed, today)
    @Test fun exactThirteenthBirthdayAndConsent() {
        assertNull(check(today.minusYears(13)))
        assertNotNull(check(today.minusYears(13).plusDays(1)))
        assertNotNull(check(today.plusDays(1)))
        assertNotNull(check(null))
        assertNotNull(check(today.minusYears(18), agreed = false))
    }
    @Test fun invalidEmailAndPasswordMismatch() {
        assertNotNull(PreloginValidation.credentials("no-email", "secret", "secret", today.minusYears(18), true, today))
        assertNotNull(PreloginValidation.credentials("a@b.com", "secret", "different", today.minusYears(18), true, today))
    }
    @Test fun backKeepsPersonalAndBusinessRoutesSeparate() {
        assertEquals(PreloginStep.Credentials, PreloginValidation.previous(PreloginStep.Photo))
        assertEquals(PreloginStep.Verification, PreloginValidation.previous(PreloginStep.Review))
        assertEquals(PreloginStep.Photo, PreloginValidation.previous(PreloginStep.Gender))
        assertEquals(PreloginStep.Account, PreloginValidation.previous(PreloginStep.Intro))
    }
}
