package vmq.ui.main

import vmq.ui.common.UiText

sealed interface MainViewEffect {
    data class ShowToast(val message: UiText) : MainViewEffect
}
