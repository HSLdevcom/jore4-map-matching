package fi.hsl.jore4.mapmatching.controller.ui

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class UiController {
    @Value("\${digitransit.subscription.key}")
    private val digitransitSubscriptionKey: String? = null

    @GetMapping("/")
    fun landingPage(model: Model): String {
        model.addAttribute("digitransit_key", digitransitSubscriptionKey)
        return "index"
    }
}
