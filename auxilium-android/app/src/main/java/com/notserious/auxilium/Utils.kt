package com.notserious.auxilium

class Utils {
    companion object {
        val API_HOST = "http://35.235.101.252:5000"
        val API_REMOVE_ALERT = "$API_HOST/update_patient"

        fun getLatestAidLink(aiderName: String): String {
            return "$API_HOST/$aiderName"
        }

        val GOOGLE_MAP_API_KEY = "AIzaSyD9NIfjjr2sVjhuZV_WwSrmcl1UbXXpwa0"

        fun getMapPictureLink(lat: Double, long: Double): String {
            val link = "https://maps.googleapis.com/maps/api/staticmap?" +
                    "center=$lat,$long&zoom=13&size=1200x600&markers=color:red%7C$lat,$long&key=$GOOGLE_MAP_API_KEY"
            return link
        }

        fun getAddress(lat: Double, long: Double): String {
            val link = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$long&key=$GOOGLE_MAP_API_KEY"
            return link
        }
    }
}