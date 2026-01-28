package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    bid: Price,
    ask: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )
}
