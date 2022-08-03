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

package com.tencent.devops.metrics.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.metrics.pojo.`do`.PipelineFailDetailInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineFailInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailDTO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineFailTrendInfoDTO
import com.tencent.devops.metrics.pojo.vo.PipelineFailTrendInfoVO

interface PipelineFailManageService {

    /**
     * 查询流水线失败趋势数据
     * @param queryPipelineFailTrendDTO 查询流水线失败趋势数据传输对象
     * @return 流水线失败趋势数据列表
     */
    fun queryPipelineFailTrendInfo(
        queryPipelineFailTrendDTO: QueryPipelineFailTrendInfoDTO
    ): List<PipelineFailTrendInfoVO>

    /**
     * 查询流水线错误类型统计数据
     * @param queryPipelineFailDTO 查询流水线错误信息传输对象
     * @return 流水线错误类型统计数据集合
     */
    fun queryPipelineFailSumInfo(
        queryPipelineFailDTO: QueryPipelineFailDTO
    ): List<PipelineFailInfoDO>

    /**
     * 查询流水线失败详情数据
     * @param queryPipelineFailDTO 查询流水线错误信息传输对象
     * @return 流水线失败详情数据
     */
    fun queryPipelineFailDetailInfo(
        queryPipelineFailDTO: QueryPipelineFailDTO
    ): Page<PipelineFailDetailInfoDO>
}