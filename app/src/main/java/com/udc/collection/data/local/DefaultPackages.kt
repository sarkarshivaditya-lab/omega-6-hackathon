package com.udc.collection.data.local

import com.google.gson.Gson
import com.udc.collection.data.local.entity.LabPackageEntity

object DefaultPackages {
    private val gson = Gson()

    private fun ids(vararg ids: Long): String = gson.toJson(ids.toList())
    private fun names(vararg names: String): String = gson.toJson(names.toList())

    val packages: List<LabPackageEntity> = listOf(
        LabPackageEntity(
            name = "Basic Health Package",
            description = "CBC, Blood Sugar, Urine Routine",
            price = 599.0,
            testIds = ids(1, 19, 84),
            testNames = names("Complete Blood Count (CBC)", "Fasting Blood Glucose", "Urine Routine & Microscopy")
        ),
        LabPackageEntity(
            name = "Comprehensive Health Package",
            description = "CBC, LFT, KFT, Lipid Profile, Thyroid, Blood Sugar, Urine",
            price = 1999.0,
            testIds = ids(1, 19, 51, 84),
            testNames = names("Complete Blood Count (CBC)", "Fasting Blood Glucose", "Lipid Profile (Complete)", "Urine Routine & Microscopy")
        ),
        LabPackageEntity(
            name = "Diabetic Monitoring Package",
            description = "HbA1c, Fasting Glucose, PPBS, Creatinine, Urine Routine",
            price = 799.0,
            testIds = ids(15, 19, 20, 22, 84),
            testNames = names("HbA1c", "Fasting Blood Glucose", "Post Prandial Blood Glucose", "Creatinine", "Urine Routine & Microscopy")
        ),
        LabPackageEntity(
            name = "Cardiac Risk Package",
            description = "Lipid Profile, Troponin I, ECG, CRP, CBC",
            price = 1499.0,
            testIds = ids(1, 51, 44, 97),
            testNames = names("Complete Blood Count (CBC)", "Lipid Profile (Complete)", "Troponin I", "CRP")
        ),
        LabPackageEntity(
            name = "Liver Function Test (LFT)",
            description = "Bilirubin, SGOT, SGPT, ALP, Total Protein, Albumin",
            price = 600.0,
            testIds = ids(31, 32, 33, 34, 35, 37, 38),
            testNames = names("Total Bilirubin", "Direct Bilirubin", "Indirect Bilirubin", "SGOT (AST)", "SGPT (ALT)", "Total Protein", "Albumin")
        ),
        LabPackageEntity(
            name = "Kidney Function Test (KFT)",
            description = "Urea, Creatinine, Uric Acid, Sodium, Potassium",
            price = 500.0,
            testIds = ids(21, 22, 23, 24, 25),
            testNames = names("Urea", "Creatinine", "Uric Acid", "Sodium", "Potassium")
        ),
        LabPackageEntity(
            name = "Thyroid Profile",
            description = "T3, T4, TSH",
            price = 700.0,
            testIds = ids(58, 59, 57),
            testNames = names("T3", "T4", "TSH")
        ),
        LabPackageEntity(
            name = "Female Hormone Profile",
            description = "FSH, LH, Prolactin, Estradiol, Progesterone, AMH",
            price = 1500.0,
            testIds = ids(63, 64, 65, 68, 67, 72),
            testNames = names("FSH", "LH", "Prolactin", "Estradiol (E2)", "Progesterone", "AMH")
        ),
        LabPackageEntity(
            name = "Antenatal Profile",
            description = "Blood Group, HIV, HBsAg, VDRL, Haemoglobin, Urine Routine",
            price = 1800.0,
            testIds = ids(16, 100, 98, 96, 2, 84),
            testNames = names("Blood Group & Rh Typing", "HIV (ELISA)", "HBsAg", "VDRL", "Haemoglobin", "Urine Routine & Microscopy")
        ),
        LabPackageEntity(
            name = "Vitamin Panel",
            description = "Vitamin D, Vitamin B12, Folic Acid",
            price = 1800.0,
            testIds = ids(76, 77, 78),
            testNames = names("Vitamin D (25-OH)", "Vitamin B12", "Folic Acid")
        )
    )
}
