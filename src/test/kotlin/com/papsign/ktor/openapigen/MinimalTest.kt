package com.papsign.ktor.openapigen

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import installJackson
import installOpenAPI
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.HttpMethod
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.event.Level


const val SOME_PARAM_ROUTE = "/test/{str}"
const val SOME_RESPONSE_VALUE = "test-response"
const val SOME_BODY_VALUE = "test-body"

class MinimalTest {
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

    private val objectMapper = ObjectMapper()
    private val someResponse = SomeResponse(SOME_RESPONSE_VALUE)
    private val expectedResponseJson: String = objectMapper.writeValueAsString(someResponse)
    private val expectedBody = SomeBody(SOME_BODY_VALUE)
    private val someBodyJson: String = objectMapper.writeValueAsString(expectedBody)

    @Test
    fun testPost_bodyType_expectedBodyAndResponse() {
        withTestApplication({
            installModules()
            apiRouting {
                route(SOME_PARAM_ROUTE) {
                    post<StringParam, SomeResponse, SomeBody> {
                            params, body ->
                        println(params)
                        println(body)
                        assertEquals(expectedBody, body)
                        respond(someResponse)
                    }
                }
            }
        }) {
            sendVerifyPost()
        }
    }

    @Test
    fun testPost_noBodyType_expectedBodyAndResponse() {
        withTestApplication({
            installModules()
            apiRouting {
                route(SOME_PARAM_ROUTE) {
                    post<StringParam, SomeResponse, Any> {
                            params, body ->
                        val bodyText = pipeline.call.receiveText()
                        println(params)
                        println(body)
                        println(bodyText)
                        assertEquals(someBodyJson, bodyText)
                        assertEquals(expectedBody, objectMapper.readValue(bodyText, SomeBody::class.java))
//                        assertEquals(expectedBody, body) Always Unit
                        respond(someResponse)
                    }
                }
            }
        }) {
            sendVerifyPost()
        }
    }

    @Test
    fun testPost_ktorRouting_expectedBodyAndResponse() {
        withTestApplication({
            installModules()
            routing {
                route(SOME_PARAM_ROUTE) {
                    post {
                        val bodyText = call.receiveText()
                        println(call.parameters)
                        println(bodyText)
                        assertEquals(someBodyJson, bodyText)
                        assertEquals(expectedBody, objectMapper.readValue(bodyText, SomeBody::class.java))
                        call.respond(someResponse)
                    }
                }
            }
        }) {
            sendVerifyPost()
        }
    }

    private fun TestApplicationEngine.sendVerifyPost() {
        println("send request")
        handleRequest(HttpMethod.Post, "/test/aaa"){
            println("configure body")
            setBody(someBodyJson)
        }.apply {
            println("post request")
            assert(requestHandled)
            assertEquals(expectedResponseJson, response.content)
        }
    }

    private fun Application.installModules() {
        installOpenAPI()
        installJackson()
        install(CallLogging) {
            level = Level.DEBUG
        }
    }
}