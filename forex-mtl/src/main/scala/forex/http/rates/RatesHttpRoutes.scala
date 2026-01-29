package forex.http
package rates

import cats.effect.Sync
import cats.syntax.all._
import forex.domain.Currency
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ParseFailure, MessageFailure}
import io.circe.{DecodingFailure, CursorOp}

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    // GET /rates?from=USD&to=EUR
    case req @ GET -> Root =>
      val queryParams = req.uri.multiParams
      val errors = scala.collection.mutable.ListBuffer[String]()

      // Parse 'from' parameter
      val fromOpt =
        queryParams.get("from").flatMap(_.headOption).flatMap { fromStr =>
          scala.util
            .Try(Currency.fromString(fromStr))
            .toOption
            .map(_ -> fromStr)
        }

      // Parse 'to' parameter
      val toOpt = queryParams.get("to").flatMap(_.headOption).flatMap { toStr =>
        scala.util.Try(Currency.fromString(toStr)).toOption.map(_ -> toStr)
      }

      // Validate 'from' parameter
      queryParams.get("from") match {
        case Some(values) if values.nonEmpty =>
          val fromValue = values.head
          if (fromOpt.isEmpty) {
            errors += s"Invalid 'from' currency: '$fromValue'. Must be one of: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD"
          }
        case _ => errors += "Missing required query parameter: 'from'"
      }

      // Validate 'to' parameter
      queryParams.get("to") match {
        case Some(values) if values.nonEmpty =>
          val toValue = values.head
          if (toOpt.isEmpty) {
            errors += s"Invalid 'to' currency: '$toValue'. Must be one of: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD"
          }
        case _ => errors += "Missing required query parameter: 'to'"
      }

      // If there are validation errors, return BadRequest
      if (errors.nonEmpty) {
        BadRequest(ErrorResponse(errors.mkString("; ")))
      } else {
        // Both parameters are valid, proceed with the request
        (fromOpt, toOpt) match {
          case (Some((from, _)), Some((to, _))) =>
            rates
              .get(RatesProgramProtocol.GetRatesRequest(from, to))
              .flatMap {
                case Right(rate) => Ok(rate.asGetApiResponse)
                case Left(error) => mapProgramError(error)
              }
          case _ =>
            BadRequest(
              ErrorResponse(
                "Missing required query parameters. Both 'from' and 'to' parameters are required. " +
                  "Example: /rates?from=USD&to=EUR"
              )
            )
        }
      }

    // POST /rates
    case req @ POST -> Root =>
      req
        .as[PostApiRequest]
        .flatMap { request =>
          rates
            .compare(
              RatesProgramProtocol.CompareRatesRequest(
                request.from,
                request.to,
                request.amount
              )
            )
            .flatMap {
              case Right(rate) => Ok(rate.asPostApiResponse(request.amount))
              case Left(error) => mapProgramError(error)
            }
        }
        .handleErrorWith {
          case decodingFailure: DecodingFailure =>
            BadRequest(ErrorResponse(extractFieldError(decodingFailure)))
          case parseFailure: ParseFailure =>
            BadRequest(ErrorResponse(s"Invalid request: ${parseFailure.message}"))
          case messageFailure: MessageFailure =>
            val cause = messageFailure.cause
            val errorMessage = cause match {
              case Some(df: DecodingFailure) => extractFieldError(df)
              case _                         => s"Invalid request: ${messageFailure.message}"
            }
            BadRequest(ErrorResponse(errorMessage))
          case throwable: Throwable =>
            BadRequest(ErrorResponse(s"Invalid request body: ${throwable.getMessage}"))
        }
  }

  // Map program errors to HTTP responses
  private def mapProgramError(
      error: forex.programs.rates.errors.Error
  ): F[org.http4s.Response[F]] = {
    import forex.programs.rates.errors.Error._
    error match {
      case RateLookupFailed(msg) =>
        BadGateway(ErrorResponse(s"Upstream service error: $msg"))
      case RateStale(msg) =>
        ServiceUnavailable(
          ErrorResponse(s"Rate is currently unavailable: $msg")
        )
      case _ =>
        InternalServerError(ErrorResponse("An unexpected error occurred"))
    }
  }

  // Helper method to extract field-specific error messages from DecodingFailure
  private def extractFieldError(failure: DecodingFailure): String = {
    val fieldPath = failure.history
      .collectFirst { case CursorOp.DownField(field) =>
        field
      }
      .getOrElse("unknown")

    failure.message match {
      case msg if msg.contains("Invalid currency") =>
        s"Invalid field '$fieldPath': $msg. Must be one of: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD"
      case msg if msg.contains("amount") || msg.contains("Field 'amount'") =>
        msg
      case msg if msg.contains("Missing") =>
        s"Missing required field: '$fieldPath'"
      case msg =>
        s"Invalid field '$fieldPath': $msg"
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
