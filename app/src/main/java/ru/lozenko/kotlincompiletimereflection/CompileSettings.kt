package ru.lozenko.kotlincompiletimereflection

import ru.lozenko.kotlincompiletimereflection.library.Settings

/**
 * Created by Ivan Lozenko
 *
 *
 */
@Settings("COMPILE_SETTINGS")
interface CompileSettings{

    var setting1: Boolean

    val setting2: Int

    var setting3: Float

    var setting4: Long

    var setting5: String

    var setting6: String?

}