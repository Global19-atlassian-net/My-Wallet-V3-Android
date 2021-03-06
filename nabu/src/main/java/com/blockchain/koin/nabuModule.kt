package com.blockchain.koin

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.NabuUserSync
import com.blockchain.swap.nabu.api.nabu.Nabu
import com.blockchain.swap.nabu.api.nabu.NabuMarkets
import com.blockchain.swap.nabu.api.trade.TransactionStateAdapter
import com.blockchain.swap.nabu.datamanagers.AnalyticsNabuUserReporterImpl
import com.blockchain.swap.nabu.datamanagers.AnalyticsWalletReporter
import com.blockchain.swap.nabu.datamanagers.BalanceProviderImpl
import com.blockchain.swap.nabu.datamanagers.BalancesProvider
import com.blockchain.swap.nabu.datamanagers.CreateNabuTokenAdapter
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.NabuAuthenticator
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManagerImpl
import com.blockchain.swap.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.swap.nabu.datamanagers.NabuDataUserProviderNabuDataManagerAdapter
import com.blockchain.swap.nabu.datamanagers.NabuUserReporter
import com.blockchain.swap.nabu.datamanagers.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.swap.nabu.datamanagers.UniqueAnalyticsNabuUserReporter
import com.blockchain.swap.nabu.datamanagers.UniqueAnalyticsWalletReporter
import com.blockchain.swap.nabu.datamanagers.WalletReporter
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.featureflags.FeatureEligibility
import com.blockchain.swap.nabu.datamanagers.featureflags.KycFeatureEligibility
import com.blockchain.swap.nabu.datamanagers.repositories.AssetBalancesRepository
import com.blockchain.swap.nabu.datamanagers.repositories.NabuUserRepository
import com.blockchain.swap.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestEnabledProvider
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestEnabledProviderImpl
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestEnabledRepository
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestLimitsProvider
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestLimitsProviderImpl
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestLimitsRepository
import com.blockchain.swap.nabu.datamanagers.repositories.serialization.InterestLimitsMapAdapter
import com.blockchain.swap.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.swap.nabu.models.nabu.CampaignStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.CampaignTransactionStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.IsoDateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.KycStateAdapter
import com.blockchain.swap.nabu.models.nabu.KycTierStateAdapter
import com.blockchain.swap.nabu.models.nabu.UserCampaignStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.UserStateAdapter
import com.blockchain.swap.nabu.service.NabuMarketsService
import com.blockchain.swap.nabu.service.NabuService
import com.blockchain.swap.nabu.service.NabuTierService
import com.blockchain.swap.nabu.service.RetailWalletTokenService
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.swap.nabu.service.TierUpdater
import com.blockchain.swap.nabu.service.TradeLimitService
import com.blockchain.swap.nabu.status.KycTiersQueries
import com.blockchain.swap.nabu.stores.NabuSessionTokenStore
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val nabuModule = module {

    single { get<Retrofit>(nabu).create(NabuMarkets::class.java) }

    scope(payloadScopeQualifier) {

        factory { NabuMarketsService(get(), get()) }
            .bind(TradeLimitService::class)

        factory {
            MetadataRepositoryNabuTokenAdapter(
                metadataRepository = get(),
                createNabuToken = get(),
                metadataManager = get()
            )
        }.bind(NabuToken::class)

        factory {
            NabuDataManagerImpl(
                nabuService = get(),
                retailWalletTokenService = get(),
                nabuTokenStore = get(),
                appVersion = getProperty("app-version"),
                settingsDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                walletReporter = get(uniqueId),
                userReporter = get(uniqueUserAnalytics)
            )
        }.bind(NabuDataManager::class)

        factory {
            LiveCustodialWalletManager(
                nabuService = get(),
                authenticator = get(),
                simpleBuyPrefs = get(),
                paymentAccountMapperMappers = mapOf(
                    "EUR" to get(eur), "GBP" to get(gbp)
                ),
                cardsPaymentFeatureFlag = get(cardPaymentsFeatureFlag),
                fundsFeatureFlag = get(simpleBuyFundsFeatureFlag),
                kycFeatureEligibility = get(),
                assetBalancesRepository = get(),
                interestLimitsRepository = get(),
                interestEnabledRepository = get()
            )
        }.bind(CustodialWalletManager::class)

        factory {
            InterestLimitsProviderImpl(
                nabuService = get(),
                authenticator = get(),
                currencyPrefs = get(),
                exchangeRates = get()
            )
        }.bind(InterestLimitsProvider::class)

        factory {
            InterestEnabledProviderImpl(
                nabuService = get(),
                authenticator = get()
            )
        }.bind(InterestEnabledProvider::class)

        factory {
            BalanceProviderImpl(
                nabuService = get(),
                authenticator = get()
            )
        }.bind(BalancesProvider::class)

        factory(uniqueUserAnalytics) {
            UniqueAnalyticsNabuUserReporter(
                nabuUserReporter = get(userAnalytics),
                prefs = get()
            )
        }.bind(NabuUserReporter::class)

        factory(userAnalytics) {
            AnalyticsNabuUserReporterImpl(
                userAnalytics = get()
            )
        }.bind(NabuUserReporter::class)

        factory(uniqueId) {
            UniqueAnalyticsWalletReporter(get(walletAnalytics), prefs = get())
        }.bind(WalletReporter::class)

        factory(walletAnalytics) {
            AnalyticsWalletReporter(userAnalytics = get())
        }.bind(WalletReporter::class)

        factory {
            get<Retrofit>(nabu).create(Nabu::class.java)
        }

        factory { NabuTierService(get(), get()) }
            .bind(TierService::class)
            .bind(TierUpdater::class)

        factory {
            CreateNabuTokenAdapter(get())
        }.bind(CreateNabuToken::class)

        factory { NabuDataUserProviderNabuDataManagerAdapter(get(), get()) }.bind(
            NabuDataUserProvider::class)

        factory { NabuUserSyncUpdateUserWalletInfoWithJWT(get(), get()) }.bind(NabuUserSync::class)

        factory { KycTiersQueries(get(), get()) }

        scoped { KycFeatureEligibility(userRepository = get()) }.bind(FeatureEligibility::class)

        scoped {
            NabuUserRepository(
                nabuDataUserProvider = get())
        }

        scoped {
            AssetBalancesRepository(balancesProvider = get())
        }

        scoped {
            InterestLimitsRepository(interestLimitsProvider = get())
        }

        scoped {
            InterestEnabledRepository(interestEnabledProvider = get())
        }
        scoped {
            WithdrawLocksRepository(custodialWalletManager = get())
        }
    }

    moshiInterceptor(nabu) { builder ->
        builder.add(TransactionStateAdapter())
    }

    moshiInterceptor(interestLimits) { builder ->
        builder.add(InterestLimitsMapAdapter())
    }

    single { NabuSessionTokenStore() }

    single { NabuService(get(nabu)) }

    single {
        RetailWalletTokenService(
            environmentConfig = get(),
            apiCode = getProperty("api-code"),
            retrofit = get(moshiExplorerRetrofit)
        )
    }

    moshiInterceptor(kyc) { builder ->
        builder
            .add(KycStateAdapter())
            .add(KycTierStateAdapter())
            .add(UserStateAdapter())
            .add(IsoDateMoshiAdapter())
            .add(UserCampaignStateMoshiAdapter())
            .add(CampaignStateMoshiAdapter())
            .add(CampaignTransactionStateMoshiAdapter())
    }
}

val authenticationModule = module {
    scope(payloadScopeQualifier) {
        factory {
            NabuAuthenticator(
                nabuToken = get(),
                nabuDataManager = get(),
                crashLogger = get()
            )
        }.bind(Authenticator::class)
    }
}