package helpers


// import java.nio.file.Path

//import org.json.XML
//import org.json.JSONObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import id.walt.services.did.DidService
import id.walt.vclib.credentials.VerifiableId
import id.walt.webwallet.backend.auth.JWTService
import id.walt.webwallet.backend.auth.UserData
import id.walt.webwallet.backend.auth.UserInfo
import okhttp3.internal.toImmutableList

import org.json.JSONException
import org.json.JSONObject
import org.json.XML
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.Path

class StudentId()
{
    val familyName: String = "Doe"
    val firstName: String = "John"
    val dateOfBirth: String = "1901-01-01"
    val personalIdentifier: String = "STUD123456"
    val nameAndFamilyNameAtBirth: String = "John Doe"
    val placeOfBirth: String = ""
    val currentAddress: String = ""
    val gender: String = ""
}
class Diploma {

    //asmuo :

    val firstName : String = "Jonas"
    val lastName : String = "Ponas"
    val birthDay : String = "1901-01-01"
    val sex : String = "Man"

    val programCode : String = "6121AX005"
    val programName : String = "Taikomoji matematika"
//    val blankoKodas : String = "6115"
//    val serija : String = "123456"

}


class StudentCredentialsGenerator {
    internal val WALTID_DATA_ROOT = System.getenv("WALTID_DATA_ROOT") ?: "."

    companion object{
        @JvmStatic
        fun getStudentIdCredential(user: String): VerifiableId.VerifiableIdSubject
        {

            // val stud: StudentId? =
            //     Klaxon().parse<StudentId>(Path(WALTID_DATA_ROOT, user, "MUCredentials", "studentid").toFile())
            // fun relativePath = File.relativeTo(base: File) : File
            // Log.i(TAG, "Hello World")
            // Log.i(TAG, relativePath)

            //println(Paths.get("").toAbsolutePath().toString() )
            // val pathToStudCred = File.relativeTo(studentid)
            val path = Paths.get("").toAbsolutePath().toString()
            // File("").walkTopDown().forEach { println(it) }
            // File("/tmp").list().forEach { println(it) }
            println("tekstas")
            println(path)


            //XML

            //println(pathToStudCred.toString())
            // println(Paths.get("").toAbsolutePath().toString() )

            val stud: StudentId = Klaxon().parse<StudentId>(Path("C:\\Users\\jonsei\\Desktop\\dltnode-waltid-wallet-backend\\data\\jonas@sks.lt\\MUCredentials\\studentid").toFile()) ?: StudentId()
            println("student: \n")
            println(stud)
                // Klaxon().parse<StudentId>(Path("/data/jonas@sks.lt/JonoK
                // odasBack/MUCredentials/studentid").toFile()) ?: StudentId()

                
               //Klaxon().parse<StudentId>(Path("/mnt/c/Users/jonsei/Desktop").toFile()) ?: StudentId()
                //Klaxon().parse<StudentId>(Path("C:\\Users\\jonsei\\Desktop\\studentid").toFile()) ?: StudentId()
                //Klaxon().parse<StudentId>(Path("C:\\Users\\JonasZalinkevicius\\source\\kotlin\\EBSI\\waltid-wallet-backend\\data\\jonas@sks.lt\\MUCredentials\\studentid").toFile()) ?: StudentId()
               
                //Klaxon().parse<StudentId>(Path(relativePath).toFile()) ?: StudentId()

            val address = listOf(stud.currentAddress)

            val services = DidService.listDids()
            println(services)
            return  VerifiableId.VerifiableIdSubject()
//            return VerifiableId.VerifiableIdSubject(
//                DidService.listDids().firstOrNull() ?: DidService.create(
//                    DidMethod.key), null, stud.familyName,  stud.firstName, stud.dateOfBirth, stud.personalIdentifier, stud.nameAndFamilyNameAtBirth, stud.placeOfBirth, address, stud.gender)
        }

        fun readUserData(path : String) : List<UserData>{

            var userDataList = mutableListOf<UserData>()

            val xmlIdData = File(path).readText(charset = Charsets.UTF_8)

            val file = File(path)
            try {
                BufferedReader(FileReader(file)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        println(line)
                        var lines = line!!.split(" ")
                        println("lines:")
                        println(lines)
                        var user = UserData(lines[0],lines[1],lines[2])
                        println("user:")
                        println(user)
                        userDataList.add(user)

                    }
                    println("userDataList")
                    println(userDataList)

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return userDataList.toImmutableList()
        }

        @JvmStatic
        fun getStudentDiplomaXml(user : UserInfo) : JSONObject?{
           //val xmlIdPath = "C:\\Users\\jonsei\\Documents\\WalletTamper\\WalletBackEndTamper\\waltid-wallet-backend\\data\\Documents\\diplomas.xml"
            val path = Paths.get("").toAbsolutePath().toString() + "/data/Logins/logins.txt"
            var userDataList = readUserData(path)

            println("userdataList: \n")
            println(userDataList)
            var pathToDiploma  = Paths.get("").toAbsolutePath().toString() + "/data/Documents/"
            for(userFromList in userDataList){

                if(user.email == userFromList.email){
                    pathToDiploma += userFromList.diplomaName
                    break
                }
            }



            println("sadsasadasddsa")
//            val path = Paths.get("").toAbsolutePath().toString() + "/data/Documents/" + diplomaFile
            println("path:")
            println(path)
            println("pathtoDiploma:")
            println(pathToDiploma)
            val xmlIdData = File(pathToDiploma).readText(charset = Charsets.UTF_8)

            println("xmldIdData: \n  ")
            println(xmlIdData)

            var jsonObj: JSONObject? = null
            try {
                jsonObj = XML.toJSONObject(xmlIdData)
            } catch(e : JSONException) {
                throw  RuntimeException(e);
            }
            println("xmlstring: ")
            println(xmlIdData)
            println("jsonObj: ")
            println(jsonObj)

            println("print json duomenys pazymejimas isslavinimo pavadinimas")
            println((jsonObj.getJSONObject("duomenys").getJSONObject("pazymejimas").getString("issilavinimoPavadinimas")))

            return jsonObj


        }

//        fun readXml(): Document {
//            val xmlFile = File("./input/items.xml")
//
//            val dbFactory = DocumentBuilderFactory.newInstance()
//            val dBuilder = dbFactory.newDocumentBuilder()
//            val xmlInput = InputSource(StringReader(xmlFile.readText()))
//            val doc = dBuilder.parse(xmlInput)
//
//            return doc
//        }
//        fun getStudentDiploma(user: String): VerifiableId.VerifiableIdSubject
//        {
//
//
//
//
////            val fileName = "C:\\Users\\jonsei\\Desktop\\dltnode-waltid-wallet-backend\\data\\DigitalFiles\\DigitalDiplomas\\diplomasInfo";
////
////
////            val xmlString = File(fileName).readText(Charsets.UTF_8)
////
////            println("xml string :")
////            println(xmlString)
////
////            val jsonObj = XML.toJSONObject(xmlString)
////            val jsonPrettyPrintString = jsonObj.toString()
////
////            println(jsonPrettyPrintString)
//
//
//
//            // val stud: StudentId? =
//            //     Klaxon().parse<StudentId>(Path(WALTID_DATA_ROOT, user, "MUCredentials", "studentid").toFile())
//            // fun relativePath = File.relativeTo(base: File) : File
//            // Log.i(TAG, "Hello World")
//            // Log.i(TAG, relativePath)
//
//            //println(Paths.get("").toAbsolutePath().toString() )
//            // val pathToStudCred = File.relativeTo(studentid)
//            //val path = Paths.get("").toAbsolutePath().toString()
//            // File("").walkTopDown().forEach { println(it) }
//            // File("/tmp").list().forEach { println(it) }
//            //println("tekstas")
//            //println(path)
//
//            //println(pathToStudCred.toString())
//            // println(Paths.get("").toAbsolutePath().toString() )
//
//            val diploma: Diploma = Klaxon().parse<Diploma>(Path("C:\\Users\\jonsei\\Desktop\\dltnode-waltid-wallet-backend\\data\\DigitalFiles\\DigitalDiplomas\\diplomasJSONTest").toFile()) ?: Diploma()
//
//            println("diplomas:")
//            println(diploma.firstName + diploma.programName)
//
//            // Klaxon().parse<StudentId>(Path("/data/jonas@sks.lt/JonoK
//            // odasBack/MUCredentials/studentid").toFile()) ?: StudentId()
//
//
//            //Klaxon().parse<StudentId>(Path("/mnt/c/Users/jonsei/Desktop").toFile()) ?: StudentId()
//            //Klaxon().parse<StudentId>(Path("/mnt/c/Users/jonsei/Desktop").toFile()) ?: StudentId()
//            //Klaxon().parse<StudentId>(Path("C:\\Users\\jonsei\\Desktop\\studentid").toFile()) ?: StudentId()
//            //Klaxon().parse<StudentId>(Path("C:\\Users\\JonasZalinkevicius\\source\\kotlin\\EBSI\\waltid-wallet-backend\\data\\jonas@sks.lt\\MUCredentials\\studentid").toFile()) ?: StudentId()
//
//            //Klaxon().parse<StudentId>(Path(relativePath).toFile()) ?: StudentId()
//
//
//            //val diplomas = arrayOf<Diploma>()
//
//            return VerifiableId.VerifiableIdSubject(
//                DidService.listDids().firstOrNull() ?: DidService.create(
//                    DidMethod.key), diploma.firstName, diploma.lastName, diploma.birthDay, diploma.sex, diploma.programCode, diploma.programName)
//        }
    }
}