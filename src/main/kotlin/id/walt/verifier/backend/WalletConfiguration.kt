package id.walt.verifier.backend

import id.walt.webwallet.backend.config.ExternalHostnameUrl

data class WalletConfiguration(
  val id: String,
  @ExternalHostnameUrl val url: String,
  val presentPath: String,
  val receivePath: String,
  val description: String
) {

  companion object {
    fun getDefaultWalletConfigurations(): Map<String, WalletConfiguration> {
      return mapOf(


//        Pair("walt.id", WalletConfiguration("antras", "http://localhost:3000", "api/wallet/siopv2/initPresentation", "pi/wallet/siopv2/initPassiveIssuancea" , "walt.id web wallet"))
//                Pair("walt.id", WalletConfiguration("trecias", "http://localhost:3000", "api/wallet/siopv2/initPresentation", "pi/wallet/siopv2/initPassiveIssuancea" , "walt.id web wallet"))
        Pair("walt.id", WalletConfiguration("walt.id", "http://localhost:3000", "api/wallet/siopv2/initPresentation", "pi/wallet/siopv2/initPassiveIssuancea" , "walt.id web wallet"))
      )
    }
  }
}
