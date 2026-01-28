package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import interpreters._

import forex.config.ApplicationConfig
import forex.services.cache.CacheService
import org.http4s.client.Client

object Interpreters {
  // Defaut dummy data for testing
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  // Live data from OneFrame service
  def live[F[_]: Sync](config: ApplicationConfig, client: Client[F], cache: CacheService[F]): Algebra[F] = 
    new OneFrameLive[F](config, client, cache)
}
