package id.walt.issuer.backend

import fi.tuni.microblock.edclexcel2ebsi.CredentialLib
import fi.tuni.microblock.edclexcel2ebsi.DiplomaDataProvider
import com.google.common.cache.CacheBuilder
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.oauth2.sdk.token.AccessTokenType
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.dif.PresentationDefinition
import id.walt.model.oidc.*
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.services.essif.didebsi.DidEbsiService
import id.walt.services.hkvstore.FileSystemHKVStore
import id.walt.services.hkvstore.FilesystemStoreConfig
import id.walt.services.hkvstore.HKVKey
import id.walt.services.key.KeyService
import id.walt.services.keystore.HKVKeyStoreService
import id.walt.services.vcstore.HKVVcStoreService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.credentials.*
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.CredentialSubject
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.templates.VcTemplateManager
import id.walt.verifier.backend.SIOPv2RequestManager
import id.walt.verifier.backend.VerifierConfig
import id.walt.verifier.backend.WalletConfiguration
import id.walt.webwallet.backend.auth.JWTService
import id.walt.webwallet.backend.auth.UserInfo
import id.walt.webwallet.backend.config.WalletConfig
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.WALTID_DATA_ROOT
import id.walt.webwallet.backend.context.UserContext
import id.walt.webwallet.backend.context.WalletContextManager
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

const val URL_PATTERN = "^https?:\\/\\/(?!-.)[^\\s\\/\$.?#].[^\\s]*\$"
fun isSchema(typeOrSchema: String): Boolean {
  return Regex(URL_PATTERN).matches(typeOrSchema)
}

object IssuerManager {

  val issuerContext = UserContext(
    hkvStore = FileSystemHKVStore(FilesystemStoreConfig("$WALTID_DATA_ROOT/data/issuer")),
    keyStore = HKVKeyStoreService(),
    vcStore = HKVVcStoreService()
  )
  val EXPIRATION_TIME: Duration = Duration.ofMinutes(5)
  val reqCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRATION_TIME.seconds, TimeUnit.SECONDS).build<String, IssuanceRequest>()
  val nonceCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRATION_TIME.seconds, TimeUnit.SECONDS).build<String, Boolean>()
  val sessionCache = CacheBuilder.newBuilder().expireAfterAccess(EXPIRATION_TIME.seconds, TimeUnit.SECONDS).build<String, IssuanceSession>()
  lateinit var issuerDid: String;
  lateinit var credentialLib : CredentialLib;

  init {
    WalletContextManager.runWith(issuerContext) {
      credentialLib = CredentialLib()
      //issuerDid = DidService.listDids().firstOrNull() ?: DidService.create(DidMethod.key)
      issuerDid = credentialLib.getIssuerDid()
    }
  }

  fun listIssuableCredentialsFor(user: String): Issuables {
//<<<<<<< HEAD
    return Issuables(
      credentials = credentialLib.listCredentialsForStudent(user)
        .map {
          IssuableCredential(
            DiplomaDataProvider.getCredentialSchema(),
            DiplomaDataProvider.getCredentialType(),
            mapOf( "title" to it )
          )
        }
    )
/*=======
    val credentials = HashMap<String, IssuableCredential>()
    for ( title in credentialLib.listCredentialsForStudent(user) ) {
      val credential = IssuableCredential( type = "VerifiableDiploma", description = title )
      credentials.put( title, credential )
    }

    return Issuables( credentials = credentials )
>>>>>>> master*/
  }

  fun newSIOPIssuanceRequest(user: String, selectedIssuables: Issuables): SIOPv2Request {
    val nonce = UUID.randomUUID().toString()
    val redirectUri = URI.create("${IssuerConfig.config.issuerApiUrl}/credentials/issuance/fulfill")
    val req = SIOPv2Request(
      redirect_uri = redirectUri.toString(),
      response_mode = "post",
      nonce = nonce,
      claims = VCClaims(vp_token = VpTokenClaim(PresentationDefinition(listOf()))),
      state = nonce
    )
    reqCache.put(nonce, IssuanceRequest(user, nonce, selectedIssuables))
    return req
  }

  fun fulfillIssuanceRequest(nonce: String, id_token: IDToken?, vp_token: VerifiablePresentation): List<String> {
    val issuanceReq = reqCache.getIfPresent(nonce);
    if(issuanceReq == null) {
      return listOf()
    }
    // TODO: verify id_token!!
    return WalletContextManager.runWith(issuerContext) {
      if(vp_token.challenge == nonce &&
        // TODO: verify id_token subject
        //id_token.subject == VcUtils.getSubject(vp_token) &&
        // TODO: verify VP signature (import public key for did, currently not supported for did:key!)
        //Auditor.getService().verify(vp_token.encode(), listOf(SignaturePolicy())).overallStatus
        true
      ) {
/*<<<<<<< HEAD
        issuanceReq.selectedIssuables.credentials.map {
          Signatory.getService().issue(it.type,
=======
        issuanceReq.selectedIssuables.credentials.*/
        issuanceReq.selectedIssuables.credentials.map {
          Signatory.getService().issue(it.type,
//>>>>>>> master
            ProofConfig(issuerDid = issuerDid,
              proofType = ProofType.LD_PROOF,
              subjectDid = vp_token.subject),
            dataProvider = credentialLib.createDataProvider( issuanceReq.user, it.credentialData?.get("title") as String? ))
        }
      } else {
        listOf()
      }
    }
  }

  private fun prompt(prompt: String, default: String?): String? {
    print("$prompt [$default]: ")
    val input = readLine()
    return when(input.isNullOrBlank()) {
      true -> default
      else -> input
    }
  }

  fun initializeInteractively() {
    val method = prompt("DID method ('key' or 'ebsi') [key]", "key")
    if(method == "ebsi") {
      val token = prompt("EBSI bearer token: ", null)
      if(token.isNullOrEmpty()) {
        println("EBSI bearer token required, to register EBSI did")
        return
      }
      WalletContextManager.runWith(issuerContext) {
        DidService.listDids().forEach( { ContextManager.hkvStore.delete(HKVKey("did", "created", it)) })
        val key =
          KeyService.getService().listKeys().firstOrNull { k -> k.algorithm == KeyAlgorithm.ECDSA_Secp256k1 }?.keyId
            ?: KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        val did = DidService.create(DidMethod.ebsi, key.id)
        EssifClient.onboard(did, token)
        EssifClient.authApi(did)
        DidEbsiService.getService().registerDid(did, did)
        println("Issuer DID created and registered: $did")
      }
    } else {
      WalletContextManager.runWith(issuerContext) {
        DidService.listDids().forEach( { ContextManager.hkvStore.delete(HKVKey("did", "created", it)) })
        val did = DidService.create(DidMethod.key)
        println("Issuer DID created: $did")
      }
    }
  }

  fun newNonce(): NonceResponse {
    val nonce = UUID.randomUUID().toString()
    nonceCache.put(nonce, true)
    return NonceResponse(nonce, expires_in = EXPIRATION_TIME.seconds.toString())
  }

  fun initializeIssuanceSession(credentialClaims: List<CredentialClaim>, authRequest: AuthorizationRequest): IssuanceSession {
    val id = UUID.randomUUID().toString()
    //TODO: validata/verify PAR request, VP tokens, claims, etc
    val session = IssuanceSession(id, credentialClaims, authRequest, UUID.randomUUID().toString(), Issuables.fromCredentialClaims(credentialClaims))
    sessionCache.put(id, session)
    return session
  }

  fun getIssuanceSession(id: String): IssuanceSession? {
    return sessionCache.getIfPresent(id)
  }

  fun updateIssuanceSession(session: IssuanceSession, issuables: Issuables?): String {
    session.issuables = issuables
    sessionCache.put(session.id, session)
    return session.id
  }

  fun fulfillIssuanceSession(session: IssuanceSession, typeOrSchema: String, did: String, format: String = "ldp_vc"): String? {
    return WalletContextManager.runWith(issuerContext) {
      when(isSchema(typeOrSchema)) {
        true -> session.issuables!!.credentialsBySchemaId[typeOrSchema]
        else -> session.issuables!!.credentialsByType[typeOrSchema]
      }?.let {
          Signatory.getService().issue(it.type,
            ProofConfig(
              issuerDid = issuerDid,
              proofType = when (format) {
                "jwt" -> ProofType.JWT
                else -> ProofType.LD_PROOF
              },
              subjectDid = did
            ),
            dataProvider = it.credentialData?.let { cd -> credentialLib.createDataProvider( session.user, cd.get("title") as String? ) })
        }
    }
  }
}
