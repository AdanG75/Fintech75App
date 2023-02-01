package com.example.fintech75.data.model

import com.google.gson.annotations.SerializedName

data class BranchBase(
    @SerializedName("id_branch")
    val idBranch: String,
    @SerializedName("branch_name")
    val branchName: String,
    @SerializedName("service_hours")
    val serviceHours: String,
    val phone: String? = null,
    @SerializedName("created_time")
    val createdTime: String
)


data class BranchComplete(
    @SerializedName("id_branch")
    val idBranch: String,
    @SerializedName("branch_name")
    val branchName: String,
    @SerializedName("service_hours")
    val serviceHours: String,
    val phone: String? = null,
    @SerializedName("created_time")
    val createdTime: String,
    val address: AddressBase
)


data class BranchRegister(
    @SerializedName("id_market")
    val idMarket: String,
    @SerializedName("branch_name")
    val branchName: String,
    @SerializedName("service_hours")
    val serviceHours: String,
    val phone: String? = null,
    val password: String
)
