package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import interpreters._

import forex.config.ApplicationConfig
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](config: ApplicationConfig, client: Client[F]): Algebra[F] = 
    new OneFrameLive[F](config, client)
}
