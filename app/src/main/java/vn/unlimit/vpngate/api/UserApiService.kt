package vn.unlimit.vpngate.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import vn.unlimit.vpngate.models.User
import vn.unlimit.vpngate.models.request.ChangePasswordRequest
import vn.unlimit.vpngate.models.request.ForgotPasswordRequest
import vn.unlimit.vpngate.models.request.RegisterRequest
import vn.unlimit.vpngate.models.request.ResetPasswordRequest
import vn.unlimit.vpngate.models.request.UpdateProfileRequest
import vn.unlimit.vpngate.models.request.UserLoginRequest
import vn.unlimit.vpngate.models.response.CaptchaResponse
import vn.unlimit.vpngate.models.response.UpdateProfileResponse
import vn.unlimit.vpngate.models.response.UserActivateResponse
import vn.unlimit.vpngate.models.response.UserLoginResponse

interface UserApiService {
    @POST("user/login")
    suspend fun login(@Body userLoginRequest: UserLoginRequest): UserLoginResponse

    @GET("user/get")
    suspend fun fetchUser(): User

    @GET("user/logout")
    suspend fun logout()

    @DELETE("user/delete")
    suspend fun delete()

    @POST("user/profile")
    suspend fun updateProfile(@Body updateProfileRequest: UpdateProfileRequest): UpdateProfileResponse

    @POST("user/password/change")
    suspend fun changePass(@Body changePasswordRequest: ChangePasswordRequest)

    @POST("user/password-reset")
    suspend fun resetPassword(@Body resetPasswordRequest: ResetPasswordRequest)

    @GET("user/{userId}/activate/{activateCode}")
    suspend fun activateUser(
        @Path("userId") userId: String,
        @Path("activateCode") activateCode: String
    ): UserActivateResponse

    @POST("user/password/forgot")
    suspend fun forgotPassword(@Body forgotPasswordRequest: ForgotPasswordRequest)

    @GET("user/password-reset/{resetPassToken}")
    suspend fun checkResetPassToken(@Path("resetPassToken") resetPassToken: String)

    @POST("user/register")
    suspend fun register(@Body registerRequest: RegisterRequest, @Query("version") version: String?)

    @GET("user/captcha")
    suspend fun getCaptcha(
        @Query("width") width: Int? = 120,
        @Query("height") height: Int? = 90,
        @Query("fontSize") fontSize: Int? = 60
    ): CaptchaResponse
}