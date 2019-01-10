package ru.lozenko.kotlincompiletimereflection.library

/**
 * Created by Ivan Lozenko
 *
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Settings(val name: String)