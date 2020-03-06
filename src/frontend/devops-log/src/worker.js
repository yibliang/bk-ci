import ansiParse from './assets/ansiParse'

const colorList = [
    { key: '##[command]', color: 'rgba(146,166,202,1)' },
    { key: '##[info]', color: 'rgba(127,202,84,1)' },
    { key: '##[warning]', color: 'rgba(246,222,84,1)' },
    { key: '##[error]', color: 'rgba(247,49,49,1)' },
    { key: '##[debug]', color: 'rgba(99,176,106,1)' }
]

const reg = (() => {
    const colors = colorList.map((color) => {
        let key = color.key
        key = key.replace(/\[|\]/gi, '\\$&')
        return `(${key})`
    })
    return new RegExp(`${colors.join('|')}`, 'gi')
})()

function handleColor (val) {
    const parseRes = ansiParse(val) || [{ message: '' }]
    const res = { message: '' }
    parseRes.forEach((item) => {
        res.message += item.message
        if (!res.color && item.color) res.color = item.color
    })
    
    const currentColor = colorList.find(color => String(val).includes(color.key))
    if (currentColor) res.color = currentColor.color
    if (res.color) res.fontWeight = 600
    res.message = String(res.message).replace(reg, '')
    return res
}

let allListData = []
let tagList = []
let mainWidth = 0
let mainWordNum = 0

onmessage = function (e) {
    const data = e.data
    const type = data.type
    switch (type) {
        case 'initLog':
            mainWidth = data.mainWidth - 70
            mainWordNum = Math.floor(mainWidth / 6.8)
            addListData(data)
            postMessage({ type: 'completeInit', number: allListData.length })
            break
        case 'addListData':
            mainWidth = data.mainWidth - 70
            mainWordNum = Math.floor(mainWidth / 6.8)
            addListData(data)
            postMessage({ type: 'completeAdd', number: allListData.length })
            break
        case 'wheelGetData':
            getListData(data)
            break
        case 'foldListData':
            foldListData(data)
            postMessage({ type: 'completeFold', number: allListData.length })
            break
        case 'resetData':
            allListData = []
            tagList = []
            break
    }
}

function foldListData ({ startIndex }) {
    const realIndex = allListData.findIndex(x => x.realIndex === startIndex)
    const currentItem = allListData[realIndex]
    if (!currentItem || !currentItem.children) return

    if (!currentItem.children.length) {
        let totalNum = currentItem.endIndex - startIndex
        while (totalNum > 0) {
            let currentNum
            if (totalNum > 10000) {
                currentNum = 10000
                totalNum -= 10000
            } else {
                currentNum = totalNum
                totalNum = 0
            }
            const subList = allListData.splice(realIndex + 1, currentNum)
            currentItem.children.push(...subList)
        }
    } else {
        for (let index = 0; index < currentItem.children.length;) {
            const someList = currentItem.children.slice(index, index + 10000)
            allListData.splice(realIndex + 1 + index, 0, ...someList)
            index += 10000
        }
        currentItem.children = []
    }
}

let repeatLineNum = -1
function addListData ({ list }) {
    list.forEach((item) => {
        const { message, color } = handleColor(item.message || '')
        const newItemArr = message.split(/\r\n|\n/)
        newItemArr.forEach((message) => {
            const splitTextArr = splitText(message)
            splitTextArr.forEach((message, i) => {
                const currentIndex = allListData.length
                const newItem = {
                    message, color,
                    isNewLine: i > 0 ? (repeatLineNum++, true) : false,
                    showIndex: allListData.length - repeatLineNum,
                    realIndex: currentIndex,
                    timestamp: item.timestamp
                }
                if (message.includes('##[group]')) {
                    newItem.message = newItem.message.replace('##[group]', '')
                    tagList.push(newItem)
                }

                if (message.includes('##[endgroup]') && tagList.length) {
                    newItem.message = newItem.message.replace('##[endgroup]', '')
                    const linkItem = tagList.pop()
                    linkItem.endIndex = currentIndex
                    linkItem.children = []
                }

                allListData.push(newItem)
            })
        })
    })
}

function splitText (message) {
    let tempMes = ''
    const totalWidth = getTextWidth(message)
    const mesRes = []
    if (totalWidth < mainWidth) {
        mesRes.push(message)
    } else {
        const regex = /<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi
        const aList = []
        let tempA = null
        let currentIndex = 0

        while ((tempA = regex.exec(message)) != null) aList.push({
            content: tempA[0],
            href: tempA[1],
            text: tempA[2],
            startIndex: tempA.index
        })
        if (aList.length) message = message.replace(regex, '$2')

        while (message !== '') {
            [tempMes, message] = splitByChar(message)
            // a标签单独处理
            aList.forEach((x) => {
                if (x.startIndex <= currentIndex + tempMes.length && x.startIndex >= currentIndex) {
                    const curStartIndex = x.startIndex - currentIndex
                    const curLength = x.text.length
                    const diffDis = curStartIndex + curLength - tempMes.length
                    if (diffDis > 0) {
                        message = tempMes.slice(curStartIndex) + message
                        tempMes = tempMes.slice(0, curStartIndex)
                    } else {
                        tempMes = (tempMes.slice(0, curStartIndex) + x.content + tempMes.slice(curStartIndex + curLength))
                    }
                }
            })

            currentIndex += tempMes.length
            mesRes.push(tempMes)
        }
    }
    return mesRes
}

function splitByChar (message) {
    let tempMes = message.slice(0, mainWordNum)
    message = message.slice(mainWordNum)
    let tempWidth = getTextWidth(tempMes)
    while (tempWidth > mainWidth || (mainWidth - tempWidth > 15 && message !== '')) {
        if (tempWidth > mainWidth) {
            message = tempMes.slice(-1) + message
            tempMes = tempMes.slice(0, -1)
            tempWidth = getTextWidth(tempMes)
        } else {
            tempMes += message.slice(0, 1)
            message = message.slice(1)
            tempWidth = getTextWidth(tempMes)
        }
    }
    return [tempMes, message]
}

const canvas = new OffscreenCanvas(100, 1)
const context = canvas.getContext("2d")
context.font = 'normal 12px Consolas, "Courier New", monospace'
function getTextWidth(text) {
    const metrics = context.measureText(text)
    return metrics.width
}

function getListData ({ totalScrollHeight, itemHeight, itemNumber, canvasHeight, minMapTop, totalHeight, mapHeight, type }) {
    const realHeight = minMapTop / ((mapHeight - canvasHeight / 8) || 1) * (totalHeight - canvasHeight)
    let startIndex = Math.floor(realHeight / itemHeight)
    const endIndex = startIndex + itemNumber
    startIndex = startIndex > 0 ? startIndex - 1 : 0

    const listData = []
    const indexList = []
    const nums = Math.floor(startIndex * itemHeight / 500000)
    for (let i = startIndex; i <= endIndex; i++) {
        const top = i * itemHeight - nums * 500000
        const currentItem = allListData[i]
        if (typeof currentItem === 'undefined') continue
        indexList.push({
            top,
            value: currentItem.showIndex,
            isNewLine: currentItem.isNewLine,
            index: currentItem.realIndex,
            isFold: currentItem.endIndex !== undefined,
            hasFolded: (currentItem.children || []).length > 0
        })
        listData.push({ 
            top,
            isNewLine: currentItem.isNewLine,
            value: currentItem.message,
            color: currentItem.color,
            index: currentItem.realIndex,
            showIndex: currentItem.showIndex,
            fontWeight: currentItem.fontWeight,
            isFold: currentItem.endIndex !== undefined,
            timestamp: currentItem.timestamp
        })
    }

    totalScrollHeight = totalScrollHeight - nums * 500000

    let minMapStartIndex = startIndex - Math.floor(itemNumber * 8 * minMapTop / canvasHeight)
    if (minMapStartIndex < 0) minMapStartIndex = 0
    const minMapEndIndex = minMapStartIndex + itemNumber * 8
    const minMapList = []
    for (let i = minMapStartIndex; i <= minMapEndIndex; i++) {
        const currentItem = allListData[i]
        if (typeof currentItem === 'undefined') continue
        minMapList.push(currentItem)
    }

    postMessage({ type, indexList, listData, totalScrollHeight, minMapList })
}
