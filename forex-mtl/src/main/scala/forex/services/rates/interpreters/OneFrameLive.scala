package forex.services.rates.interpreters

import forex.services.rates.Algebra
import forex.domain._
import forex.services.rates.errors.{ Error => RateError }
import forex.config.ApplicationConfig
import cats.effect.Sync
import cats.syntax.all._
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe.jsonOf
import org.http4s.Method.GET
import org.typelevel.ci.CIString
import io.circe._
import io.circe.generic.semiauto._
import java.time.{OffsetDateTime, LocalDateTime, ZoneOffset}

class OneFrameLive[F[_]: Sync](
    config: ApplicationConfig,
    client: Client[F]
) extends Algebra[F] {

  import OneFrameLive._

  override def get(pair: Rate.Pair): F[RateError Either Rate] = {
    val uriResult = Uri.fromString(s"${config.oneFrame.baseUrl}/rates")
      .map(_.withQueryParam("pair", s"${pair.from}${pair.to}"))

    uriResult match {
      case Left(e) =>
        (RateError.OneFrameLookupFailed(s"Invalid URI: $e"): RateError).asLeft[Rate].pure[F]
      case Right(uri) =>
        val request = Request[F](GET, uri)
          .withHeaders(Header.Raw(CIString("token"), config.oneFrame.token))

        client.run(request).use { response =>
          if (response.status.isSuccess) {
            response.as[List[OneFrameRate]].map { rates =>
              rates.find(r => r.from == pair.from && r.to == pair.to) match {
                case Some(rate) =>
                  Rate(
                    pair,
                    rate.price,
                    Price(rate.bid),
                    Price(rate.ask),
                    Timestamp(rate.time_stamp)
                  ).asRight[RateError]
                case None =>
                  (RateError.OneFrameLookupFailed("Rate not found in response"): RateError).asLeft[Rate]
              }
            }
          } else {
            (RateError.OneFrameLookupFailed(s"Upstream returned status: ${response.status}"): RateError).asLeft[Rate].pure[F]
          }
        }.handleError { e =>
          (RateError.OneFrameLookupFailed(s"Request failed: ${e.getMessage}"): RateError).asLeft[Rate]
        }
    }
  }
}

object OneFrameLive {
  case class OneFrameRate(
      from: Currency,
      to: Currency,
      bid: BigDecimal,
      ask: BigDecimal,
      price: Price,
      time_stamp: OffsetDateTime
  )

  object OneFrameRate {
    implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emap { s =>
       Either.catchNonFatal(Currency.fromString(s)).leftMap(_ => s"Invalid currency: $s")
    }

    implicit val priceDecoder: Decoder[Price] = Decoder.decodeBigDecimal.map(Price(_))

    implicit val timestampDecoder: Decoder[OffsetDateTime] = Decoder.decodeString.emap { str =>
      Either.catchNonFatal(OffsetDateTime.parse(str))
        .orElse(Either.catchNonFatal(LocalDateTime.parse(str).atOffset(ZoneOffset.UTC)))
        .leftMap(_ => s"Failed to parse timestamp: $str")
    }

    implicit val decoder: Decoder[OneFrameRate] = deriveDecoder[OneFrameRate]
  }
  
  implicit def entityDecoder[F[_]: Sync]: EntityDecoder[F, List[OneFrameRate]] = jsonOf[F, List[OneFrameRate]]
}
