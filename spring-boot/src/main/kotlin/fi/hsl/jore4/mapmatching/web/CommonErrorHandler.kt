package fi.hsl.jore4.mapmatching.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun onMissingParameterException(ex: MissingServletRequestParameterException): String {
        LOGGER.info("Handling missing request parameter", ex)

        return "Required request parameter '${ex.parameterName}' missing"
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun onValidationException(ex: MethodArgumentNotValidException): Map<String, String?> {
        LOGGER.info("Handling invalid method argument", ex)

        val errors: MutableMap<String, String?> = HashMap()
        ex.bindingResult.allErrors.forEach(Consumer { error: ObjectError ->
            val fieldName = (error as FieldError).field
            errors[fieldName] = error.getDefaultMessage()
        })
        return errors
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
