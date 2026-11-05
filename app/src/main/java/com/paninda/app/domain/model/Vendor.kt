package com.paninda.app.domain.model

data class Vendor(
    val id: String,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val updatedAt: Long = 0,
    val version: Long = 1,
)

data class VendorDraft(val name: String, val contactPerson: String = "", val phone: String = "",
    val email: String = "", val address: String = "", val notes: String = "")

sealed interface SaveVendorResult {
    data class Saved(val id: String) : SaveVendorResult
    data object Invalid : SaveVendorResult
    data object DuplicateName : SaveVendorResult
    data object Missing : SaveVendorResult
    data object Failed : SaveVendorResult
}

enum class VendorFilter { ACTIVE, ARCHIVED, ALL;
    fun matches(vendor: Vendor, query: String): Boolean =
        (this == ALL || vendor.isActive == (this == ACTIVE)) && (query.isBlank() ||
            listOf(vendor.name, vendor.contactPerson, vendor.phone, vendor.email).any { it.contains(query.trim(), true) })
}
