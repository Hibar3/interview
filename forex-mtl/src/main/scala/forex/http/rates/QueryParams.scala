package forex.http.rates

import forex.domain.Currency
import org.http4s.QueryParamDecoder
import org.http4s.ParseFailure
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { s =>
      scala.util.Try(Currency.fromString(s)).toEither.left.map { _ =>
        ParseFailure(
          s"Invalid currency: '$s'",
          s"Currency must be one of: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD"
        )
      }
    }

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")

}
