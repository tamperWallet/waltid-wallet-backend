package id.walt.issuer.backend

import helpers.StudentCredentialsGenerator
import id.walt.model.oidc.CredentialClaim
import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.model.*
import id.walt.vclib.registry.VcTypeRegistry
import id.walt.vclib.templates.VcTemplateManager
import id.walt.webwallet.backend.auth.UserInfo
import org.json.XML

data class IssuableCredential (
  val schemaId: String,
  val type: String,
  val credentialData: Map<String, Any>? = null
) {
  companion object {
    fun fromTemplateId(templateId: String, user : UserInfo): IssuableCredential {
//      var crd : VerifiableId  = VcTemplateManager.loadTemplate(templateId) as VerifiableId
      var crd = VcTemplateManager.loadTemplate(templateId)

      println("template id:")
      println(templateId)
//      val kitasTmpl = VerifiableCredential()
      println("template")
//      println(tmpl)
//      println("template subject:")
      //     crd   = crd as VerifiableId
      if (templateId == "VerifiableId") {
        crd = crd as VerifiableId
        println("credential subject:")
        println(crd.credentialSubject)

        crd.credentialSubject = generateStudentId(crd)

        println("credential subject po pakeitimo :")
        println(crd.credentialSubject)
      }
     else if (templateId == "VerifiableDiploma"){
        crd = crd as VerifiableDiploma
        println("credential subject:")
        println(crd.credentialSubject)
        crd.credentialSubject
        crd.credentialSubject = generateDiploma(crd,user)

        println("credential subject po pakeitimo :")
        println(crd.credentialSubject)
      }

//      crd   = crd as VerifiableId

        return IssuableCredential(
          crd!!.credentialSchema!!.id,
          crd.type.last(),
          mapOf(
            Pair(
              "credentialSubject",
              (crd as AbstractVerifiableCredential<out CredentialSubject>).credentialSubject!!
            )
          )
        )
      }
      fun generateStudentId(cred: VerifiableId): VerifiableId.VerifiableIdSubject? {

//        val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml()
        var changedCred = cred.credentialSubject
        changedCred?.currentAddress = listOf("SomeStreet 28 g.")
        changedCred?.dateOfBirth = "1987-04-05"
        changedCred?.gender = "Male"
        changedCred?.firstName = "fdhgfghgfh"
        changedCred?.familyName = "Jonas"
        changedCred?.placeOfBirth = "Lietuva"
        changedCred?.nameAndFamilyNameAtBirth = "Litva"



        return changedCred

      }

      fun generateDiploma(cred: VerifiableDiploma, user : UserInfo): VerifiableDiploma.VerifiableDiplomaSubject? {



        println("ffff")
        println("ffff")

        println("userInfo")
        println(user)
        println("diplomaFile")

        val jsonDiploma = StudentCredentialsGenerator.getStudentDiplomaXml(user)
        val diplomas = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")

        var changedCred = cred.credentialSubject


        changedCred?.dateOfBirth = jsonDiploma?.getJSONObject("duomenys")?.getJSONObject("pazymejimas")?.getJSONObject("asmuo")?.getString("gimimoData")
        changedCred?.familyName = diplomas?.getJSONObject("asmuo")?.getString("pavarde")
        changedCred?.givenNames = diplomas?.getJSONObject("asmuo")?.getString("vardas")
        changedCred?.identifier = diplomas?.getJSONObject("isdavusiInstitucija")?.getString("institucijosPavadinimas")



        var gradingScheme = VerifiableDiploma.VerifiableDiplomaSubject.GradingScheme(
          id = diplomas?.getJSONArray("dalykas")?.getJSONObject(0)!!.getInt("kodas").toString(),
          title = diplomas?.getJSONArray("dalykas")?.getJSONObject(0)!!.getString("pavadinimas"),
          description = diplomas?.getJSONArray("dalykas")?.getJSONObject(0)!!.getString("ivertinimas")
        )
        changedCred?.gradingScheme = gradingScheme



        var learningAchievement = VerifiableDiploma.VerifiableDiplomaSubject.LearningAchievement(
          diplomas?.getJSONObject("priedas").getString("numeris").toString(),
          diplomas?.getJSONObject("priedas").getString("tipas"),
          diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai"),
          listOf(
            diplomas?.getJSONObject("priedas").getString("kvalifikacijosGalimybes"),
            diplomas?.getJSONObject("priedas").getString("studijuProgramosReikalavimai")
          )

        )
        changedCred?.learningAchievement = learningAchievement


         var awardingOpportunity =  VerifiableDiploma.VerifiableDiplomaSubject.AwardingOpportunity(
            diplomas?.getInt("blankoKodas").toString(),
            diplomas?.getString("programosPavadinimas"),
            VerifiableDiploma.VerifiableDiplomaSubject.AwardingOpportunity.AwardingBody(
              id = diplomas?.getJSONObject("isdavusiInstitucija").getInt("institucijosKodas").toString(),
              eidasLegalIdentifier = null,
              registration = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosPavadinimas"),
              preferredName = diplomas?.getJSONObject("isdavusiInstitucija").getString("institucijosVadovas")
            )
          )
        changedCred?.awardingOpportunity = awardingOpportunity
        return changedCred

      }
    }
  }

  data class Issuables(
    val credentials: List<IssuableCredential>
  ) {
    val credentialsByType
      get() = credentials.associateBy { it.type }
    val credentialsBySchemaId
      get() = credentials.associateBy { it.schemaId }

    companion object {
      fun fromCredentialClaims(credentialClaims: List<CredentialClaim>, user : UserInfo): Issuables {
        return Issuables(
          credentials = credentialClaims.flatMap { claim ->
            VcTypeRegistry.getTypesWithTemplate().values
              .map { it.metadata.template!!() }
              .filter { it.credentialSchema != null }
              .filter {
                (isSchema(claim.type!!) && it.credentialSchema!!.id == claim.type) ||
                        (!isSchema(claim.type!!) && it.type.last() == claim.type)
              }
              .map { it.type.last() }
          }.map { IssuableCredential.fromTemplateId(it,user) }
        )
      }
    }
  }


data class NonceResponse(
    val p_nonce: String,
    val expires_in: String? = null
)

