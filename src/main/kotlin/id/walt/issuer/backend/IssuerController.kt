package id.walt.issuer.backend
import com.beust.klaxon.Klaxon
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.http.ServletUtils
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import id.walt.model.dif.CredentialManifest
import id.walt.model.dif.OutputDescriptor
import id.walt.model.oidc.CredentialResponse
import id.walt.model.oidc.klaxon
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.OIDCUtils
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.Proof
import id.walt.vclib.model.toCredential
import id.walt.vclib.registry.VcTypeRegistry
import id.walt.verifier.backend.VerifierConfig
import id.walt.verifier.backend.VerifierController
import id.walt.verifier.backend.WalletConfiguration
import id.walt.webwallet.backend.auth.JWTService
import id.walt.webwallet.backend.auth.UserInfo
import id.walt.webwallet.backend.auth.UserRole
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
//import id.walt.edcl.edclExcel.edclexcel2ebsi.src.main.java.fi.tuni.microblock.edclexcel2ebsi.DiplomaDataProvider
import fi.tuni.microblock.edclexcel2ebsi.DiplomaDataProvider

object IssuerController {
  val routes
    get() =
      path("") {
//        path("jwt"){
//          get("getJwtToken", documented(
//            document().operation {
//              it.summary("Get JWT token")
//                .addTagsItem("Issuer")
//                .operationId("getJwtToken")
//            }
//              .jsonArray<WalletConfiguration>("200"),
//            VerifierController::getJwtToken,
//          ))
//        }
        path("wallets") {
          get("list", documented(
            document().operation {
              it.summary("List wallet configurations")
                .addTagsItem("Issuer")
                .operationId("listWallets")
            }
              .jsonArray<WalletConfiguration>("200"),
            VerifierController::listWallets,
          ))
        }
        path("credentials") {
          get("listIssuables", documented(
            document().operation {
              it.summary("List issuable credentials")
                .addTagsItem("Issuer")
                .operationId("listIssuableCredentials")
            }
              .queryParam<String>("sessionId")
              .json<Issuables>("200"),
            IssuerController::listIssuableCredentials), UserRole.AUTHORIZED)
          path("issuance") {
            post("request", documented(
              document().operation {
                it.summary("Request issuance of selected credentials to wallet")
                  .addTagsItem("Issuer")
                  .operationId("requestIssuance")
              }
                .queryParam<String>("walletId")
                .queryParam<String>("sessionId")
                .body<Issuables>()
                .result<String>("200"),
              IssuerController::requestIssuance
            ), UserRole.AUTHORIZED)
            post("fulfill", documented(
              document().operation {
                it.summary("SIOPv2 issuance fulfillment callback")
                  .addTagsItem("Issuer")
                  .operationId("fulfillIssuance")
              }
                .formParamBody<String> { }
                .jsonArray<String>("200"),
              IssuerController::fulfillIssuance
            ))
          }
        }
        path("oidc") {
          get(".well-known/openid-configuration", documented(
              document().operation {
                it.summary("get OIDC provider meta data")
                  .addTagsItem("Issuer")
                  .operationId("oidcProviderMeta")
              }
                .json<OIDCProviderMetadata>("200"),
              IssuerController::oidcProviderMeta
            ))
          post("nonce", documented(
            document().operation {
              it.summary("get presentation nonce")
                .addTagsItem("Issuer")
                .operationId("nonce")
            }
              .json<NonceResponse>("200"),
            IssuerController::nonce
          ))
          post("par", documented(
            document().operation {
              it.summary("pushed authorization request")
                .addTagsItem("Issuer")
                .operationId("par")
            }
              .formParam<String>("response_type")
              .formParam<String>("client_id")
              .formParam<String>("redirect_uri")
              .formParam<String>("scope")
              .formParam<String>("claims")
              .formParam<String>("state")
              .json<PushedAuthorizationSuccessResponse>("201"),
            IssuerController::par
          ))
          get("fulfillPAR", documented(
            document().operation { it.summary("fulfill PAR").addTagsItem("Issuer").operationId("fulfillPAR") }
              .queryParam<String>("request_uri"),
            IssuerController::fulfillPAR
          ))
          post("token", documented(
            document().operation {
              it.summary("token endpoint")
                .addTagsItem("Issuer")
                .operationId("token")
            }
              .formParam<String>("grant_type")
              .formParam<String>("code")
              .formParam<String>("redirect_uri")
              .formParam<String>("code_verifier")
              .json<OIDCTokenResponse>("200"),
            IssuerController::token
          ))
          post("credential", documented(
            document().operation {
              it.summary("Credential endpoint").operationId("credential").addTagsItem("Issuer")
            }
              .header<String>("Authorization")
              .formParam<String>("format")
              .formParam<String>("type")
              .formParam<String>("did")
              .formParam<Proof>("proof")
              .json<CredentialResponse>("200"),
            IssuerController::credential
          ))
        }
      }

//  fun listIssuableCredentials(ctx: Context) {
//    val userInfo = JWTService.getUserInfo(ctx)
//    if(userInfo == null) {
//      ctx.status(HttpCode.UNAUTHORIZED)
//      return
//    }
//    val sessionId = ctx.queryParam("sessionId")
//    println("session id:")
//    println(sessionId)
//    var claimsEuropass = false
//    var claimsId = false
//    var credentialTypes: List<String?>? = null
//    if ( sessionId != null ) {
//      val session = IssuerManager.getIssuanceSession(sessionId)
//      session!!.user = userInfo!!.id
//      credentialTypes = session!!.credentialClaims.map { it.type }
//      claimsEuropass = session!!.credentialClaims.find { it.type.equals(DiplomaDataProvider.getCredentialSchema()) } != null
//      claimsId = session!!.credentialClaims.find { it.type.equals(DiplomaDataProvider.getIdCredentialSchema()) } != null
//    }
//
//    else {
//      credentialTypes = listOf( DiplomaDataProvider.getIdCredentialSchema(), DiplomaDataProvider.getCredentialSchema())
//    }
//    println("IdCrdentialScheme")
//    println(DiplomaDataProvider.getIdCredentialSchema())
//    credentialTypes = listOf( DiplomaDataProvider.getIdCredentialSchema(), DiplomaDataProvider.getCredentialSchema())
//
//
////    ctx.json(IssuerManager.listIssuableCredentialsFor(userInfo!!.id, credentialTypes))
//    ctx.json(IssuerManager.listIssuableCredentialsFor(userInfo!!.id))
//  }
  fun listIssuableCredentials(ctx: Context) {
    val userInfo = JWTService.getUserInfo(ctx)

    if(userInfo == null) {
      ctx.status(HttpCode.UNAUTHORIZED)
      return
    }
    val sessionId = ctx.queryParam("sessionId")


    println(userInfo)
    if(sessionId == null)
      ctx.json(IssuerManager.listIssuableCredentialsFor(userInfo))
    else
      ctx.json(IssuerManager.getIssuanceSession(sessionId)?.issuables ?: Issuables(credentials = listOf()))
  }



  fun requestIssuance(ctx: Context) {
    val userInfo = JWTService.getUserInfo(ctx)

    if(userInfo == null) {
      ctx.status(HttpCode.UNAUTHORIZED)
      return
    }
    println("userInfo:")
    println(userInfo.toString())
    val wallet = ctx.queryParam("walletId")?.let { IssuerConfig.config.wallets.getOrDefault(it, null) }
    println("wallet:")
    println(wallet)
    val session = ctx.queryParam("sessionId")?.let { IssuerManager.getIssuanceSession(it) }
    println("session:")
    println(session)
    if (wallet == null && session == null) {
      ctx.status(HttpCode.BAD_REQUEST).result("Unknown wallet or session ID given")
      return
    }

    val selectedIssuables = ctx.bodyAsClass<Issuables>()
    println("selectedIssuables:")
    println(selectedIssuables)
    if(selectedIssuables.credentials.isEmpty()) {
      ctx.status(HttpCode.BAD_REQUEST).result("No issuable credential selected")
      return;
    }
    println("ctx body:")
    println(ctx.body())
    if(wallet != null) {

      println("wallet not null")

      //ctx.result("http://localhost:8080/${wallet.receivePath}?${IssuerManager.newSIOPIssuanceRequest(userInfo.id, selectedIssuables).toUriQueryString()}")
      ctx.result(" ${wallet.url}/${wallet.receivePath}?${IssuerManager.newSIOPIssuanceRequest(userInfo.id, selectedIssuables).toUriQueryString()}")
    } else {
      println("wallet null")
      //ctx.result("${session!!.authRequest.redirectionURI}?code=${IssuerManager.updateIssuanceSession(session, selectedIssuables)}&state=${session.authRequest.state.value}")
    }


    //ctx.result(" ${wallet.url}/${wallet.receivePath}?${IssuerManager.newSIOPIssuanceRequest(userInfo.id, selectedIssuables).toUriQueryString()}")
    println("baigem")
  }

  fun fulfillIssuance(ctx: Context) {
    val id_token = ctx.formParam("id_token")
    val vp_token = ctx.formParam("vp_token")?.toCredential() as VerifiablePresentation
    //TODO: verify and parse id token
    val state = ctx.formParam("state") ?: throw BadRequestResponse("No state specified")
    println("fulfillIssuance ctx body: ")
    println(ctx.body())
    ctx.result(
      "[ ${IssuerManager.fulfillIssuanceRequest(state, null, vp_token).joinToString(",") } ]"
    )
  }

  fun oidcProviderMeta(ctx: Context) {
    ctx.json(OIDCProviderMetadata(
      Issuer(IssuerConfig.config.issuerApiUrl),
      listOf(SubjectType.PAIRWISE, SubjectType.PUBLIC),
      URI("http://blank")).apply {
        authorizationEndpointURI = URI("${IssuerConfig.config.issuerApiUrl}/oidc/fulfillPAR")
        pushedAuthorizationRequestEndpointURI = URI("${IssuerConfig.config.issuerApiUrl}/oidc/par")
        tokenEndpointURI = URI("${IssuerConfig.config.issuerApiUrl}/oidc/token")
        setCustomParameter("credential_endpoint", "${IssuerConfig.config.issuerApiUrl}/oidc/credential")
        setCustomParameter("nonce_endpoint", "${IssuerConfig.config.issuerApiUrl}/oidc/nonce")
        setCustomParameter("credential_manifests", listOf(
          CredentialManifest(
            issuer = id.walt.model.dif.Issuer(IssuerManager.issuerDid, IssuerConfig.config.issuerClientName),
            outputDescriptors = listOf(
              OutputDescriptor(
                DiplomaDataProvider.getCredentialType(),
                DiplomaDataProvider.getCredentialSchema(),
                DiplomaDataProvider.getCredentialType()
              ),
              OutputDescriptor(
                DiplomaDataProvider.getIdCredentialType(),
                DiplomaDataProvider.getIdCredentialSchema(),
                DiplomaDataProvider.getIdCredentialType()
              )
            )
          )).map { net.minidev.json.parser.JSONParser().parse(Klaxon().toJsonString(it)) }
        )
    }.toJSONObject())
  }

  fun nonce(ctx: Context) {
    ctx.json(IssuerManager.newNonce())
  }

  fun par(ctx: Context) {
    val userInfo = JWTService.getUserInfo(ctx)
    println( "user " +userInfo?.email )
    val req = AuthorizationRequest.parse(ServletUtils.createHTTPRequest(ctx.req))
    val claims = OIDCUtils.getVCClaims(req)
    if(claims == null || claims.credentials == null) {
      ctx.status(HttpCode.BAD_REQUEST).json(PushedAuthorizationErrorResponse(ErrorObject("400", "No credential claims given", 400)))
      return
    }
    val session = IssuerManager.initializeIssuanceSession(claims.credentials!!, req,userInfo!!)
    ctx.status(HttpCode.CREATED).json(PushedAuthorizationSuccessResponse(URI("urn:ietf:params:oauth:request_uri:${session.id}"), IssuerManager.EXPIRATION_TIME.seconds).toJSONObject())
  }

  fun fulfillPAR(ctx: Context) {
    val parURI = ctx.queryParam("request_uri")!!
    val sessionID = parURI.substringAfterLast("urn:ietf:params:oauth:request_uri:")
    val session = IssuerManager.getIssuanceSession(sessionID)
    if(session != null) {
      ctx.status(HttpCode.FOUND).header("Location", "${IssuerConfig.config.issuerUiUrl}/?sessionId=${session.id}")
    } else {
      ctx.status(HttpCode.FOUND).header("Location", "${IssuerConfig.config.issuerUiUrl}/IssuanceError?message=Invalid issuance session")
    }
  }

  fun token(ctx: Context) {
    val code = ctx.formParam("code")
    if(code == null) {
      ctx.status(HttpCode.BAD_REQUEST).json(TokenErrorResponse(OAuth2Error.INVALID_REQUEST).toJSONObject())
      return
    }
    val session = IssuerManager.getIssuanceSession(code)
    if(session == null) {
      ctx.status(HttpCode.NOT_FOUND).json(TokenErrorResponse(OAuth2Error.INVALID_REQUEST).toJSONObject())
      return
    }

    ctx.json(OIDCTokenResponse(OIDCTokens(JWTService.toJWT(UserInfo(session.id)), BearerAccessToken(session.id), RefreshToken()), mapOf(
      "expires_in" to IssuerManager.EXPIRATION_TIME.seconds,
      "c_nonce" to session.nonce
    )).toJSONObject())
  }

  fun credential(ctx: Context) {
    val format = ctx.formParam("format") ?: "ldp_vc"
    val type = ctx.formParam("type")
    val did = ctx.formParam("did")
    val proof = ctx.formParam("proof")?.let { klaxon.parse<Proof>(it) }
    // TODO: verify proof

    val session = ctx.header("Authorization")?.substringAfterLast("Bearer ")
      ?.let { IssuerManager.getIssuanceSession(it) }
      ?: throw ForbiddenResponse("Invalid or unknown access token")

    if(did.isNullOrEmpty() || type.isNullOrEmpty() || (session.did != null && session.did != did)) {
      throw BadRequestResponse("No type or did given, or invalid did for this session")
    }
    val credential = IssuerManager.fulfillIssuanceSession(session, type, did, format)
    if(credential.isNullOrEmpty()) {
      ctx.status(HttpCode.NOT_FOUND).result("No issuable credential with the given type found")
      return
    }
    ctx.json(CredentialResponse(format, Base64.getUrlEncoder().encodeToString(credential.toByteArray(StandardCharsets.UTF_8))))
  }
}
