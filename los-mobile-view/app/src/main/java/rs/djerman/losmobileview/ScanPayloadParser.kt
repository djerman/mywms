package rs.djerman.losmobileview

data class ParsedScanPayload(
    val rawValue: String,
    val itemCode: String? = null,
    val lot: String? = null,
    val expiryDate: String? = null,
    val serial: String? = null,
    val isGs1: Boolean = false,
)

object ScanPayloadParser {
    private const val GROUP_SEPARATOR = '\u001D'

    private val gs1AimPrefixes = setOf("]Q3", "]d2")
    private val knownAimPrefixes = setOf("]Q1", "]Q3", "]d1", "]d2")
    private val gs1Capable2dLabels = setOf(
        "QR_CODE",
        "QR",
        "DATA_MATRIX",
        "DATAMATRIX",
        "PDF417",
        "PDF_417",
        "AZTEC",
    )

    fun parse(rawValue: String, symbologyLabel: String? = null): ParsedScanPayload {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return ParsedScanPayload(rawValue = "")
        }

        val aimPrefix = trimmed.takeIf { it.length >= 3 && it[0] == ']' }?.substring(0, 3)
        val payload = if (aimPrefix != null && aimPrefix in knownAimPrefixes) {
            trimmed.substring(3)
        } else {
            trimmed
        }

        if (!shouldTreatAsGs1(payload, aimPrefix, symbologyLabel)) {
            return ParsedScanPayload(rawValue = payload)
        }

        val data = parseGs1Data(payload) ?: return ParsedScanPayload(rawValue = payload)

        return ParsedScanPayload(
            rawValue = payload,
            itemCode = data["01"],
            lot = data["10"],
            expiryDate = data["17"]?.let(::formatGs1Date),
            serial = data["21"],
            isGs1 = true,
        )
    }

    private fun shouldTreatAsGs1(
        payload: String,
        aimPrefix: String?,
        symbologyLabel: String?,
    ): Boolean {
        if (payload.startsWith("http://", ignoreCase = true) || payload.startsWith("https://", ignoreCase = true)) {
            return false
        }
        if (aimPrefix in gs1AimPrefixes) {
            return true
        }
        if (symbologyLabel?.contains("GS1", ignoreCase = true) == true) {
            return true
        }
        if (symbologyLabel != null && !isGs1Capable2d(symbologyLabel)) {
            return false
        }
        if (symbologyLabel == null) {
            return false
        }
        return payload.contains(GROUP_SEPARATOR) || payload.startsWith("01")
    }

    private fun isGs1Capable2d(symbologyLabel: String): Boolean {
        val normalized = symbologyLabel
            .trim()
            .uppercase()
            .replace('-', '_')
            .replace(' ', '_')
        return normalized in gs1Capable2dLabels
    }

    private fun parseGs1Data(payload: String): Map<String, String>? {
        var index = 0
        val fields = linkedMapOf<String, String>()

        while (index < payload.length) {
            while (index < payload.length && payload[index] == GROUP_SEPARATOR) {
                index++
            }
            if (index >= payload.length) {
                break
            }

            when {
                payload.startsWith("01", index) -> {
                    index += 2
                    if (index + 14 > payload.length) {
                        return null
                    }
                    fields["01"] = payload.substring(index, index + 14)
                    index += 14
                }

                payload.startsWith("17", index) -> {
                    index += 2
                    if (index + 6 > payload.length) {
                        return null
                    }
                    fields["17"] = payload.substring(index, index + 6)
                    index += 6
                }

                payload.startsWith("10", index) -> {
                    index += 2
                    val end = payload.indexOf(GROUP_SEPARATOR, index).let { if (it == -1) payload.length else it }
                    if (end <= index) {
                        return null
                    }
                    fields["10"] = payload.substring(index, end)
                    index = end
                }

                payload.startsWith("21", index) -> {
                    index += 2
                    val end = payload.indexOf(GROUP_SEPARATOR, index).let { if (it == -1) payload.length else it }
                    if (end <= index) {
                        return null
                    }
                    fields["21"] = payload.substring(index, end)
                    index = end
                }

                else -> return null
            }
        }

        return fields.takeIf { it.containsKey("01") }
    }

    private fun formatGs1Date(value: String): String {
        if (value.length != 6 || value.any { !it.isDigit() }) {
            return value
        }
        val year = value.substring(0, 2).toInt()
        val month = value.substring(2, 4)
        val day = value.substring(4, 6)
        val century = if (year >= 50) 1900 else 2000
        return "%04d-%s-%s".format(century + year, month, day)
    }
}
