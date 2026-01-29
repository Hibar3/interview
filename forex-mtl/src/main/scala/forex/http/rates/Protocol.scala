package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder
}

object Protocol {

  implicit val configuration: Configuration =
    Configuration.default

  // Request body for GET /rates
  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )
  // Response body for GET /rates
  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      bid: Price,
      ask: Price,
      timestamp: Timestamp
  )

  // Request body for POST /rates
  final case class PostApiRequest(
      from: Currency,
      to: Currency,
      amount: BigDecimal
  )

  // Response body for POST /rates
  final case class PostApiResponse(
      from: Currency,
      to: Currency,
      amount: BigDecimal,
      exchangeRate: Price,
      convertedAmount: BigDecimal,
      timestamp: Timestamp
  )
  // Response body for errors
  final case class ErrorResponse(
      message: String
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val currencyDecoder: Decoder[Currency] =
    Decoder.decodeString.emap { s =>
      scala.util
        .Try(Currency.fromString(s))
        .toEither
        .left
        .map(_ => s"Invalid currency: $s")
    }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

  implicit val postRequestDecoder: Decoder[PostApiRequest] =
    deriveConfiguredDecoder[PostApiRequest]
      .emap { request =>
        val errors = scala.collection.mutable.ListBuffer[String]()
        
        // Validate amount is not negative
        if (request.amount < 0) {
          errors += s"Field 'amount' must be a positive number"
        }
        
        // checks if there are any errors in the list
        if (errors.nonEmpty) {
          Left(errors.mkString("; "))
        } else {
          Right(request)
        }
      }

  implicit val postResponseEncoder: Encoder[PostApiResponse] =
    deriveConfiguredEncoder[PostApiResponse]

  implicit val errorResponseEncoder: Encoder[ErrorResponse] =
    deriveConfiguredEncoder[ErrorResponse]

}
