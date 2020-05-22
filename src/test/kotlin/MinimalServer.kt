import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.*
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level
import kotlin.reflect.KType

object MinimalServer {
    data class SomeResponse(val value: String)
    data class SomeBody @JsonCreator constructor(@JsonProperty("value") var value: String) {
        override fun equals(other: Any?): Boolean {
            val otherBody = other as? SomeBody ?: return false
            return otherBody.value == value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }
    data class StringParam(@PathParam("a simple string") val str: String)
    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, 8080, "localhost") {
            //define basic OpenAPI info
            install(OpenAPIGen) {
                info {
                    version = "0.1"
                    title = "Test API"
                    description = "The Test API"
                    contact {
                        name = "Support"
                        email = "support@test.com"
                    }
                }
                server("http://localhost:8080/") {
                    description = "Test server"
                }
                replaceModule(DefaultSchemaNamer, object: SchemaNamer {
                    val regex = Regex("[A-Za-z0-9_.]+")
                    override fun get(type: KType): String {
                        return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
                    }
                })
            }

            install(ContentNegotiation) {
                jackson()
            }
            install(CallLogging) {
                level = Level.DEBUG
            }

            routing {
                route("/ktor/{str}") {
                    post {
                        println(call.parameters)
                        val bodyText = call.receiveText()
                        println(bodyText)
                        val objectMapper = ObjectMapper()
                        withContext(Dispatchers.IO) {
                            println(objectMapper.readValue(bodyText, SomeBody::class.java).value)
                        }
                        call.respond(SomeResponse("hello"))
                    }
                }
            }

            apiRouting {
                route("/openapi/any-body/{str}") {
                    post<StringParam, SomeResponse, Any> {
                            params, body ->
                        println(params)
                        val bodyText = pipeline.call.receiveText()
                        println(bodyText)
                        println(body) // Always Unit
                        val objectMapper = ObjectMapper()
                        withContext(Dispatchers.IO) {
                            println(objectMapper.readValue(bodyText, SomeBody::class.java).value)
                        }
                        respond(SomeResponse("hello"))
                    }
                }

                route("/openapi/typed-body/{str}") {
                    post<StringParam, SomeResponse, SomeBody> {
                            params, body ->
                        println(params)
//                        All ready received
//                        val bodyText = pipeline.call.receiveText()
//                        println(bodyText)
                        println(body)
                        println(body.value)
                        respond(SomeResponse("hello"))
                    }
                }
            }
        }.start(true)
    }
}
