/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.utils

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64

@Suppress("ALL")
object CredentialUtils {

    fun getCredential(repository: Repository, credentials: List<String>, credentialType: CredentialType): Credential {
        when {
            repository is CodeSvnRepository && repository.svnType == CodeSvnRepository.SVN_TYPE_HTTP -> {
                // 兼容老的数据，老的数据是用的是password, 新的是username_password
                return if (credentialType == CredentialType.USERNAME_PASSWORD) {
                    if (credentials.size <= 1) {
                        logger.warn("Fail to get the username($credentials) of the svn repo $repository")
                        Credential(username = repository.userName, privateKey = credentials[0], passPhrase = null)
                    } else {
                        Credential(username = credentials[0], privateKey = credentials[1], passPhrase = null)
                    }
                } else {
                    Credential(username = repository.userName, privateKey = credentials[0], passPhrase = null)
                }
            }
            repository is CodeP4Repository && credentialType == CredentialType.USERNAME_PASSWORD ->
                return Credential(username = credentials[0], privateKey = "", passPhrase = credentials[1])
            else -> {
                val privateKey = credentials[0]
                val passPhrase = if (credentials.size > 1) {
                    val p = credentials[1]
                    if (p.isEmpty()) {
                        null
                    } else {
                        p
                    }
                } else {
                    null
                }
                return Credential(username = repository.userName, privateKey = privateKey, passPhrase = passPhrase)
            }
        }
    }

    fun getCredential(
        client: Client,
        projectId: String,
        credentialId: String,
        type: CredentialType,
        acrossProject: Boolean = false
    ): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(projectId, credentialId,
                                                                                encoder.encodeToString(pair.publicKey))
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) " +
                             "because of ${credentialResult.message}")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!
        if (type != credential.credentialType) {
            logger.error("CredentialId is invalid, expect:${type.name}, but real:${credential.credentialType.name}")
            throw ParamBlankException("Fail to get the credential($credentialId) of project($projectId)")
        }

        if (acrossProject && !credential.allowAcrossProject) {
            logger.warn("project $projectId credential $credentialId not allow across project use")
            throw RuntimeException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(
            DHUtil.decrypt(
            decoder.decode(credential.v1),
            decoder.decode(credential.publicKey),
            pair.privateKey))
        ticketMap["v1"] = v1

        if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            val v2 = String(
                DHUtil.decrypt(
                decoder.decode(credential.v2),
                decoder.decode(credential.publicKey),
                pair.privateKey))
            ticketMap["v2"] = v2
        }

        if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            val v3 = String(
                DHUtil.decrypt(
                decoder.decode(credential.v3),
                decoder.decode(credential.publicKey),
                pair.privateKey))
            ticketMap["v3"] = v3
        }

        if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            val v4 = String(
                DHUtil.decrypt(
                decoder.decode(credential.v4),
                decoder.decode(credential.publicKey),
                pair.privateKey))
            ticketMap["v4"] = v4
        }

        return ticketMap
    }

    private val logger = LoggerFactory.getLogger(CredentialUtils::class.java)
}

data class Credential(
    val username: String,
    val privateKey: String, // password or private key
    val passPhrase: String? // passphrase for ssh private key
)
