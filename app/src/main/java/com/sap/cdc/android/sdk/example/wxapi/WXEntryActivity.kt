package com.sap.cdc.android.sdk.example.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.sap.cdc.android.sdk.auth.provider.activity.ResultLoginActivity
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    private var weChatApi: IWXAPI? = null

    companion object {

        const val LOG_TAG = "WXEntryActivity"
        const val API_ID: String = "WECHAT_APP_ID_HERE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weChatApi = WXAPIFactory.createWXAPI(this, API_ID, false)
        weChatApi?.handleIntent(intent, this)
        finish()
    }

    override fun onReq(req: BaseReq?) {
        Log.d(LOG_TAG, "")
    }

    override fun onResp(resp: BaseResp?) {
        val resultIntent = Intent(this, ResultLoginActivity::class.java)
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        val bundle = Bundle()

        when (resp!!.errCode) {
            BaseResp.ErrCode.ERR_OK -> try {
                val sendResp = resp as SendAuth.Resp
                val code = sendResp.code

                bundle.putString("code", code)
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Exception while parsing token")
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                bundle.putBoolean("canceled", true)
            }

            else -> {
                bundle.putBoolean("error", true)
                bundle.putInt("errorCode", resp.errCode)
            }
        }

        resultIntent.putExtras(bundle)
        startActivity(resultIntent)
    }

}