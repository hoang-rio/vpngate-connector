package vn.unlimit.vpngate.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.api.UserApiService
import vn.unlimit.vpngate.models.Captcha
import vn.unlimit.vpngate.models.request.ChangePasswordRequest
import vn.unlimit.vpngate.models.request.ForgotPasswordRequest
import vn.unlimit.vpngate.models.request.RegisterRequest
import vn.unlimit.vpngate.models.request.ResetPasswordRequest
import vn.unlimit.vpngate.models.request.UpdateProfileRequest
import vn.unlimit.vpngate.models.request.UserLoginRequest
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.Calendar
import java.util.Locale


class UserViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "UserViewModel"
        const val USER_CACHE_TIME = 10 * 60 * 1000 // 10 Minute
    }
    var userInfo: MutableLiveData<vn.unlimit.vpngate.models.User?> = MutableLiveData(paidServerUtil.getUserInfo())

    var isRegisterSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorList: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())
    var isUserActivated: MutableLiveData<Boolean> = MutableLiveData(false)
    var isForgotPassSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var isPasswordReset: MutableLiveData<Boolean> = MutableLiveData(false)
    var isValidResetPassToken: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorCode: Int? = null
    var isProfileUpdate = false
    private var userApiService: UserApiService = retrofit.create(UserApiService::class.java)

    fun login(username: String, password: String) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val loginResponse = userApiService.login(UserLoginRequest(username, password))
                Log.d(TAG, "Login success with response %s".format(loginResponse.toString()))
                userInfo.postValue(loginResponse.user)
                isLoggedIn.postValue(true)
                isLoading.postValue(false)
                paidServerUtil.setUserInfo(loginResponse.user!!)
                paidServerUtil.setIsLoggedIn(true)
                loginResponse.sessionId?.let {
                    paidServerUtil.setStringSetting(
                        PaidServerUtil.SESSION_ID_KEY,
                        it
                    )
                }
            } catch (e: HttpException) {
                Log.e(TAG, "Login error with HttpException", e)
                isLoggedIn.postValue(false)
                isLoading.postValue(false)
            } catch (e: Exception) {
                Log.e(TAG, "Login error with Exception", e)
                isLoggedIn.postValue(false)
                isLoading.postValue(false)
            }
        }
    }

    fun localLogout(activity: Activity?) {
        isLoggedIn.value = false
        paidServerUtil.setIsLoggedIn(false)
        paidServerUtil.removeSetting(PaidServerUtil.USER_INFO_KEY)
        userInfo.value = null
        activity?.let {
            if (!activity.isFinishing) {
                val intentLogin = Intent(activity, LoginActivity::class.java)
                activity.startActivity(intentLogin)
                activity.finish()
            }
        }
    }

    fun logout(activity: Activity?) {
        viewModelScope.launch {
            try {
                userApiService.logout()
            } finally {
                withContext(Dispatchers.Main) {
                    localLogout(activity)
                }
            }
        }
    }

    fun fetchUser(
        updateLoading: Boolean = false,
        activity: Activity? = null,
        forceFetch: Boolean = false
    ) {
        val lastFetchTime = paidServerUtil.getLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME)
        var date = Calendar.getInstance().time
        var nowInMs = date.time
        if (!forceFetch && lastFetchTime + USER_CACHE_TIME > nowInMs || updateLoading && isLoading.value!!) {
            // Skip fetch and use user in cache
            return
        }
        if (updateLoading) {
            isLoading.value = true
        }
        viewModelScope.launch {
            try {
                val fetchUser = userApiService.fetchUser()
                paidServerUtil.setUserInfo(fetchUser)
                if (updateLoading) {
                    isLoading.postValue(false)
                }
                date = Calendar.getInstance().time
                nowInMs = date.time
                paidServerUtil.setLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME, nowInMs)
                isProfileUpdate = false
            } catch (e: HttpException) {
                Log.e(TAG, "fetch user error with HttpException", e)
                val params = Bundle()
                params.putString("username", paidServerUtil.getUserInfo()?.username)
                params.putString("errorInfo", e.message)
                FirebaseAnalytics.getInstance(getApplication())
                    .logEvent("Paid_Server_Fetch_User_Error", params)
                if (updateLoading) {
                    isLoading.postValue(false)
                }
                baseErrorHandle(e.code(), activity)
            } catch (e: Exception) {
                Log.e(TAG, "fetch user error with Exception", e)
                if (updateLoading) {
                    isLoading.postValue(false)
                }
            }
        }
    }

    fun register(
        username: String,
        fullName: String,
        email: String,
        password: String,
        repassword: String,
        birthDay: String,
        timeZone: String,
        captchaAnswer: Int,
        captchaSecret: String
    ) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val isPro = !App.getInstance().dataUtil.hasAds()
                userApiService.register(
                    RegisterRequest(
                        username,
                        fullName,
                        email,
                        password,
                        repassword,
                        birthDay,
                        timeZone,
                        language = Locale.getDefault().language,
                        userPlatform = PARAMS_USER_PLATFORM,
                        captcha = Captcha(answer = captchaAnswer, secret = captchaSecret)
                    ),
                    version = if (isPro) "pro" else null
                )
                isRegisterSuccess.postValue(true)
            } catch (e: HttpException) {
                Log.d(TAG, "Got HttpException when register", e)
                val errorResponse = JSONObject(e.response()?.errorBody().toString())
                if (errorResponse.has("errorList")) {
                    errorList.value = errorResponse.get("errorList") as JSONObject
                }
                isRegisterSuccess.postValue(false)
            } catch (e: Exception) {
                Log.d(TAG, "Got exception when register", e)
                isRegisterSuccess.postValue(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun activateUser(userId: String, activateCode: String) {
        isLoading.value = true
        isUserActivated.value = false
        viewModelScope.launch {
            try {
                val userActivateResponse = userApiService.activateUser(userId, activateCode)
                isUserActivated.postValue(userActivateResponse.result)
                userActivateResponse.errorCode.let {
                    errorCode = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when activate user", e)
                isUserActivated.postValue(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun forgotPassword(usernameOrEmail: String, captchaSecret: String, captchaAnswer: Int) {
        isLoading.value = true
        isForgotPassSuccess.value = false
        viewModelScope.launch {
            try {
                userApiService.forgotPassword(ForgotPasswordRequest(usernameOrEmail, captchaSecret, captchaAnswer))
                isForgotPassSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when forgot password", e)
                isForgotPassSuccess.postValue(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun checkResetPassToken(resetPassToken: String) {
        viewModelScope.launch {
            try {
                userApiService.checkResetPassToken(resetPassToken)
                isValidResetPassToken.postValue(true)
            } catch (e: Exception) {
                Log.e(TAG,"Got exception when check reset pass token", e)
                isValidResetPassToken.postValue(false)
            }
        }
    }

    fun resetPassword(resetPassToken: String, newPassword: String, renewPassword: String) {
        isLoading.value = true
        isPasswordReset.value = false
        viewModelScope.launch {
            try {
                userApiService.resetPassword(ResetPasswordRequest(resetPassToken, newPassword, renewPassword))
                isPasswordReset.postValue(true)
            } catch (e: HttpException) {
                Log.d(TAG, "Got HttpException when reset password", e)
                val errorResponse = JSONObject(e.response()?.errorBody().toString())
                if (errorResponse.has("errorList")) {
                    errorList.postValue(errorResponse.get("errorList") as JSONObject)
                }
                isPasswordReset.postValue(false)
            } catch (e: Exception) {
                Log.d(TAG, "Got exception when reset password", e)
                isPasswordReset.postValue(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun changePass(password: String, newPassword: String, activity: Activity) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                userApiService.changePass(ChangePasswordRequest(password, newPassword))
                localLogout(activity)
                Toast.makeText(
                    activity,
                    activity.getText(R.string.password_changed),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG,"Got exception when change pass", e)
                Toast.makeText(
                    activity,
                    activity.getText(R.string.incorrect_current_password),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun updateProfile(fullName: String, birthDay: String, timeZone: String, requestListener: RequestListener) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val updateProfileResponse = userApiService.updateProfile(UpdateProfileRequest(fullName, birthDay, timeZone))
                if (updateProfileResponse.result) {
                    requestListener.onSuccess(updateProfileResponse.user)
                } else {
                    requestListener.onError("")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "Got HttpException when update profile.", e)
                requestListener.onError("")
            } catch (e: Exception) {
                Log.e(TAG, "Got Exception when update profile.", e)
                requestListener.onError("")
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun deleteAccount(requestListener: RequestListener) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                userApiService.delete()
                requestListener.onSuccess(true)
            } catch (e: HttpException) {
                Log.e(TAG, "Got exception when delete account", e)
                requestListener.onSuccess(false)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun getCaptcha(requestListener: RequestListener) {
        viewModelScope.launch {
            try {
                val captchaResponse = userApiService.getCaptcha()
                requestListener.onSuccess(captchaResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get captcha", e)
                requestListener.onError(e.message)
            }
        }
    }
}
