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

package br.com.zup.beagle.plugin.marker

import br.com.zup.beagle.annotation.BeaglePreview
import br.com.zup.beagle.plugin.service.PluginService
import br.com.zup.beagle.plugin.util.BeagleIcons
import br.com.zup.beagle.plugin.util.JsonConverterUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.apache.commons.lang3.StringUtils
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import java.awt.event.MouseEvent

open class RunLineMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? = null

    private fun createLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<PsiElement>? {
        val virtualFile = psiElement.containingFile.virtualFile
        var methodName: String? = null
        if (psiElement.language.id.toLowerCase() in JsonConverterUtil.AVAILABLE_LANGUAGES) {
            methodName = checkIfLineIsMethodNameWithCorrectAnnotationInFileAndReturnMethodName(psiElement)
        }
        if (StringUtils.isNotBlank(methodName)) {
            val navigationHandler = { _: MouseEvent, e: PsiElement ->
                PluginService.getInstance(e.project).runPlugin(virtualFile, methodName)
            }
            val textRange = TextRange(psiElement.textRange.endOffset + 1, psiElement.textRange.endOffset + 2)
            return LineMarkerInfo(psiElement, textRange, BeagleIcons.BEAGLE_ICON, { "Run Beagle Plugin" },
                navigationHandler, GutterIconRenderer.Alignment.CENTER)
        }
        return null
    }

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        result.addAll(elements.mapNotNull { createLineMarkerInfo(it) })
    }

    private fun checkIfLineIsMethodNameWithCorrectAnnotationInFileAndReturnMethodName(psiElement: PsiElement): String? {
        if (this.checkAnnotation(psiElement)) {
            val iterator = psiElement.nextLeafs.iterator()
            while (iterator.hasNext()) {
                when (val context = iterator.next().context) {
                    is KtNamedFunction -> return context.identifyingText
                    is PsiMethod -> return context.identifyingText
                }
            }
        }
        return null
    }

    private fun checkAnnotation(psiElement: PsiElement) = when (psiElement) {
        is KtAnnotationEntry -> psiElement.toLightAnnotation()!!.hasQualifiedName(BeaglePreview::class.qualifiedName!!)
        is PsiAnnotation -> psiElement.hasQualifiedName(BeaglePreview::class.qualifiedName!!)
        else -> false
    }

    private val PsiNameIdentifierOwner.identifyingText get() = this.identifyingElement?.text
}