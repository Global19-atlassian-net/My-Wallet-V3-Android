package com.blockchain.sunriver.ui

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

class ClaimFreeCryptoSuccessDialog : BaseAirdropBottomDialog(
    Content(
        title = R.string.claim_crypto_success_title,
        description = R.string.claim_crypto_success_body,
        ctaButtonText = android.R.string.ok
    )
) {

    private val analytics: Analytics by inject()

    override fun onStart() {
        super.onStart()
        analytics.logEvent(AnalyticsEvents.ClaimFreeCryptoSuccessDialog)
    }

    override fun rocketShipClick() {
        analytics.logEvent(AnalyticsEvents.ClaimFreeCryptoSuccessDialogClickedRocket)
        dismiss()
    }

    override fun ctaButtonClick() {
        analytics.logEvent(AnalyticsEvents.ClaimFreeCryptoSuccessDialogClicked)
        dismiss()
    }
}
