package forex.services.cache

import cats.effect.IO
import forex.domain._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._
import java.time.OffsetDateTime

class InMemoryCacheSpec extends AnyFlatSpec with Matchers {

  "InMemoryCache" should "store and retrieve rates" in {
    val cache = InMemoryCache[IO].unsafeRunSync()
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val rate = Rate(
      pair,
      Price(BigDecimal("0.71")),
      Price(BigDecimal("0.61")),
      Price(BigDecimal("0.82")),
      Timestamp(OffsetDateTime.now())
    )

    // Store rate
    cache.put(pair, rate, 5.minutes).unsafeRunSync()
    
    // Retrieve rate
    val retrieved = cache.get(pair).unsafeRunSync()
    retrieved shouldBe Some(rate)
  }

  it should "return None for expired rates" in {
    val cache = InMemoryCache[IO].unsafeRunSync()
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val rate = Rate(
      pair,
      Price(BigDecimal(0.71)),
      Price(BigDecimal(0.61)),
      Price(BigDecimal(0.82)),
      Timestamp(OffsetDateTime.now().minusMinutes(10))
    )

    // Store rate with 1ms TTL (expired immediately)
    cache.put(pair, rate, 1.millis).unsafeRunSync()
    
    // Wait a bit to ensure expiration
    Thread.sleep(10)
    
    // Retrieve rate - should be None due to expiration
    val retrieved = cache.get(pair).unsafeRunSync()
    retrieved shouldBe None
  }

  it should "remove rates" in {
    val cache = InMemoryCache[IO].unsafeRunSync()
    val pair = Rate.Pair(Currency.USD, Currency.JPY)
    val rate = Rate(
      pair,
      Price(BigDecimal(0.71)),
      Price(BigDecimal(0.61)),
      Price(BigDecimal(0.82)),
      Timestamp(OffsetDateTime.now())
    )

    // Store rate
    cache.put(pair, rate, 5.minutes).unsafeRunSync()
    
    // Remove rate
    cache.remove(pair).unsafeRunSync()
    
    // Retrieve rate - should be None
    val retrieved = cache.get(pair).unsafeRunSync()
    retrieved shouldBe None
  }
}