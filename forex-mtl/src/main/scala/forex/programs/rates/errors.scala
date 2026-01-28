package forex.programs.rates

import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception {
    def getMessage: String
  }
  object Error {
    final case class RateLookupFailed(msg: String) extends Error {
      override def getMessage: String = msg
    }
    final case class RateStale(msg: String) extends Error {
      override def getMessage: String = msg
    }
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.RateStale(msg) => Error.RateStale(msg)
  }
}
