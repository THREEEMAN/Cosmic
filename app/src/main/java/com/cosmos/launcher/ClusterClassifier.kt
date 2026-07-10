package com.cosmos.launcher

object ClusterClassifier {
    private val clusterMap = mapOf(
        // Work
        "com.google.android.gm" to "work",
        "com.google.android.calendar" to "work",
        "com.slack" to "work",
        "notion.id" to "work",
        "com.microsoft.office.word" to "work",
        "com.microsoft.office.excel" to "work",

        // Social
        "com.whatsapp" to "social",
        "com.instagram.android" to "social",
        "org.telegram.messenger" to "social",
        "com.facebook.katana" to "social",
        "com.twitter.android" to "social",

        // Creative
        "com.spotify.music" to "creative",
        "com.google.android.youtube" to "creative",
        "com.figma.android" to "creative",
        "com.adobe.lightroom" to "creative",
        "com.adobe.photoshop" to "creative",

        // Utility
        "com.google.android.maps" to "utility",
        "com.google.android.apps.maps" to "utility",
        "com.android.settings" to "utility",
        "com.google.android.apps.docs" to "utility",
        "com.google.android.apps.docs.editors.sheets" to "utility",
        "com.google.android.apps.docs.editors.slides" to "utility",

        // Finance
        "com.revolut.revolutmoney" to "finance",
        "com.paypal.android.p2pmobile" to "finance",
        "com.coinbase.android" to "finance",
        "com.andrognito.cryptonator" to "finance"
    )

    fun classify(packageName: String): String {
        return clusterMap[packageName] ?: guessCluster(packageName)
    }

    internal fun guessCluster(packageName: String): String {
        return when {
            packageName.contains("mail") || packageName.contains("email") || packageName.contains("calendar") || packageName.contains("slack") -> "work"
            packageName.contains("whatsapp") || packageName.contains("messenger") || packageName.contains("telegram") || packageName.contains("instagram") || packageName.contains("twitter") -> "social"
            packageName.contains("spotify") || packageName.contains("music") || packageName.contains("youtube") || packageName.contains("video") || packageName.contains("adobe") -> "creative"
            packageName.contains("bank") || packageName.contains("finance") || packageName.contains("paypal") || packageName.contains("revolut") || packageName.contains("crypto") -> "finance"
            else -> "utility"
        }
    }
}
