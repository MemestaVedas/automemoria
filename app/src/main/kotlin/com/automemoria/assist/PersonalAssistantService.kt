package moe.memesta.automemoria.assist

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import timber.log.Timber

/**
 * Registers this app as an Android Digital Assistant.
 * The user must go to Settings → Apps → Default Apps → Digital Assistant
 * and select "Automemoria" for the long-press power button to work.
 */
class PersonalAssistantService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Timber.d("PersonalAssistantService ready")
    }
}

/**
 * Session service — creates a VoiceInteractionSession when the assistant is triggered.
 * This launches QuickCaptureActivity as an overlay.
 */
class PersonalAssistantSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return PersonalAssistantSession(this)
    }
}

class PersonalAssistantSession(service: PersonalAssistantSessionService) :
    VoiceInteractionSession(service) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Timber.d("PersonalAssistantSession onShow — launching QuickCapture")

        // Launch the quick capture overlay
        val intent = Intent(context, QuickCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
        hide()
    }
}
