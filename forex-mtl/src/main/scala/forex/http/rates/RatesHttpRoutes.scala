package forex.http
package rates

import cats.effect.Sync
import cats.syntax.all._
import forex.programs.RatesProgram
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    // GET /rates?from=USD&to=EUR
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates
        .get(RatesProgramProtocol.GetRatesRequest(from, to))
        .flatMap(Sync[F].fromEither)
        .flatMap { rate =>
          Ok(rate.asGetApiResponse)
        }

    // POST /rates
    case req @ POST -> Root =>
      for {
        request <- req.as[PostApiRequest]
        result <- rates.compare(
          RatesProgramProtocol.CompareRatesRequest(
            request.from,
            request.to,
            request.amount
          )
        )
        resp <- Sync[F].fromEither(result)
        ok <- Ok(resp.asPostApiResponse(request.amount))
      } yield ok
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
