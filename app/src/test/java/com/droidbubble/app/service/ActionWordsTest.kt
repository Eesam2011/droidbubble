package com.droidbubble.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B4.3: button words match whole words only. */
class ActionWordsTest {

    @Test
    fun end_matchesOnlyTheWholeWord() {
        assertFalse("Send".hasWord("end"))
        assertFalse("Weekend".hasWord("end"))
        assertFalse("Recommend".hasWord("end"))
        assertFalse("Ending soon".hasWord("end"))
        assertTrue("End call".hasWord("end"))
        assertTrue("END".hasWord("end"))
        assertTrue("Call · End".hasWord("end"))
    }

    @Test
    fun phrases_andAnyOfSeveralWords() {
        assertTrue("Hang up".hasWord("hang up"))
        assertTrue("Tap to hang up now".hasWord("decline", "hang up"))
        assertFalse("Hangup".hasWord("hang up"))
        assertTrue("Hangup".hasWord("hang up", "hangup"))
        assertTrue("Stop recording".hasWord("stop", "end", "finish"))
        assertFalse("Nonstop".hasWord("stop"))
    }

    @Test
    fun nonLatinText_andDigitsBorderWords() {
        assertTrue("إنهاء المكالمة".hasWord("إنهاء"))
        assertFalse("end2end".hasWord("end"))
        assertTrue("end-to-end".hasWord("end"))
        assertFalse("".hasWord("end"))
        assertFalse("End".hasWord(""))
    }
}
