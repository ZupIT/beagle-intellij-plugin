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

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.util.text.Strings
import java.io.File

/**
 *
 * In this file the implementation of getClassPath was made taking reference to the implementation of the classPath of
 * the plugin that is deprecated. Below the class package present in the file in the intellij plugin
 *
 * package com.intellij.ide.plugins.IdeaPluginDescriptorImpl.java
 * Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
*/

object ClassPath {

    fun getClassPath(plugin: IdeaPluginDescriptor): List<File> {
        val path: File = plugin.pluginPath.toFile()
        if (!path.isDirectory) {
            return listOf(path)
        }
        val result: MutableList<File> = ArrayList()
        val classesDir = File(path, "classes")
        if (classesDir.exists()) {
            result.add(classesDir)
        }
        val files = File(path, "lib").listFiles()
        if (files == null || files.size <= 0) {
            return result
        }
        for (f in files) {
            if (f.isFile) {
                val name = f.name
                if (Strings.endsWithIgnoreCase(name, ".jar") || Strings.endsWithIgnoreCase(name, ".zip")) {
                    result.add(f)
                }
            } else {
                result.add(f)
            }
        }
        return result
    }

}