package forex.services.rates.interpreters

import cats.effect.IO
import forex.config._
import forex.domain._
import forex.services.rates.errors.Error
import forex.services.cache.CacheService
import org.http4s._
import org.http4s.client.Client
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._
import java.time.OffsetDateTime

import pureconfig.ConfigSource
import pureconfig.generic.auto._

class OneFrameLiveSpec extends AnyFlatSpec with Matchers {

  val config = ConfigSource.default.at("app").load[ApplicationConfig].getOrElse(
    ApplicationConfig(
      HttpConfig("localhost", 8080, 40.seconds),
      OneFrameConfig("http://localhost:8080", "token", 5.minutes)
    )
  )

  val sampleJson = """
    [
      {
        "from": "USD",
        "to": "JPY",
        "bid": 0.61,
        "ask": 0.82,
        "price": 0.71,
        "time_stamp": "2019-01-01T00:00:00.000"
      }
    ]
  """

  "OneFrameLive" should "correctly adapt valid response to domain Rate" in {
    val client = Client.fromHttpApp[IO](HttpRoutes.of[IO] {
      case _ => IO.pure(Response[IO](Status.Ok).withEntity(sampleJson))
    }.orNotFound)

    val cache = CacheService[IO].unsafeRunSync()
    val interpreter = new OneFrameLive[IO](config, client, cache)
    val pair = Rate.Pair(Currency.USD, Currency.JPY)

    val result = interpreter.get(pair).unsafeRunSync()

    result should matchPattern { case Right(_) => }
    val rate = result.toOption.get
    rate.pair shouldBe pair
    rate.price shouldBe Price(BigDecimal(0.71))
    rate.bid shouldBe Price(BigDecimal(0.61))
    rate.ask shouldBe Price(BigDecimal(0.82))
    rate.timestamp.value shouldBe OffsetDateTime.parse("2019-01-01T00:00:00Z")
  }

  it should "return OneFrameLookupFailed when pair does not match" in {
    val client = Client.fromHttpApp[IO](HttpRoutes.of[IO] {
      case _ => IO.pure(Response[IO](Status.Ok).withEntity(sampleJson))
    }.orNotFound)

    val cache = CacheService[IO].unsafeRunSync()
    val interpreter = new OneFrameLive[IO](config, client, cache)
    val pair = Rate.Pair(Currency.EUR, Currency.USD) // Mismatch

    val result = interpreter.get(pair).unsafeRunSync()

    result should matchPattern { case Left(Error.OneFrameLookupFailed("Rate not found in response")) => }
  }

  it should "handle invalid JSON gracefully" in {
    val client = Client.fromHttpApp[IO](HttpRoutes.of[IO] {
      case _ => IO.pure(Response[IO](Status.Ok).withEntity("invalid json"))
    }.orNotFound)

    val cache = CacheService[IO].unsafeRunSync()
    val interpreter = new OneFrameLive[IO](config, client, cache)
    val pair = Rate.Pair(Currency.USD, Currency.JPY)

    val result = interpreter.get(pair).unsafeRunSync()

    result should matchPattern { case Left(Error.OneFrameLookupFailed(_)) => }
  }
}
