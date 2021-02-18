package com.r307.arbitrader.service.paper;

import com.r307.arbitrader.BaseTestCase;
import com.r307.arbitrader.config.ExchangeConfiguration;
import com.r307.arbitrader.config.FeeComputation;
import com.r307.arbitrader.config.PaperConfiguration;
import com.r307.arbitrader.service.ExchangeService;
import com.r307.arbitrader.service.TickerService;
import com.r307.arbitrader.service.TradingService;
import org.junit.Before;
import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import si.mazi.rescu.SynchronizedValueFactory;

import java.awt.print.Paper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.r307.arbitrader.DecimalConstants.BTC_SCALE;
import static com.r307.arbitrader.DecimalConstants.USD_SCALE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PaperTradeServiceTest extends BaseTestCase {

    PaperExchange paperExchange;
    PaperTradeService paperTradeService;


    ExchangeConfiguration exchangeConfiguration;

    @Mock
    private TickerService tickerService;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private Exchange exchange;

    @Before
    public void setUp() {
        PaperConfiguration paperConfiguration = new PaperConfiguration();
        paperConfiguration.setActive(true);
        paperConfiguration.setInitialBalance(new BigDecimal("100"));
        exchangeConfiguration = new ExchangeConfiguration();

        paperExchange = new PaperExchange(exchange, Currency.USD,tickerService, exchangeService, paperConfiguration);
        paperTradeService = paperExchange.getPaperTradeService();

        when(exchange.getExchangeSpecification()).thenReturn(new ExchangeSpecification(PaperExchange.class));
        when(exchangeService.getExchangeMetadata(any(Exchange.class))).thenReturn(exchangeConfiguration);
    }

    @Test
    public void testFillOrderBuyFeeComputationClient() {
        exchangeConfiguration.setFeeComputation(FeeComputation.CLIENT);
        when(exchangeService.getExchangeMetadata(any(Exchange.class))).thenReturn(exchangeConfiguration);
        when(exchangeService.getExchangeFee(any(Exchange.class),any(CurrencyPair.class),anyBoolean())).thenReturn(new BigDecimal("0.001"));

        //TODO this test use ad-hock data, test with real data!
        //tradingService.addFees(...,...,5) will return 5.005
        LimitOrder order = new LimitOrder.Builder(Order.OrderType.BID, CurrencyPair.BTC_USD)
            .originalAmount(new BigDecimal("5.005"))
            .timestamp(new Date())
            .limitPrice(new BigDecimal("10"))
            .orderStatus(Order.OrderStatus.NEW)
            .build();

        paperTradeService.fillOrder(order, order.getLimitPrice());
        assertEquals(new BigDecimal("49.95").setScale(2, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.USD).setScale(2, RoundingMode.HALF_EVEN));
        assertEquals(new BigDecimal("5").setScale(BTC_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.BTC).setScale(BTC_SCALE, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testFillOrderSellFeeComputationClient() {
        exchangeConfiguration.setFeeComputation(FeeComputation.CLIENT);
        when(exchangeService.getExchangeMetadata(any(Exchange.class))).thenReturn(exchangeConfiguration);
        when(exchangeService.getExchangeFee(any(Exchange.class),any(CurrencyPair.class),anyBoolean())).thenReturn(new BigDecimal("0.001"));

        //TODO this test use ad-hock data, test with real data!
        //tradingService.subtractFees(...,...,5) will return 4.995
        LimitOrder order = new LimitOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_USD)
            .originalAmount(new BigDecimal("4.995"))
            .timestamp(new Date())
            .limitPrice(new BigDecimal("10"))
            .orderStatus(Order.OrderStatus.NEW)
            .build();

        paperTradeService.fillOrder(order, order.getLimitPrice());
        assertEquals(new BigDecimal("149.95").setScale(USD_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.USD).setScale(USD_SCALE, RoundingMode.HALF_EVEN));
        assertEquals(new BigDecimal("-5").setScale(BTC_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.BTC).setScale(BTC_SCALE, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testFillOrderBuyFeeComputationServer() {
        exchangeConfiguration.setFeeComputation(FeeComputation.SERVER);
        when(exchangeService.getExchangeMetadata(any(Exchange.class))).thenReturn(exchangeConfiguration);
        when(exchangeService.getExchangeFee(any(Exchange.class),any(CurrencyPair.class),anyBoolean())).thenReturn(new BigDecimal("0.001"));

        LimitOrder order = new LimitOrder.Builder(Order.OrderType.BID, CurrencyPair.BTC_USD)
            .originalAmount(new BigDecimal("5"))
            .timestamp(new Date())
            .limitPrice(new BigDecimal("10"))
            .orderStatus(Order.OrderStatus.NEW)
            .build();

        paperTradeService.fillOrder(order, order.getLimitPrice());

        assertEquals(new BigDecimal("49.95").setScale(2, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.USD).setScale(2, RoundingMode.HALF_EVEN));
        assertEquals(new BigDecimal("5").setScale(BTC_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.BTC).setScale(BTC_SCALE, RoundingMode.HALF_EVEN));
    }

    @Test
    public void testFillOrderSellFeeComputationServer() {
        exchangeConfiguration.setFeeComputation(FeeComputation.SERVER);
        when(exchangeService.getExchangeFee(any(Exchange.class),any(CurrencyPair.class),anyBoolean())).thenReturn(new BigDecimal("0.001"));

        LimitOrder order = new LimitOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_USD)
            .originalAmount(new BigDecimal("5"))
            .timestamp(new Date())
            .limitPrice(new BigDecimal("10"))
            .orderStatus(Order.OrderStatus.NEW)
            .build();

        paperTradeService.fillOrder(order, order.getLimitPrice());
        assertEquals(new BigDecimal("149.95").setScale(USD_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.USD).setScale(USD_SCALE, RoundingMode.HALF_EVEN));
        assertEquals(new BigDecimal("-5").setScale(BTC_SCALE, RoundingMode.HALF_EVEN), paperExchange.getPaperAccountService().getBalance(Currency.BTC).setScale(BTC_SCALE, RoundingMode.HALF_EVEN));
    }
}