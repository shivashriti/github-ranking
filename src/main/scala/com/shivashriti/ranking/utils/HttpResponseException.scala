package com.shivashriti.ranking.utils

import com.shivashriti.ranking.ErrorResponse

abstract class HttpResponseException extends Exception {
  def error: ErrorResponse
}

object HttpResponseException{
  case object UnknownResponseException extends HttpResponseException{
    def error = ErrorResponse("Did not receive valid Http response from GitHub for the resource", 500)
  }
  case object ResourceNotFoundException extends HttpResponseException{
    val error = ErrorResponse("Resource not found on GitHub", 404)
  }
  case object InternalErrorException extends HttpResponseException{
    val error = (ErrorResponse("Internal Server Error! It seems a request to GitHub did not complete. Please Try again", 500))
  }
  case object ForbiddenException extends HttpResponseException{
    val error = (ErrorResponse("Request forbidden! Suggestion: Please check your authorization token", 403))
  }
}