package forex.services.rates

import cats.Applicative
import interpreters._

import forex.config.ApplicationConfig
import org.http4s.client.Client

object Interpreters {
  // TODO: replace dummy data with live Oneframe data
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Applicative](config: ApplicationConfig, client: Client[F]): Algebra[F] = {
    val _ = config
    val _ = client
    new OneFrameDummy[F]()
  }
}
