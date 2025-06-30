package com.stiffrock.wakatimewidgets.data

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

// API interface
interface GithubService {
    @GET("github/linguist/master/lib/linguist/languages.yml")
    suspend fun getLanguagesYaml(): String
}

object LanguageService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()

    val githubService: GithubService = retrofit.create(GithubService::class.java)
}