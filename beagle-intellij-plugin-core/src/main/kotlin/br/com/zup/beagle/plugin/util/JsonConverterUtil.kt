/*
 * Copyright 2020 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.zup.beagle.plugin.util

import br.com.zup.beagle.annotation.BeaglePreview
import br.com.zup.beagle.serialization.jackson.BeagleSerializationUtil
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.util.lang.UrlClassLoader
import org.apache.commons.lang3.StringUtils
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Paths

open class JsonConverterUtil(private val project: Project) {

    private val logger = Logger.getInstance(JsonConverterUtil::class.java)

    companion object {
        const val KOTLIN_LANGUAGE = "kotlin"
        const val JAVA_LANGUAGE = "java"
        val AVAILABLE_LANGUAGES = listOf(KOTLIN_LANGUAGE, JAVA_LANGUAGE)
        val AVAILABLE_EXTENSIONS = listOf("kt", "java")

        private fun JsonNode.unwrapPlatform(): JsonNode = this.also {
            if (this is ObjectNode && this.has("platform")) {
                this.get("child").also {
                    this.removeAll()
                    this.setAll<JsonNode>(it as ObjectNode)
                }
            }
            this.forEach { it.unwrapPlatform() }
        }
    }

    open fun getJsonFromClass(virtualFile: VirtualFile?, methodName: String?, console: ConsoleView): String? {
        if (virtualFile != null && StringUtils.isNotBlank(methodName)) {
            val psiFile = virtualFile.toPsiFile(this.project)!!
            when (psiFile.language.id.toLowerCase()) {
                JAVA_LANGUAGE, KOTLIN_LANGUAGE -> {
                    val classLoader = getClassLoader()
                    val methodWithClassInstance = getMethod(virtualFile, psiFile, classLoader, methodName!!)
                    if (isValidMethodWithClassInstance(virtualFile, psiFile, methodWithClassInstance, console)) {
                        val result = methodWithClassInstance!!.method.invoke(methodWithClassInstance.instance)
                        if (result != null) {
                            val beagleObjectMapper = BeagleSerializationUtil.beagleObjectMapper(classLoader)
                            beagleObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                            beagleObjectMapper.enable(SerializationFeature.INDENT_OUTPUT)
                            val json = beagleObjectMapper.writeValueAsString(result)
                            val filtered = beagleObjectMapper.readTree(json).unwrapPlatform().toString()
                            console.print("\n${filtered}\n", ConsoleViewContentType.LOG_WARNING_OUTPUT)
                            return filtered
                        }
                    }
                }
                else -> {
                    console.print(
                        "\nFile ${virtualFile.name} with extension not supported, please select a valid file with one of the these extensions: $KOTLIN_LANGUAGE\n",
                        ConsoleViewContentType.ERROR_OUTPUT
                    )
                    return null
                }
            }
        }
        console.print("\nJson conversion error\n", ConsoleViewContentType.ERROR_OUTPUT)
        return null
    }

    private fun getClassLoader() = UrlClassLoader.build().files(
        OrderEnumerator.orderEntries(this.project).recursively().classes().pathsList.pathList
            .map { Paths.get(File(FileUtil.toSystemIndependentName(it)).toURI()) }
    ).get()


    private fun isValidMethodWithClassInstance(virtualFile: VirtualFile, psiFile: PsiFile, methodWithClassInstance: MethodWithClassInstance?, console: ConsoleView): Boolean {
        if (methodWithClassInstance != null) {
            if (methodWithClassInstance.instance != null || !methodWithClassInstance.instanceRequired) {
                return true
            }
            console.print("\nClass ${getClassPath(virtualFile, psiFile)} without a no args constructor\n", ConsoleViewContentType.ERROR_OUTPUT)
        } else {
            console.print(
                "\nMethod with annotation ${BeaglePreview::class.qualifiedName} not found for class ${getClassPath(virtualFile, psiFile)}\n",
                ConsoleViewContentType.ERROR_OUTPUT
            )
        }
        return false
    }

    private fun getMethod(virtualFile: VirtualFile, psiFile: PsiFile, classLoader: UrlClassLoader, methodName: String, isKtClass: Boolean = false): MethodWithClassInstance? {
        try {
            var classPath = getClassPath(virtualFile, psiFile)
            if (isKtClass) {
                classPath = classPath.plus("Kt")
            }
            val clazz = Class.forName(classPath, true, classLoader)
            val constructor = clazz.constructors.firstOrNull { it.parameterCount == 0 }
            var classInstance: Any? = null
            if (constructor != null && !isKtClass) {
                classInstance = constructor.newInstance()
            }
            val annotationClass = Class.forName(BeaglePreview::class.qualifiedName, true, classLoader).asSubclass(Annotation::class.java)
            val method = clazz.methods.filter { it.isAnnotationPresent(annotationClass) }
                .filter { it.parameterCount == 0 }
                .firstOrNull { it.name.equals(methodName, true) }
            if (method != null) {
                return MethodWithClassInstance(method, !isKtClass, classInstance)
            }
        } catch (exception: Exception) {
            this.logger.error("Unexpected error occurred", exception)
        }
        if (!isKtClass) {
            return getMethod(virtualFile, psiFile, classLoader, methodName, true)
        }
        return null
    }

    private fun getClassPath(virtualFile: VirtualFile, psiFile: PsiFile) = when (psiFile) {
        is KtFile -> psiFile.packageFqName.asString().plus(".").plus(virtualFile.nameWithoutExtension)
        is PsiJavaFile -> psiFile.packageName.plus(".").plus(virtualFile.nameWithoutExtension)
        else -> ""
    }

    class MethodWithClassInstance(val method: Method, val instanceRequired: Boolean, val instance: Any?)
}
