package forex.services.cache

import forex.domain.Rate
import scala.concurrent.duration.FiniteDuration

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Option[Rate]]
  def put(pair: Rate.Pair, rate: Rate, ttl: FiniteDuration): F[Unit]
  def remove(pair: Rate.Pair): F[Unit]
  def clear(): F[Unit]
}