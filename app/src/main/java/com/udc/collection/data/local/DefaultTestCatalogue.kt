package com.udc.collection.data.local

import com.udc.collection.data.local.entity.LabTestEntity

object DefaultTestCatalogue {
    // OMEGA 6.0 ships with an empty catalogue so it can be configured for any profession.
    val tests: List<LabTestEntity> = emptyList()
}
