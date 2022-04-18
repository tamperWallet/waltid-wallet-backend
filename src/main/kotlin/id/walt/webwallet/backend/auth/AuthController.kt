package id.walt.webwallet.backend.auth

import id.walt.model.DidMethod
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.webwallet.backend.context.WalletContextManager
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import okhttp3.internal.toImmutableList
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Paths

object AuthController {
    val routes
        get() = path("auth") {
            path("login") {
                post(documented(document().operation {
                    it.summary("Login")
                        .operationId("login")
                        .addTagsItem("Authentication")
                }
                    .body<UserInfo> { it.description("Login info") }
                    .json<UserInfo>("200"),
                    AuthController::login), UserRole.UNAUTHORIZED)
            }
            path("userInfo") {
                get(
                    documented(document().operation {
                        it.summary("Get current user info")
                            .operationId("userInfo")
                            .addTagsItem("Authentication")
                    }
                        .json<UserInfo>("200"),
                        AuthController::userInfo), UserRole.AUTHORIZED)
            }
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
    fun login(ctx: Context) {
        var userInfo = ctx.bodyAsClass(UserInfo::class.java)

        val path = Paths.get("").toAbsolutePath().toString() + "/data/Logins/logins.txt"

        var userDataList = readUserData(path)



//        val xmlIdData = File(path).readText(charset = Charsets.UTF_8)
//
//        val file = File("/home/data/file.txt")
//        try {
//            BufferedReader(FileReader(file)).use { br ->
//                var line: String?
//                while (br.readLine().also { line = it } != null) {
//                    println(line)
//                }
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
        println("xmlIdData")

//        C:\Users\jonsei\Documents\WalletTamper\WalletBackEndTamper\waltid-wallet-backend\data\Documents
        println("userInfo:")
        println(userInfo)
        // TODO: verify login credentials!!

        println("userDataList")
        println(userDataList)

        var authentificated = false
        for(user in userDataList){
            if(user.email == userInfo.email && user.password == userInfo.password){
                userInfo.diplomaFile = user.diplomaName
                authentificated = true
                println("duomenys prisijungimo geri")
                ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
                    if(DidService.listDids().isEmpty()) {
                        DidService.create(DidMethod.key)
                    }
                }

                ctx.json(UserInfo(userInfo.id).apply {
                    token = JWTService.toJWT(userInfo)

                   }   )


                //var jsonString = "{${UserInfo(userInfo.id).apply { token = JWTService.toJWT(userInfo) } }, diplomaFile: ${user.diplomaName}}"
//                var jsonString = "{ token : \"${JWTService.toJWT(userInfo)}\", diplomaFile: \"${user.diplomaName}\" }"
//
//                var jsonObject = JSONObject(jsonString)
//                println(jsonString)
//                ctx.json(jsonObject)
                return

            }
        }
        println("prisijungimo duomenys blogi")
//        userDataList.any { user -> (user.email == userInfo.email) && (user.password == userInfo.password) }
//        if(authentificated)
//        {
//
//            println("duomenys prisijungimo geri")
//            ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
//            if(DidService.listDids().isEmpty()) {
//                DidService.create(DidMethod.key)
//                }
//            }
//
//            ctx.json(UserInfo(userInfo.id).apply {
//                token = JWTService.toJWT(userInfo)
//            })
//        }
//        else {
//            println("prisijungimo duomenys blogi")
//            return
//        }

//        ContextManager.runWith(WalletContextManager.getUserContext(userInfo)) {
//            if(DidService.listDids().isEmpty()) {
//                DidService.create(DidMethod.key)
//            }
//        }

//        if(DidService.listDids().isNotEmpty()){
//            println("listDids: \n")
//            var did = DidService.listDids()[0]
//            println(DidService.listDids())
//            var authentificMethod = DidService.getAuthenticationMethods(did)
//            println("authentific method: ")
//            println(authentificMethod)
//        }


//        ctx.json(UserInfo(userInfo.id).apply {
//            token = JWTService.toJWT(userInfo)
//        })
        println("bimbam")
    }

    fun userInfo(ctx: Context) {
        println("ctx: ")
        println(ctx.body())
        ctx.json(JWTService.getUserInfo(ctx)!!)
        println("ctx json: ")
        println(ctx.body())

    }
}
