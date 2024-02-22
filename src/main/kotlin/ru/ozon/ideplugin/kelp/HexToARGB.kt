package ru.ozon.ideplugin.kelp

internal fun hexToARGB(colorString: String): Int = runCatching {
    // Use a long to avoid rollovers on #ffXXXXXX
    var color = colorString.toLong(16)
    if (colorString.length == 6) {
        // Set the alpha value
        color = color or 0x00000000ff000000L
    } else {
        require(colorString.length == 8) { "Unknown color" }
    }
    return color.toInt()
}.getOrElse {
    throw IllegalArgumentException("Invalid color: \"$colorString\"", it)
}
