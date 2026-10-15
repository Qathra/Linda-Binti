package com.example.lindabinti.data.content

import com.example.lindabinti.data.remote.LocalizedTextDto
import com.example.lindabinti.data.remote.SupportServiceDto
import com.example.lindabinti.data.remote.SupportServicesCatalogDto

object SupportServicesMerger {

    fun merge(
        bundled: List<SupportServiceRecord> = SupportServicesCatalog.records,
        remote: SupportServicesCatalogDto?
    ): List<SupportServiceRecord> {
        val byId = bundled.associateBy { it.id }.toMutableMap()
        remote?.services.orEmpty().forEach { dto ->
            val parsed = dto.toRecord() ?: return@forEach
            byId[parsed.id] = parsed.withoutUnverifiedPhones()
        }
        val phones = byId.values.flatMap { listOfNotNull(it.phone, it.alternatePhone) }.toSet()
        SupportServicesCatalog.REQUIRED_PHONES.forEach { required ->
            if (required !in phones) {
                val original = bundled.firstOrNull {
                    it.phone == required || it.alternatePhone == required
                }
                if (original != null) byId[original.id] = original
            }
        }
        val bundledIds = bundled.map { it.id }
        val extraIds = byId.keys.filterNot { it in bundledIds }
        return (bundledIds + extraIds).mapNotNull { byId[it] }
    }
}

fun SupportServiceRecord.withoutUnverifiedPhones(): SupportServiceRecord {
    if (phoneVerified && !phone.isNullOrBlank()) return this
    return copy(phone = null, alternatePhone = null, phoneVerified = false)
}

fun SupportServiceRecord.toDto(): SupportServiceDto = SupportServiceDto(
    id = id,
    kind = kind.wireName(),
    alsoKinds = alsoKinds.map { it.wireName() },
    name = name.toDto(),
    helpsWith = helpsWith.toDto(),
    phone = phone,
    alternatePhone = alternatePhone,
    phoneVerified = phoneVerified,
    location = location.toDto(),
    locationVerified = locationVerified,
    referralGuidance = referralGuidance.toDto(),
    area = area.wireName()
)

fun SupportServicesCatalog.toDto(): SupportServicesCatalogDto = SupportServicesCatalogDto(
    version = VERSION,
    source = "bundled",
    services = records.map { it.toDto() }
)

fun SupportServiceDto.toRecord(): SupportServiceRecord? {
    val resolvedId = id.trim()
    val resolvedKind = kind.toKind() ?: return null
    if (resolvedId.isBlank()) return null
    return SupportServiceRecord(
        id = resolvedId,
        kind = resolvedKind,
        alsoKinds = alsoKinds.mapNotNull { it.toKind() }.toSet(),
        name = name.toCopy(),
        helpsWith = helpsWith.toCopy(),
        phone = phone?.trim()?.takeIf { it.isNotBlank() },
        alternatePhone = alternatePhone?.trim()?.takeIf { it.isNotBlank() },
        phoneVerified = phoneVerified,
        location = location.toCopy(),
        locationVerified = locationVerified,
        referralGuidance = referralGuidance.toCopy(),
        area = area.toArea()
    )
}

private fun Copy.toDto(): LocalizedTextDto = LocalizedTextDto(en = en, sw = sw)

private fun LocalizedTextDto.toCopy(): Copy = Copy(en = en.ifBlank { sw }, sw = sw.ifBlank { en })

private fun SupportServiceKind.wireName(): String = when (this) {
    SupportServiceKind.EMERGENCY -> "emergency"
    SupportServiceKind.GBV -> "gbv"
    SupportServiceKind.CHILD_PROTECTION -> "child_protection"
    SupportServiceKind.MEDICAL -> "medical"
    SupportServiceKind.COUNSELLING -> "counselling"
    SupportServiceKind.POLICE -> "police"
    SupportServiceKind.FGM -> "fgm"
}

private fun String.toKind(): SupportServiceKind? = when (trim().lowercase()) {
    "emergency" -> SupportServiceKind.EMERGENCY
    "gbv" -> SupportServiceKind.GBV
    "child_protection", "child" -> SupportServiceKind.CHILD_PROTECTION
    "medical" -> SupportServiceKind.MEDICAL
    "counselling", "psychosocial" -> SupportServiceKind.COUNSELLING
    "police", "security" -> SupportServiceKind.POLICE
    "fgm" -> SupportServiceKind.FGM
    else -> null
}

private fun SupportServiceArea.wireName(): String = when (this) {
    SupportServiceArea.KENYA -> "kenya"
    SupportServiceArea.KAJIADO -> "kajiado"
}

private fun String.toArea(): SupportServiceArea =
    if (trim().equals("kajiado", ignoreCase = true)) SupportServiceArea.KAJIADO
    else SupportServiceArea.KENYA
