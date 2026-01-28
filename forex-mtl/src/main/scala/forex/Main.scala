package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.blaze.client.BlazeClientBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(ExecutionContext.global).compile.drain.as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- Stream.resource(BlazeClientBuilder[F](ec).resource)
      module = new Module[F](config, client)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, "0.0.0.0")
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
