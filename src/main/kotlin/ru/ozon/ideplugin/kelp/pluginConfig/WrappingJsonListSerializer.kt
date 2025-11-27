package ru.ozon.ideplugin.kelp.pluginConfig

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.serializer

object WrappingDemoJetSerializer : KSerializer<List<KelpConfig.DemoJetStubGeneration>> by wrappingJsonListSerializer()
object WrappingIconsRenderingSerializer : KSerializer<List<KelpConfig.IconsRendering>> by wrappingJsonListSerializer()
object WrappingFunctionFilterSerializer : KSerializer<List<KelpConfig.FunctionFilter>> by wrappingJsonListSerializer()

/** Wraps a single object to a list */
private inline fun <reified T> wrappingJsonListSerializer() = object : JsonTransformingSerializer<T>(serializer<T>()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonObject) return JsonArray(listOf(element))
        return element
    }
}