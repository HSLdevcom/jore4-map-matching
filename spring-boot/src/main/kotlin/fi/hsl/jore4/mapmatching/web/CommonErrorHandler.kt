package fi.hsl.jore4.mapmatching.web

import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.function.Consumer
import javax.validation.ConstraintViolationException

/**
 * Provides error handler methods that return custom error messages back to client.
 */
@ControllerAdvice
class CommonErrorHandler {

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun onMissingParameterException(ex: MissingServletRequestParameterException): RoutingResponse {
        LOGGER.info("Handling missing request parameter: ${ex.message}")

        val message = "Required request parameter missing: \"${ex.parameterName}\""

        return RoutingResponse.invalidUrl(message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun onValidationException(ex: MethodArgumentNotValidException): RoutingResponse {
        LOGGER.info("Handling invalid method argument: ${ex.message}")

        val errors: MutableMap<String, String?> = HashMap()
        ex.bindingResult.allErrors.forEach(Consumer { error: ObjectError ->
            val fieldName = (error as FieldError).field
            errors[fieldName] = error.getDefaultMessage()
        })

        val message = errors.entries.joinToString(separator = ", ", prefix = "{", postfix = "}") { errorItem ->
            "\"${errorItem.key}\": \"${errorItem.value}\""
        }

        return RoutingResponse.invalidValue(message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun onDeserializationException(ex: HttpMessageNotReadableException): RoutingResponse {
        LOGGER.info("Handling deserialization exception: ${ex.message}")

        val message: String = ex.message ?: ex.javaClass.name

        return RoutingResponse.invalidValue(message)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun onConstraintViolationException(ex: ConstraintViolationException): String {
        LOGGER.error("Handling constraint violation", ex)

        return ex.message.toString()
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CommonErrorHandler::class.java)
    }
}
