// SavedAccounts.kt

package com.mochyy.mochymusic.utils

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.constants.AccountChannelHandleKey
import com.mochyy.mochymusic.constants.AccountEmailKey
import com.mochyy.mochymusic.constants.AccountNameKey
import com.mochyy.mochymusic.constants.DataSyncIdKey
import com.mochyy.mochymusic.constants.InnerTubeCookieKey
import com.mochyy.mochymusic.constants.SavedAccountsKey
import com.mochyy.mochymusic.constants.VisitorDataKey
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(
    val cookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val name: String,
    val email: String,
    val channelHandle: String,
) {
    // brand accounts share their owner's email, so the data sync id goes first
    val id: String get() = dataSyncId.ifBlank { email.ifBlank { cookie } }

    /**
     * The same account as [id], for things that get queued, compared or logged: a one-way hash, since [id]
     * can be the sign-in cookie itself when the account has no data sync id or email yet.
     */
    val opaqueId: String
        get() = java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }

    // a data class prints every field; this one holds a sign-in cookie, so a stray log or crash report must not
    override fun toString() = "SavedAccount(name=$name)"

    companion object {
        // the account that is signed in right now, or null when nobody is
        fun fromActive(prefs: Preferences): SavedAccount? {
            val cookie = prefs[InnerTubeCookieKey].orEmpty()
            if (cookie.isBlank()) return null
            return SavedAccount(
                cookie = cookie,
                visitorData = prefs[VisitorDataKey].orEmpty(),
                dataSyncId = prefs[DataSyncIdKey].orEmpty(),
                name = prefs[AccountNameKey].orEmpty(),
                email = prefs[AccountEmailKey].orEmpty(),
                channelHandle = prefs[AccountChannelHandleKey].orEmpty(),
            )
        }

        fun parse(json: String?): List<SavedAccount> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(json)
                List(array.length()) { i ->
                    val o = array.getJSONObject(i)
                    SavedAccount(
                        cookie = o.optString("cookie"),
                        visitorData = o.optString("visitorData"),
                        dataSyncId = o.optString("dataSyncId"),
                        name = o.optString("name"),
                        email = o.optString("email"),
                        channelHandle = o.optString("channelHandle"),
                    )
                }
            }.getOrDefault(emptyList())
        }

        fun encode(accounts: List<SavedAccount>): String {
            val array = JSONArray()
            accounts.forEach { a ->
                array.put(
                    JSONObject()
                        .put("cookie", a.cookie)
                        .put("visitorData", a.visitorData)
                        .put("dataSyncId", a.dataSyncId)
                        .put("name", a.name)
                        .put("email", a.email)
                        .put("channelHandle", a.channelHandle),
                )
            }
            return array.toString()
        }

        // adds the account, or refreshes the saved copy of it when it is already in the list
        fun upsert(prefs: MutablePreferences, account: SavedAccount) {
            val list = parse(prefs[SavedAccountsKey])
            val next = if (list.any { it.id == account.id }) list.map { if (it.id == account.id) account else it } else list + account
            if (next != list) prefs[SavedAccountsKey] = encode(next)
        }
    }
}
