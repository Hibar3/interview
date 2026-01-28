package forex.services.cache

import cats.effect.Sync
import cats.syntax.all._
import forex.domain.Rate
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.ConcurrentHashMap

class InMemoryCache[F[_]: Sync] private (
    cache: ConcurrentHashMap[Rate.Pair, CacheEntry]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Option[Rate]] = {
    val currentTime = System.currentTimeMillis()
    val entry = Option(cache.get(pair))
    val validEntry = entry.filter(_.isValid(currentTime))
    
    // Remove expired entries
    if (entry.isDefined && validEntry.isEmpty) {
      cache.remove(pair)
    }
    
    Sync[F].pure(validEntry.map(_.rate))
  }

  override def put(pair: Rate.Pair, rate: Rate, ttl: FiniteDuration): F[Unit] = {
    val currentTime = System.currentTimeMillis()
    val expiresAt = currentTime + ttl.toMillis
    cache.put(pair, CacheEntry(rate, expiresAt))
    Sync[F].pure(())
  }

  override def remove(pair: Rate.Pair): F[Unit] = {
    Sync[F].delay(cache.remove(pair)).void
  }

  override def clear(): F[Unit] = {
    Sync[F].delay(cache.clear()).void
  }
}

object InMemoryCache {
  def apply[F[_]: Sync]: F[Algebra[F]] = {
    Sync[F].delay(new ConcurrentHashMap[Rate.Pair, CacheEntry]()).map(new InMemoryCache[F](_))
  }
}

private case class CacheEntry(rate: Rate, expiresAt: Long) {
  def isValid(currentTime: Long): Boolean = currentTime < expiresAt
}