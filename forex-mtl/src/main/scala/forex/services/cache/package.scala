package forex.services

import cats.effect.Sync

package object cache {
  type CacheService[F[_]] = forex.services.cache.Algebra[F]

  object CacheService {
    def apply[F[_]: Sync]: F[CacheService[F]] = InMemoryCache[F]
  }
}