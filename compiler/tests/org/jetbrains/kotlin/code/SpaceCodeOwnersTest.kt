/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.eclipse.jgit.ignore.FastIgnoreRule
import org.eclipse.jgit.ignore.IgnoreNode
import java.io.File

class SpaceCodeOwnersTest : TestCase() {
    private val ownersFile = File(".space/CODEOWNERS")
    private val owners = parseCodeOwners(ownersFile)
    private val fileWalkDepthLimit = 8

    fun testOwnerListNoDuplicates() {
        val entries = owners.filterIsInstance<OwnershipItem.OwnerListEntry>()
        val set = mutableSetOf<String>()
        for (entry in entries) {
            if (!set.add(entry.name)) {
                fail("Duplicated OWNER_LIST entry: $entry")
            }
        }
    }

    fun testAllOwnersInOwnerList() {
        val permittedOwners = owners
            .filterIsInstance<OwnershipItem.OwnerListEntry>()
            .map { it.name }
            .toSet()
        for (pattern in owners.filterIsInstance<OwnershipItem.Pattern>()) {
            for (owner in pattern.owners) {
                if (owner !in permittedOwners) {
                    fail("Owner \"$owner\" not listed in OWNER_LIST, but used in $pattern")
                }
            }
        }
    }

    fun testPatterns() {
        data class ItemUse(val item: OwnershipItem, val rule: FastIgnoreRule, var uses: Int = 0) {
            fun countMatch(path: String, isDirectory: Boolean): Boolean {
                if (!rule.isMatch(path, isDirectory)) return false
                uses++
                return true
            }
        }

        val matchers = owners.mapNotNull {
            when (it) {
                is OwnershipItem.OwnerListEntry -> return@mapNotNull null
                is OwnershipItem.Pattern -> ItemUse(it, FastIgnoreRule(it.pattern))
                is OwnershipItem.UnknownPathPattern -> ItemUse(it, FastIgnoreRule(it.pattern))
            }
        }.reversed()

        val fileMatchers = matchers.filterNot { (_, rule) -> rule.dirOnly() }

        val ignoreTracker = GitIgnoreTracker()
        val root = File(".")

        val unmatchedFilesTop = mutableListOf<File>()

        fun isOwned(path: String, isDirectory: Boolean): Boolean {
            return if (isDirectory) {
                matchers.any { it.countMatch(path, true) }
            } else {
                fileMatchers.any { it.countMatch(path, false) }
            }
        }

        fun visitFile(file: File, parentOwned: Boolean) {
            val path = file.path
            if (ignoreTracker.isIgnored(path, isDirectory = true)) return
            if (isOwned(path, isDirectory = true)) return
            if (parentOwned) return
            if (unmatchedFilesTop.size < 10) {
                unmatchedFilesTop.add(file)
            }
        }

        fun visitDirectory(directory: File, parentOwned: Boolean, depth: Int) {
            if (depth > fileWalkDepthLimit) return
            val path = directory.path

            if (ignoreTracker.isIgnored(path, isDirectory = true)) return
            val directoryOwned = isOwned(path, isDirectory = true) || parentOwned
            ignoreTracker.withDirectory(directory) {
                for (childName in (directory.list() ?: emptyArray())) {
                    val child = if (directory == root) {
                        File(childName)
                    } else {
                        File(directory, childName)
                    }
                    if (child.isDirectory) {
                        visitDirectory(child, directoryOwned, depth + 1)
                    } else {
                        visitFile(child, directoryOwned)
                    }
                }
            }
        }

        visitDirectory(root, false, 0)

        if (unmatchedFilesTop.isNotEmpty()) {
            fail("Found files without owner: [${unmatchedFilesTop.joinToString()}], please add it to $ownersFile")
        }

        val unusedPatterns = matchers.filter { it.uses == 0 }
        if (unusedPatterns.isNotEmpty()) {
            fail("Unused patterns in $ownersFile: ${unusedPatterns.joinToString { it.item.toString() }}")
        }
    }
}


private class GitIgnoreTracker {
    val ignoreNodeStack = mutableListOf(
        IgnoreNode(listOf(FastIgnoreRule("/.git")))
    )

    fun isIgnored(path: String, isDirectory: Boolean): Boolean {
        return ignoreNodeStack.asReversed().firstNotNullOfOrNull { ignoreNode -> ignoreNode.checkIgnored(path, isDirectory) } ?: false
    }

    inline fun withDirectory(directory: File, action: () -> Unit) {
        val ignoreFile = directory.resolve(".gitignore").takeIf { it.exists() }
        if (ignoreFile != null) {
            val ignoreNode = IgnoreNode().apply {
                ignoreFile.inputStream().use {
                    parse(ignoreFile.path, ignoreFile.inputStream())
                }
            }
            ignoreNodeStack.add(ignoreNode)
        }
        action()
        if (ignoreFile != null) {
            ignoreNodeStack.removeAt(ignoreNodeStack.lastIndex)
        }
    }
}

private sealed class OwnershipItem {
    abstract val line: Int

    data class Pattern(val pattern: String, val owners: List<String>, override val line: Int) : OwnershipItem()
    data class UnknownPathPattern(val pattern: String, override val line: Int) : OwnershipItem()
    data class OwnerListEntry(val name: String, override val line: Int) : OwnershipItem()
}


private fun parseCodeOwners(file: File): List<OwnershipItem> {
    fun parseDirective(line: String, directive: String): String? {
        val value = line.substringAfter("# $directive: ")
        if (value != line) return value
        return null
    }

    val ownersPattern = "(\"[^\"]+\")|(\\S+)".toRegex()

    fun parseOwnerNames(ownerString: String): List<String> {
        return ownersPattern.findAll(ownerString).map { it.value.removeSurrounding("\"") }.toList()
    }

    return file.useLines { lines ->
        buildList {
            val out = this
            for ((index, line) in lines.withIndex()) {
                val lineNumber = index + 1
                if (line.startsWith("#")) {
                    parseDirective(line, UNKNOWN_DIRECTIVE)?.let {
                        out.add(OwnershipItem.UnknownPathPattern(it.trim(), lineNumber))
                    }
                    parseDirective(line, OWNER_LIST_DIRECTIVE)?.let {
                        parseOwnerNames(it).mapTo(out) { owner ->
                            OwnershipItem.OwnerListEntry(owner, lineNumber)
                        }
                    }
                } else if (line.isNotBlank()) {
                    // Note: Space CODEOWNERS grammar is ambiguous, as it is impossible to distinguish between file pattern with spaces
                    // and team name, so we re-use similar logic
                    // ex:
                    // ```
                    // /some/path/Read Me.md Owner
                    // ```
                    // In such pattern it is impossible to distinguish between file ".../Read Me.md" or file ".../Read" owned by "Me.md"
                    // See SPACE-17772
                    val (pattern, owners) = line.split(' ', limit = 2)
                    out.add(OwnershipItem.Pattern(pattern, parseOwnerNames(owners), lineNumber))
                }
            }
        }
    }
}

private const val OWNER_LIST_DIRECTIVE = "OWNER_LIST"
private const val UNKNOWN_DIRECTIVE = "UNKNOWN"