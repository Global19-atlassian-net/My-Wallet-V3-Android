package piuk.blockchain.android.coincore.impl

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.math.BigDecimal

internal abstract class CryptoAssetBase(
    protected val payloadManager: PayloadDataManager,
    protected val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    protected val currencyPrefs: CurrencyPrefs,
    protected val labels: DefaultLabels,
    protected val custodialManager: CustodialWalletManager,
    private val pitLinking: PitLinking,
    protected val crashLogger: CrashLogger,
    private val tiersService: TierService,
    protected val environmentConfig: EnvironmentConfig
) : CryptoAsset {

    private val accounts = mutableListOf<SingleAccount>()
    override val isEnabled: Boolean
        get() = !asset.hasFeature(CryptoCurrency.STUB_ASSET)

    // Init token, set up accounts and fetch a few activities
    override fun init(): Completable =
        initToken()
            .doOnError { throwable ->
                crashLogger.logException(throwable, "Coincore: Failed to load $asset wallet")
            }
            .then { loadAccounts() }
            .doOnComplete { Timber.d("Coincore: Init $asset Complete") }
            .doOnError { Timber.d("Coincore: Init $asset Failed") }

    private fun loadAccounts(): Completable =
        Completable.fromCallable { accounts.clear() }
            .then {
                loadNonCustodialAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .then {
                loadCustodialAccount()
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .then {
                loadInterestAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .doOnError { Timber.e("Error loading accounts for ${asset.networkTicker}: $it") }

    abstract fun initToken(): Completable

    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList>

    private fun loadInterestAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(
            CryptoInterestAccount(
                asset,
                labels.getDefaultInterestWalletLabel(asset),
                custodialManager,
                exchangeRates,
                environmentConfig
            )
        ).flatMap { account ->
            account.isInterestEnabled().map {
                if (account.isConfigured) {
                    listOf(account)
                } else {
                    emptyList()
                }
            }
        }

    override fun interestRate(): Single<Double> = custodialManager.getInterestAccountRates(asset)

    open fun loadCustodialAccount(): Single<SingleAccountList> =
        Single.just(
            CustodialTradingAccount(
                asset = asset,
                label = labels.getDefaultCustodialWalletLabel(asset),
                exchangeRates = exchangeRates,
                custodialWalletManager = custodialManager,
                environmentConfig = environmentConfig
            )
        ).flatMap { account ->
            account.accountBalance.map {
                if (account.isConfigured) {
                    listOf(account)
                } else {
                    emptyList()
                }
            }
        }

    final override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        Maybe.fromCallable {
            filterTokenAccounts(asset, labels, accounts, filter)
        }

    final override fun defaultAccount(): Single<SingleAccount> =
        Single.fromCallable {
            accounts.first { it.isDefault }
        }

    private fun getNonCustodialAccountList(): Single<SingleAccountList> =
        accountGroup(filter = AssetFilter.NonCustodial)
            .map { group -> group.accounts.mapNotNull { it as? SingleAccount } }
            .toSingle(emptyList())

    final override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.fetchExchangeRate(asset, currencyPrefs.selectedFiatCurrency)
            .map {
                ExchangeRate.CryptoToFiat(
                    asset,
                    currencyPrefs.selectedFiatCurrency,
                    it
                )
            }

    final override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricPrice(asset, currencyPrefs.selectedFiatCurrency, epochWhen)
            .map {
                ExchangeRate.CryptoToFiat(
                    asset,
                    currencyPrefs.selectedFiatCurrency,
                    it.toBigDecimal()
                )
            }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(asset, currencyPrefs.selectedFiatCurrency, period)

    private fun getPitLinkingAccount(): Maybe<SingleAccount> =
        pitLinking.isPitLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(asset) }
            .map { address ->
                CryptoExchangeAccount(
                    asset = asset,
                    label = labels.getDefaultExchangeWalletLabel(asset),
                    address = address,
                    exchangeRates = exchangeRates,
                    environmentConfig = environmentConfig
                )
            }

    private fun getInterestAccount(): Maybe<SingleAccount> =
        tiersService.tiers().flatMapMaybe { tier ->
            if (tier.isApprovedFor(KycTierLevel.GOLD)) {
                val interestAccounts = accounts.filterIsInstance<CryptoInterestAccount>()
                if (interestAccounts.isNotEmpty()) {
                    Maybe.just(interestAccounts.first())
                } else {
                    Maybe.empty()
                }
            } else {
                Maybe.empty()
            }
        }

    private fun getCustodialAccount(): Maybe<SingleAccount> =
        accountGroup(AssetFilter.Custodial)
            .map { it.accounts.first() }
            .onErrorComplete()

    final override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> {
        require(account is CryptoAccount)
        require(account.asset == asset)

        return when (account) {
            is TradingAccount -> getNonCustodialAccountList()
            is NonCustodialAccount ->
                Maybe.concat(
                    listOf(
                        getPitLinkingAccount(),
                        getInterestAccount(),
                        getCustodialAccount()
                    )
                ).toList()
                    .onErrorReturnItem(emptyList())
            else -> Single.just(emptyList())
        }
    }
}

fun ExchangeRateDataManager.fetchExchangeRate(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<BigDecimal> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { it.toBigDecimal() }
