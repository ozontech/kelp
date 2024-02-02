package ru.ozon.ideplugin.kelp

/**
 * @return true, if the property with [propertyName] MUST be rendered with an icon
 */
internal fun filterDsIconProperty(
    propertyNameFilter: KelpConfig.IconsRendering.IconPropertyNameFilter?,
    propertyName: String,
): Boolean {
    val startsWithFilterPassed = propertyNameFilter?.startsWith
        ?.any { prefix -> propertyName.startsWith(prefix) } != false

    val doesNotStartWithFilterPassed = propertyNameFilter?.doesNotStartWith
        ?.all { prefix -> !propertyName.startsWith(prefix) } != false

    return startsWithFilterPassed && doesNotStartWithFilterPassed
}

/**
 * Converts the property name of the ds icons class to the actual drawable resource name
 */
internal fun getDsIconResourceName(
    mapper: KelpConfig.IconsRendering.PropertyToResourceMapper?,
    propertyName: String,
): String {
    val prefix = mapper?.addPrefix ?: ""
    val resourceName = prefix + if (mapper?.convertToSnakeCase == true) {
        propertyName.camelToSnakeCase()
    } else {
        propertyName
    }
    return resourceName
}
