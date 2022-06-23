package de.devin.monity.network.auth

/**
 * Model authentication levels
 */
enum class AuthLevel(val nextLevel: AuthLevel?, val weight: Int) {


    /**
     * Able to perform any admin tasks
     */
    AUTH_LEVEL_ADMIN(null, 3),

    /**
     * Able to perform any user tasks
     */
    AUTH_LEVEL_USER(AUTH_LEVEL_ADMIN, 2),

    /**
     * Able to log in
     */
    AUTH_LEVEL_NONE(AUTH_LEVEL_USER, 1)

}