package forex.http.rates

import forex.domain._

object Converters {
  import Protocol._

  // Response body for GET /rates?from={currencyPair}&to={currencyPair}
  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        bid = rate.bid,
        ask = rate.ask,
        timestamp = rate.timestamp
      )
  }

  // Response body for POST /rates
  private[rates] implicit class PostApiResponseOps(val rate: Rate) extends AnyVal {
    def asPostApiResponse(amount: BigDecimal): PostApiResponse =
      PostApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        amount = amount,
        exchangeRate = rate.price,
        convertedAmount = amount * rate.price.value,
        timestamp = rate.timestamp
      )
  }

}
