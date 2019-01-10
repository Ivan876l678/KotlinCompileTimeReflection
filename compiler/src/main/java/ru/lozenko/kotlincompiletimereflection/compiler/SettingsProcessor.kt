package ru.lozenko.kotlincompiletimereflection.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.classKind
import me.eugeniomarletti.kotlin.metadata.isVar
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import ru.lozenko.kotlincompiletimereflection.library.Settings
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.KClass

/**
 * Created by Ivan Lozenko
 *
 *
 */
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(SettingsProcessor.OUTPUT_DIR)
@SupportedAnnotationTypes("ru.lozenko.kotlincompiletimereflection.library.Settings")
class SettingsProcessor: AbstractProcessor(){

    companion object {
        const val OUTPUT_DIR = "kapt.kotlin.generated"
    }

    override fun process(elements: MutableSet<out TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        if (elements.isEmpty()) return false
        val kotlinGeneratedDirString = processingEnv.options[OUTPUT_DIR]?.let { File(it) } ?: run {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }
        val types = roundEnvironment.getElementsAnnotatedWith(Settings::class.java)
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Processing ${types.size} elements")
        types.forEach { it ->
            val metadata: KotlinClassMetadata = it.kotlinMetadata as? KotlinClassMetadata ?: throw Throwable("${it.simpleName} must be kotlin file")
            val annotation: Settings = it.getAnnotation(Settings::class.java)
            processMetadata(metadata, it as TypeElement, annotation, kotlinGeneratedDirString)
        }
        return true
    }

    private fun processMetadata(metadata: KotlinClassMetadata, element: TypeElement, annotation: Settings, generatedDir: File){
        val data = metadata.data
        val nameResolver = data.nameResolver
        val proto = data.classProto
        val settingsName = annotation.name
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Processing ${element.simpleName}")
        if (settingsName.isBlank()) throw Exception("${element.simpleName} settings name must not be blank")
        if (proto.classKind != ProtoBuf.Class.Kind.INTERFACE) throw Exception("${nameResolver.getString(proto.fqName)} must be interface")

        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
        val simpleName = element.simpleName.toString()
        val settingsClass = ClassName(packageName, "${simpleName}Impl")
        val contextClass = ClassName("android.content", "Context")
        val sharedPreferencesClass = ClassName("android.content", "SharedPreferences")
        val fileSpecBuilder = FileSpec.builder(settingsClass.packageName, settingsClass.simpleName)
        val typeBuilder = TypeSpec.classBuilder(settingsClass.simpleName)
            .addSuperinterface(element.asClassName())
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("context", contextClass)
                .build())
            .addProperty(PropertySpec.builder("sharedPreferences", sharedPreferencesClass, KModifier.PRIVATE)
                .mutable(false)
                .initializer(CodeBlock.of("context.getSharedPreferences(\"$settingsName\", %T.MODE_PRIVATE)", contextClass))
                .build())
        addFields(nameResolver, proto, typeBuilder)
        fileSpecBuilder.addType(typeBuilder.build())
        fileSpecBuilder.build().writeTo(generatedDir)

        //generating extension
        FileSpec.builder(settingsClass.packageName, "${simpleName}Extension")
            .addProperty(PropertySpec.builder(element.simpleName.toString().decapitalize(), element.asClassName())
                .mutable(false)
                .receiver(contextClass)
                .getter(FunSpec.getterBuilder()
                    .addCode("return %T(this)", settingsClass)
                    .build())
                .build())
            .build().writeTo(generatedDir)
    }

    private fun addFields(nameResolver: me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver, classProto: ProtoBuf.Class, typeBuilder: TypeSpec.Builder){
        classProto.propertyList.forEach {
            val fieldName = nameResolver.getString(it.name)
            val returnTypeName = nameResolver.getQualifiedClassName(it.returnType.className).replace("/", ".")
            val nullableType = it.returnType.nullable
            val mutableField = it.isVar
            val returnType: KClass<*> = when(returnTypeName){
                Int::class.qualifiedName -> if (nullableType) throw Exception("Unsupported type of $fieldName") else Int::class
                Long::class.qualifiedName -> if (nullableType) throw Exception("Unsupported type of $fieldName") else Long::class
                Float::class.qualifiedName -> if (nullableType) throw Exception("Unsupported type of $fieldName") else Float::class
                Double::class.qualifiedName -> if (nullableType) throw Exception("Unsupported type of $fieldName") else Double::class
                Boolean::class.qualifiedName -> if (nullableType) throw Exception("Unsupported type of $fieldName") else Boolean::class
                String::class.qualifiedName -> String::class
                else -> throw Exception("Unsupported type of $fieldName")
            }
            val poetReturnType = returnType.asTypeName().copy(nullableType)
            val propertyBuilder = PropertySpec.builder(fieldName, poetReturnType, KModifier.PUBLIC, KModifier.OVERRIDE)
                .mutable(mutableField)
                .getter(FunSpec.getterBuilder().addCode("return ${getterCodeForType(returnType, fieldName, nullableType)}\n").build())
            if (mutableField){
                propertyBuilder.setter(FunSpec.setterBuilder()
                    .addParameter("value", poetReturnType)
                    .addCode(setterCodeForType(returnType, fieldName)).build())
            }
            typeBuilder.addProperty(propertyBuilder.build())
        }
    }

    private fun getterCodeForType(kotlinClass: KClass<*>, fieldName: String, nullable: Boolean): String{
        return when(kotlinClass){
            Boolean::class -> "sharedPreferences.getBoolean(\"$fieldName\", false)"
            Int::class -> "sharedPreferences.getInt(\"$fieldName\", -1)"
            Long::class -> "sharedPreferences.getLong(\"$fieldName\", -1L)"
            Float::class -> "sharedPreferences.getFloat(\"$fieldName\", -1.0f)"
            String::class -> "sharedPreferences.getString(\"$fieldName\", ${if (nullable) "null)" else "\"\") ?: \"\""}"
            else -> throw Exception("Unsupported type of $fieldName")
        }
    }

    private fun setterCodeForType(kotlinClass: KClass<*>, fieldName: String): String{
        return when(kotlinClass){
            Boolean::class -> "sharedPreferences.edit().putBoolean(\"$fieldName\", value).apply()"
            Int::class -> "sharedPreferences.edit().putInt(\"$fieldName\", value).apply()"
            Long::class -> "sharedPreferences.edit().putLong(\"$fieldName\", value).apply()"
            Float::class -> "sharedPreferences.edit().putFloat(\"$fieldName\", value).apply()"
            String::class -> "sharedPreferences.edit().putString(\"$fieldName\", value).apply()"
            else -> throw Exception("Unsupported type of $fieldName")
        }
    }
}