package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.PortInfo
import com.mocca.app.domain.model.ProcessInfo
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SystemResources
import io.github.aakira.napier.Napier
import kotlin.math.roundToInt

class SystemMonitorRepository(
    private val apiClient: MoccaApiClient
) {
    companion object {
        private const val TAG = "SystemMonitorRepository"
        private const val NO_SESSION_MESSAGE = "Start a session to view system info"
        private val PROCESS_NAME_REGEX = Regex("\"([^\"]+)\"")
        private val PID_REGEX = Regex("pid=(\\d+)")
        private val IDLE_REGEX = Regex("([0-9]+(?:\\.[0-9]+)?)%?\\s*id", RegexOption.IGNORE_CASE)
        private val PAGE_SIZE_REGEX = Regex("page size of (\\d+) bytes")
        private val MEMORY_PAGE_REGEX = Regex("(Pages [^:]+):\\s+(\\d+)\\.")
    }

    suspend fun getProcesses(sessionId: String?): Resource<List<ProcessInfo>> {
        if (sessionId.isNullOrBlank()) return Resource.Error(NO_SESSION_MESSAGE)

        val windowsProcesses = runShell(sessionId, windowsProcessCommand())
            .getOrNull()
            ?.let(::parseWindowsProcessCsv)
            ?.takeIf { it.isNotEmpty() }

        if (windowsProcesses != null) {
            return Resource.Success(windowsProcesses.sortedByDescending { it.cpu ?: Float.MIN_VALUE })
        }

        val unixProcesses = runShell(sessionId, "ps -axo pid=,user=,%cpu=,%mem=,comm= --sort=-%cpu")
            .getOrNull()
            ?.let(::parseUnixProcesses)
            ?.takeIf { it.isNotEmpty() }

        if (unixProcesses != null) {
            return Resource.Success(unixProcesses.sortedByDescending { it.cpu ?: Float.MIN_VALUE })
        }

        val taskListProcesses = runShell(sessionId, "tasklist /FO CSV /NH")
            .getOrNull()
            ?.let(::parseTaskList)
            ?.takeIf { it.isNotEmpty() }

        return if (taskListProcesses != null) {
            Resource.Success(taskListProcesses)
        } else {
            Resource.Error("Unable to read process list")
        }
    }

    suspend fun getPorts(sessionId: String?): Resource<List<PortInfo>> {
        if (sessionId.isNullOrBlank()) return Resource.Error(NO_SESSION_MESSAGE)

        val taskListByPid = runShell(sessionId, "tasklist /FO CSV /NH")
            .getOrNull()
            ?.let(::parseTaskList)
            ?.associateBy { it.pid }
            .orEmpty()

        val ssPorts = runShell(sessionId, "ss -tlnp")
            .getOrNull()
            ?.let(::parseSsPorts)
            ?.takeIf { it.isNotEmpty() }
        if (ssPorts != null) return Resource.Success(ssPorts)

        val lsofPorts = runShell(sessionId, "lsof -nP -iTCP -sTCP:LISTEN")
            .getOrNull()
            ?.let(::parseLsofPorts)
            ?.takeIf { it.isNotEmpty() }
        if (lsofPorts != null) return Resource.Success(lsofPorts)

        val netstatPorts = runShell(sessionId, "netstat -ano -p tcp")
            .getOrNull()
            ?.let { parseWindowsNetstat(it, taskListByPid) }
            ?.takeIf { it.isNotEmpty() }
        if (netstatPorts != null) return Resource.Success(netstatPorts)

        val macNetstatPorts = runShell(sessionId, "netstat -anv -p tcp")
            .getOrNull()
            ?.let(::parseUnixNetstat)
            ?.takeIf { it.isNotEmpty() }
        if (macNetstatPorts != null) return Resource.Success(macNetstatPorts)

        return Resource.Error("Unable to read listening ports")
    }

    suspend fun getSystemResources(sessionId: String?): Resource<SystemResources> {
        if (sessionId.isNullOrBlank()) return Resource.Error(NO_SESSION_MESSAGE)

        val cpu = readCpuPercent(sessionId)
        val memory = readMemory(sessionId)
        val disk = readDisk(sessionId)

        val resources = SystemResources(
            cpuPercent = cpu,
            memoryUsed = memory?.first,
            memoryTotal = memory?.second,
            diskUsed = disk?.first,
            diskTotal = disk?.second
        )

        return if (
            resources.cpuPercent != null ||
            resources.memoryUsed != null ||
            resources.memoryTotal != null ||
            resources.diskUsed != null ||
            resources.diskTotal != null
        ) {
            Resource.Success(resources)
        } else {
            Resource.Error("Unable to read system resources")
        }
    }

    private suspend fun runShell(sessionId: String, command: String): Result<String> {
        return apiClient.executeShell(sessionId, command).onFailure {
            Napier.w("$TAG command failed: $command", it)
        }
    }

    private fun windowsProcessCommand(): String =
        "powershell -NoProfile -Command \"Get-Process | Sort-Object CPU -Descending | Select-Object -First 20 @{Name='command';Expression={\$_.ProcessName}}, @{Name='pid';Expression={\$_.Id}}, @{Name='cpu';Expression={[math]::Round((if(\$_.CPU){\$_.CPU}else{0}),2)}}, @{Name='memory';Expression={('{0:N1} MB' -f (\$_.WorkingSet64 / 1MB))}}, @{Name='user';Expression={''}} | ConvertTo-Csv -NoTypeInformation\""

    private fun parseWindowsProcessCsv(output: String): List<ProcessInfo> {
        val lines = output.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2 || !lines.first().contains("command", ignoreCase = true)) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val columns = parseCsvLine(line)
            if (columns.size < 5) return@mapNotNull null
            ProcessInfo(
                pid = columns[1].trim(),
                command = columns[0].trim(),
                cpu = columns[2].trim().toFloatOrNull(),
                memory = columns[3].trim().ifBlank { null },
                user = columns[4].trim().ifBlank { null }
            )
        }
    }

    private fun parseTaskList(output: String): List<ProcessInfo> {
        return output.lines()
            .map { it.trim() }
            .filter { it.startsWith('"') }
            .mapNotNull { line ->
                val columns = parseCsvLine(line)
                if (columns.size < 5) return@mapNotNull null
                ProcessInfo(
                    pid = columns[1].trim(),
                    command = columns[0].trim(),
                    cpu = null,
                    memory = columns[4].trim().ifBlank { null },
                    user = null
                )
            }
    }

    private fun parseUnixProcesses(output: String): List<ProcessInfo> {
        return output.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 5)
                if (parts.size < 5) return@mapNotNull null
                val memoryPercent = parts[3].toFloatOrNull()?.let { "${formatPercent(it)}%" }
                ProcessInfo(
                    pid = parts[0],
                    user = parts[1],
                    cpu = parts[2].toFloatOrNull(),
                    memory = memoryPercent,
                    command = parts[4]
                )
            }
    }

    private fun parseSsPorts(output: String): List<PortInfo> {
        return output.lines()
            .dropWhile { !it.contains("Local Address") }
            .drop(1)
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank()) return@mapNotNull null
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size < 5) return@mapNotNull null
                val protocol = parts[0].lowercase()
                val address = parts[4]
                val port = extractPort(address) ?: return@mapNotNull null
                val process = PROCESS_NAME_REGEX.find(trimmed)?.groupValues?.getOrNull(1)
                    ?: PID_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.let { "pid $it" }
                PortInfo(port = port, protocol = protocol, process = process, address = address)
            }
            .distinctBy { "${it.protocol}:${it.address}:${it.port}" }
            .sortedBy { it.port }
    }

    private fun parseLsofPorts(output: String): List<PortInfo> {
        return output.lines()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 9) return@mapNotNull null
                val command = parts[0]
                val nameColumn = parts.last()
                val port = extractPort(nameColumn) ?: return@mapNotNull null
                PortInfo(
                    port = port,
                    protocol = "tcp",
                    process = command,
                    address = nameColumn.substringBeforeLast(':').ifBlank { nameColumn }
                )
            }
            .sortedBy { it.port }
    }

    private fun parseWindowsNetstat(
        output: String,
        processesByPid: Map<String, ProcessInfo>
    ): List<PortInfo> {
        return output.lines()
            .map { it.trim() }
            .filter { it.startsWith("TCP", ignoreCase = true) && it.contains("LISTENING", ignoreCase = true) }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size < 5) return@mapNotNull null
                val address = parts[1]
                val port = extractPort(address) ?: return@mapNotNull null
                val pid = parts.last()
                PortInfo(
                    port = port,
                    protocol = parts[0].lowercase(),
                    process = processesByPid[pid]?.command ?: if (pid.isBlank()) null else "pid $pid",
                    address = address.substringBeforeLast(':')
                )
            }
            .sortedBy { it.port }
    }

    private fun parseUnixNetstat(output: String): List<PortInfo> {
        return output.lines()
            .map { it.trim() }
            .filter { it.startsWith("tcp", ignoreCase = true) && it.contains("LISTEN", ignoreCase = true) }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                val address = parts.firstOrNull { it.contains(':') } ?: return@mapNotNull null
                val port = extractPort(address) ?: return@mapNotNull null
                PortInfo(
                    port = port,
                    protocol = "tcp",
                    process = null,
                    address = address.substringBeforeLast(':')
                )
            }
            .sortedBy { it.port }
    }

    private suspend fun readCpuPercent(sessionId: String): Float? {
        runShell(sessionId, "powershell -NoProfile -Command \"(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average\"")
            .getOrNull()
            ?.trim()
            ?.toFloatOrNull()
            ?.let { return it.coerceIn(0f, 100f) }

        runShell(sessionId, "top -bn1")
            .getOrNull()
            ?.let(::parseLinuxCpuPercent)
            ?.let { return it }

        runShell(sessionId, "top -l 1")
            .getOrNull()
            ?.let(::parseMacCpuPercent)
            ?.let { return it }

        return null
    }

    private suspend fun readMemory(sessionId: String): Pair<Long, Long>? {
        runShell(sessionId, "free -b")
            .getOrNull()
            ?.let(::parseFreeMemory)
            ?.let { return it }

        runShell(sessionId, "powershell -NoProfile -Command \"Get-CimInstance Win32_OperatingSystem | Select-Object TotalVisibleMemorySize,FreePhysicalMemory | ConvertTo-Csv -NoTypeInformation\"")
            .getOrNull()
            ?.let(::parseWindowsMemoryCsv)
            ?.let { return it }

        val vmStat = runShell(sessionId, "vm_stat").getOrNull()
        val totalMemory = runShell(sessionId, "sysctl -n hw.memsize").getOrNull()?.trim()?.toLongOrNull()
        if (vmStat != null && totalMemory != null) {
            parseMacMemory(vmStat, totalMemory)?.let { return it }
        }

        return null
    }

    private suspend fun readDisk(sessionId: String): Pair<Long, Long>? {
        runShell(sessionId, "df -kP /")
            .getOrNull()
            ?.let(::parseUnixDisk)
            ?.let { return it }

        runShell(sessionId, "powershell -NoProfile -Command \"Get-CimInstance Win32_LogicalDisk -Filter 'DriveType=3' | Select-Object Size,FreeSpace | ConvertTo-Csv -NoTypeInformation\"")
            .getOrNull()
            ?.let(::parseWindowsDiskCsv)
            ?.let { return it }

        return null
    }

    private fun parseLinuxCpuPercent(output: String): Float? {
        val line = output.lines().firstOrNull { it.contains("Cpu(s)") || it.contains("CPU:") } ?: return null
        val idle = IDLE_REGEX.find(line)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        return idle?.let { (100f - it).coerceIn(0f, 100f) }
    }

    private fun parseMacCpuPercent(output: String): Float? {
        val line = output.lines().firstOrNull { it.contains("CPU usage", ignoreCase = true) } ?: return null
        val idle = IDLE_REGEX.find(line)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        return idle?.let { (100f - it).coerceIn(0f, 100f) }
    }

    private fun parseFreeMemory(output: String): Pair<Long, Long>? {
        val line = output.lines().firstOrNull { it.trim().startsWith("Mem:") } ?: return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 3) return null
        val total = parts[1].toLongOrNull() ?: return null
        val used = parts[2].toLongOrNull() ?: return null
        return used to total
    }

    private fun parseWindowsMemoryCsv(output: String): Pair<Long, Long>? {
        val dataLine = output.lines().map { it.trim() }.firstOrNull { it.startsWith('"') && !it.contains("TotalVisibleMemorySize") }
            ?: return null
        val columns = parseCsvLine(dataLine)
        if (columns.size < 2) return null
        val total = columns[0].trim().toLongOrNull()?.times(1024) ?: return null
        val free = columns[1].trim().toLongOrNull()?.times(1024) ?: return null
        return (total - free).coerceAtLeast(0L) to total
    }

    private fun parseMacMemory(vmStatOutput: String, totalMemory: Long): Pair<Long, Long>? {
        val pageSize = PAGE_SIZE_REGEX.find(vmStatOutput)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 4096L
        val freePages = MEMORY_PAGE_REGEX.findAll(vmStatOutput)
            .filter { match ->
                val key = match.groupValues[1]
                key.contains("Pages free") || key.contains("Pages speculative")
            }
            .sumOf { it.groupValues[2].replace(".", "").toLongOrNull() ?: 0L }
        val freeBytes = freePages * pageSize
        return (totalMemory - freeBytes).coerceAtLeast(0L) to totalMemory
    }

    private fun parseUnixDisk(output: String): Pair<Long, Long>? {
        val line = output.lines().map { it.trim() }.firstOrNull { it.isNotBlank() && !it.startsWith("Filesystem") } ?: return null
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 6) return null
        val totalKb = parts[1].toLongOrNull() ?: return null
        val usedKb = parts[2].toLongOrNull() ?: return null
        return usedKb * 1024L to totalKb * 1024L
    }

    private fun parseWindowsDiskCsv(output: String): Pair<Long, Long>? {
        val rows = output.lines()
            .map { it.trim() }
            .filter { it.startsWith('"') && !it.contains("Size") }
            .map(::parseCsvLine)
        if (rows.isEmpty()) return null
        val total = rows.sumOf { it.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L }
        val free = rows.sumOf { it.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L }
        if (total <= 0L) return null
        return (total - free).coerceAtLeast(0L) to total
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        values += current.toString()
        return values
    }

    private fun extractPort(address: String): Int? {
        val portText = address.substringAfterLast(':', missingDelimiterValue = "")
            .removePrefix("[")
            .removeSuffix("]")
        return portText.toIntOrNull()
    }

    private fun formatPercent(value: Float): String {
        val rounded = (value * 10f).roundToInt() / 10f
        return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    }

}
