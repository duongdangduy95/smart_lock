package com.example.smartlock.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://fgcnyrlnlqjvnvddzvfh.supabase.co",
        supabaseKey = "sb_secret_91nmxAZuoGfYns0a4dLkhA_H-XXfmFF"
    ) {
        install(Postgrest)
        install(Realtime)
    }
}
