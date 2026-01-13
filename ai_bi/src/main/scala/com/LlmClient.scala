package com

import scala.concurrent.{Future, ExecutionContext}
import java.net.{URL, HttpURLConnection}
import java.nio.charset.StandardCharsets
import java.io.{BufferedWriter, OutputStreamWriter}
import spray.json._
import ModelsJsonProtocol._

/**
 * Minimal LLM client using Java HttpURLConnection + spray-json.
 * Sends a prompt to OpenRouter (OpenRouter API) and returns the assistant text.
 *
 * Requires env var OPENROUTER_API_KEY to be set (or pass apiKey to constructor).
 */
class LlmClient(apiKey: String)(implicit ec: ExecutionContext) {

  private val endpoint = "https://openrouter.ai/api/v1/chat/completions"
  //private val modelId = "meta-llama/llama-3.3-8b-instruct:free"

  private val modelId = "xiaomi/mimo-v2-flash:free"


  //private val endpoint = "https://api.awanllm.com/v1/chat/completions"
  //private val modelId = "Meta-Llama-3.1-70B-Instruct"

  def callLLM(prompt: String, timeoutMs: Int = 30000): Future[String] = Future {
    val conn = new URL(endpoint).openConnection().asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Authorization", s"Bearer $apiKey")
    conn.setConnectTimeout(timeoutMs)
    conn.setReadTimeout(timeoutMs)

    // Build JSON payload using spray-json objects for safety
    val payload = JsObject(
      "model" -> JsString(modelId),
      "messages" -> JsArray(
        JsObject(
          "role" -> JsString("user"),
          "content" -> JsString(prompt)
        )
      )
    ).compactPrint


    val out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream, StandardCharsets.UTF_8))
    out.write(payload)
    out.flush()
    out.close()

    val respCode = conn.getResponseCode
    val respStream = if (respCode >= 200 && respCode < 300) conn.getInputStream else conn.getErrorStream
    val respText = scala.io.Source.fromInputStream(respStream, "UTF-8").mkString
    respStream.close()
    conn.disconnect()
    println("Raw LLM response: " + respText)

    // Parse JSON response using spray-json
    try {
      val js = respText.parseJson.asJsObject
      js.fields.get("choices") match {
        case Some(JsArray(choices)) if choices.nonEmpty =>
          val first = choices.head.asJsObject
          // expected: { "message": { "content": "..." } }
          first.fields.get("message") match {
            case Some(msg: JsObject) =>
              msg.fields.get("content") match {
                case Some(JsString(s)) => s
                case other => respText // fallback
              }
            case _ => respText
          }
        case _ =>
          // return raw response if structure unexpected
          respText
      }
    } catch {
      case ex: Exception =>
        s"Failed to parse LLM response: ${ex.getMessage}\nRaw: $respText"
    }
  }
}
