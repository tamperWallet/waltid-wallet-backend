package id.walt.onboarding.backend

import com.beust.klaxon.Klaxon
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import id.walt.auditor.Auditor
import id.walt.auditor.ChallengePolicy
import id.walt.auditor.SignaturePolicy
import id.walt.issuer.backend.*
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.model.dif.*
import id.walt.services.context.ContextManager
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.OIDCUtils
import id.walt.webwallet.backend.auth.JWTService
import id.walt.webwallet.backend.auth.UserInfo
import id.walt.webwallet.backend.auth.UserRole
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.*
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import java.net.URI

data class GenerateDomainVerificationCodeRequest(val domain: String)
data class CheckDomainVerificationCodeRequest(val domain: String)
data class IssueParticipantCredentialRequest(val domain: String)

object OnboardingController {
    val routes
        get() =
            path("") {
                path("domain") {
                    path("generateDomainVerificationCode") {
                        post("", documented(
                            document().operation {
                                it.summary("Generate domain verification code")
                                    .addTagsItem("Onboarding")
                                    .operationId("generateDomainVerificationCode")
                            }
                                .body<GenerateDomainVerificationCodeRequest>()
                                .result<String>("200"),
                            OnboardingController::generateDomainVerificationCode
                        ), UserRole.AUTHORIZED)
                    }
                    path("checkDomainVerificationCode") {
                        post("", documented(
                            document().operation {
                                it.summary("Check domain verification code")
                                    .addTagsItem("Onboarding")
                                    .operationId("checkDomainVerificationCode")
                            }
                                .body<CheckDomainVerificationCodeRequest>()
                                .result<Boolean>("200"),
                            OnboardingController::checkDomainVerificationCode
                        ), UserRole.AUTHORIZED)
                    }
                }
                // provide customized oidc discovery document and authorize endpoint
                path("oidc") {
                   get(".well-known/openid-configuration", documented(
                        document().operation {
                            it.summary("get OIDC provider meta data")
                                .addTagsItem("Onboarding")
                                .operationId("ob-oidcProviderMeta")
                        }
                            .json<OIDCProviderMetadata>("200"),
                        OnboardingController::oidcProviderMeta
                    ))
                    get("fulfillPAR", documented(
                        document().operation {
                            it.summary("fulfill PAR").addTagsItem("Issuer").operationId("fulfillPAR") }
                            .queryParam<String>("request_uri"),
                        OnboardingController::fulfillPAR
                    ))
                }
                path("auth") {
                    get("userToken", documented(document().operation {
                        it.summary("get user token") }.json<UserInfo>("200"),
                        OnboardingController::userToken))
                }
                post("issue", documented(document().operation {
                    it.summary("Issue participant credential to did:web-authorized user").addTagsItem("Onboarding") }
                    .queryParam<String>("sessionId").body<IssueParticipantCredentialRequest>(), OnboardingController::issue), UserRole.AUTHORIZED)
            }

    private fun generateDomainVerificationCode(ctx: Context) {
        val did = checkAuthDid(ctx) ?: return
        val domainReq = ctx.bodyAsClass<GenerateDomainVerificationCodeRequest>()
        ctx.result(DomainOwnershipService.generateWaltIdDomainVerificationCode(domainReq.domain, did))
    }

    private fun checkDomainVerificationCode(ctx: Context) {
        val did = checkAuthDid(ctx) ?: return
        val domainReq = ctx.bodyAsClass<CheckDomainVerificationCodeRequest>()
        ctx.json(DomainOwnershipService.checkWaltIdDomainVerificationCode(domainReq.domain, did))
    }

    private fun checkAuthDid(ctx: Context): String? {
        val userInfo = JWTService.getUserInfo(ctx)
        if (userInfo == null) {
            ctx.status(HttpCode.UNAUTHORIZED)
            return null
        } else if (userInfo.did == null) {
            ctx.result("An authenticated DID is required for accessing this API")
            ctx.status(HttpCode.UNAUTHORIZED)
            return null
        }
        return userInfo.did!!
    }

    const val PARICIPANT_CREDENTIAL_SCHEMA_ID = "https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/ParticipantCredential.json"

    private fun oidcProviderMeta(ctx: Context) {
        ctx.json(OIDCProviderMetadata(
            Issuer(IssuerConfig.config.onboardingApiUrl),
            listOf(SubjectType.PAIRWISE, SubjectType.PUBLIC),
            URI("http://blank")
        ).apply {
            authorizationEndpointURI = URI("${IssuerConfig.config.onboardingApiUrl}/oidc/fulfillPAR")
            pushedAuthorizationRequestEndpointURI = URI("${IssuerConfig.config.issuerApiUrl}/oidc/par") // keep issuer-api
            tokenEndpointURI = URI("${IssuerConfig.config.issuerApiUrl}/oidc/token") // keep issuer-api
            setCustomParameter("credential_endpoint", "${IssuerConfig.config.issuerApiUrl}/oidc/credential") // keep issuer-api
            setCustomParameter("nonce_endpoint", "${IssuerConfig.config.issuerApiUrl}/oidc/nonce") // keep issuer-api
            setCustomParameter("credential_manifests", listOf(
                CredentialManifest(
                    issuer = id.walt.model.dif.Issuer(IssuerManager.issuerDid, IssuerConfig.config.issuerClientName),
                    outputDescriptors = listOf(
                        OutputDescriptor(
                                    "ParticipantCredential",
                                PARICIPANT_CREDENTIAL_SCHEMA_ID,
                                "ParticipantCredential")
                    ),
                    presentationDefinition = PresentationDefinition(listOf()) // Request empty presentation to be sent along with issuance request
                )).map { net.minidev.json.parser.JSONParser().parse(Klaxon().toJsonString(it)) }
            )
        }.toJSONObject())
    }

    fun fulfillPAR(ctx: Context) {
        try {
            val parURI = ctx.queryParam("request_uri") ?: throw BadRequestResponse("no request_uri specified")
            val sessionID = parURI.substringAfterLast("urn:ietf:params:oauth:request_uri:")
            val session = IssuerManager.getIssuanceSession(sessionID)
                ?: throw BadRequestResponse("No session found for given sessionId, or session expired")
            // TODO: verify VP from auth request claims
            val vcclaims = OIDCUtils.getVCClaims(session.authRequest)
            val credClaim =
                vcclaims.credentials?.filter { cred -> cred.type == PARICIPANT_CREDENTIAL_SCHEMA_ID }?.firstOrNull()
                    ?: throw BadRequestResponse("No participant credential claim found in authorization request")
            val vp_token =
                credClaim.vp_token
                ?: session.authRequest.customParameters["vp_token"]?.flatMap {
                    OIDCUtils.fromVpToken(it) ?: listOf()
                } ?: listOf()
            val vp = vp_token.firstOrNull() ?: throw BadRequestResponse("No VP token found on authorization request")
            val subject = ContextManager.runWith(IssuerManager.issuerContext) {
                val verificationResult = Auditor.getService().verify(vp.encode(), listOf(SignaturePolicy(), ChallengePolicy(IssuerManager::checkNonce)))
                if(!verificationResult.valid) {
                    throw BadRequestResponse("Invalid VP token given, signature (${verificationResult.policyResults["SignaturePolicy"]}) and/or challenge (${verificationResult.policyResults["ChallengePolicy"]}) could not be verified")
                }
                vp.subject
            }

            if(subject?.let { DidUrl.from(it).method } != DidMethod.web.name) throw BadRequestResponse("did:web is required for onboarding!")

            session.did = subject
            IssuerManager.updateIssuanceSession(session, session.issuables)

            val access_token = ContextManager.runWith(IssuerManager.issuerContext) {
                JwtService.getService().sign(IssuerManager.issuerDid, JWTClaimsSet.Builder().subject(session.id).build().toString())
            }

            ctx.status(HttpCode.FOUND)
                .header("Location", "${IssuerConfig.config.onboardingUiUrl}?access_token=${access_token}&sessionId=${session.id}")
        } catch (exc: Exception) {
            ctx.status(HttpCode.FOUND).header(
                "Location",
                "${IssuerConfig.config.issuerUiUrl}/IssuanceError/?message=${exc.message}"
            )
        }
    }

    fun userToken(ctx: Context) {
        val accessToken = ctx.header("Authorization")?.let { it.substringAfterLast("Bearer ") } ?: throw UnauthorizedResponse("No valid access token set on request")
        val sessionId = ContextManager.runWith(IssuerManager.issuerContext) {
            if(JwtService.getService().verify(accessToken)) {
                JwtService.getService().parseClaims(accessToken)!!["sub"].toString()
            } else {
                null
            }
        }
        val session = sessionId?.let{ IssuerManager.getIssuanceSession(it) } ?: throw UnauthorizedResponse("Invalid access token or session expired")
        val did = session.did ?: throw ForbiddenResponse("No DID specified on current session")

        val userInfo = UserInfo(did)
        ctx.json(userInfo.apply { token = JWTService.toJWT(userInfo) })
    }

    fun issue(ctx: Context) {
        val userInfo = JWTService.getUserInfo(ctx) ?: throw UnauthorizedResponse()
        if(userInfo.did?.let { DidUrl.from(it).method } != DidMethod.web.name) {
            throw BadRequestResponse("User is not did:web-authorized")
        }
        val session = ctx.queryParam("sessionId")?.let{ IssuerManager.getIssuanceSession(it) } ?: throw BadRequestResponse("Session expired or not found")
        if(userInfo.did != session.did) {
            throw BadRequestResponse("Session DID not matching authorized DID")
        }
        // Use the following if we should rely on the domain used in the did:web
        // val domain = DidUrl.from(userInfo.did!!).identifier.substringBefore(":").let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val domain = ctx.bodyAsClass<IssueParticipantCredentialRequest>().domain
        val selectedIssuables = Issuables(
            credentials = listOf(
                IssuableCredential(
                    schemaId = PARICIPANT_CREDENTIAL_SCHEMA_ID,
                    type = "ParticipantCredential",
                    credentialData = mapOf(
                        "credentialSubject" to mapOf(
                            "domain" to domain
                        )
                    )
                )
            )
        )
        ctx.result("${session!!.authRequest.redirectionURI}?code=${IssuerManager.updateIssuanceSession(session, selectedIssuables)}&state=${session.authRequest.state.value}")
    }
}
