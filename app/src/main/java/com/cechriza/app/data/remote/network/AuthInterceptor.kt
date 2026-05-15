package com.cechriza.app.data.remote.network


import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    companion object {
        private const val COMPROMISO_API_KEY = "cmp_4fb3ed3e0005c4d1ec974320f9a26a78c286ab03fe66e27a854bcd35dcc0cd6f"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        if (originalRequest.header("Accept").isNullOrBlank()) {
            requestBuilder.addHeader("Accept", "application/json")
        }
        requestBuilder.addHeader("X-API-Key", COMPROMISO_API_KEY)
        tokenProvider()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
