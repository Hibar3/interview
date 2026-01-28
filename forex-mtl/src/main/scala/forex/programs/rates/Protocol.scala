package forex.programs.rates

import forex.domain.Currency

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )

  final case class CompareRatesRequest(
      from: Currency,
      to: Currency,
      amount: BigDecimal
  )

}
