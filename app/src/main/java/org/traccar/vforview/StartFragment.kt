/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")
package org.traccar.vforview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.preference.PreferenceManager
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class StartFragment : Fragment(), View.OnClickListener {

    private lateinit var serverField: EditText
    private lateinit var startButton: Button
    private lateinit var serverOptionSpinner: Spinner
    private lateinit var serverValue: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)
//        serverField = view.findViewById(R.id.field_server)
        startButton = view.findViewById(R.id.button_start)
        serverOptionSpinner = view.findViewById(R.id.spinner_server_options)
        startButton.setOnClickListener(this)
        setArrayAdapter(context = activity.applicationContext)
        spinnerSelectAction()
        return view
    }

    @SuppressLint("StaticFieldLeak")
    override fun onClick(view: View) {
        startButton.isEnabled = false
        object : AsyncTask<String, Unit, Boolean>() {
            override fun doInBackground(vararg urls: String): Boolean {
                try {
                    val uri = Uri.parse(urls[0]).buildUpon().appendEncodedPath("api/server").build()
                    var url = uri.toString()
                    var urlConnection: HttpURLConnection? = null
                    for (i in 0 until MAX_REDIRECTS) {
                        val resourceUrl = URL(url)
                        urlConnection = resourceUrl.openConnection() as HttpURLConnection
                        urlConnection.instanceFollowRedirects = false
                        when (urlConnection.responseCode) {
                            HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                                url = urlConnection.getHeaderField("Location")
                                continue
                            }
                        }
                        break
                    }
                    val reader = BufferedReader(InputStreamReader(urlConnection?.inputStream))
                    var line: String?
                    val responseBuilder = StringBuilder()
                    while (reader.readLine().also { line = it } != null) {
                        responseBuilder.append(line)
                    }
                    JSONObject(responseBuilder.toString())
                    return true
                } catch (e: IOException) {
                    Log.w(TAG, e)
                } catch (e: JSONException) {
                    Log.w(TAG, e)
                }
                return false
            }

            override fun onPostExecute(result: Boolean) {
                if (activity != null) {
                    if (result) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
            }
        }.execute(serverValue)
    }

    private fun setArrayAdapter(context: Context) {
        ArrayAdapter.createFromResource(
            context,
            R.array.server_options_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            serverOptionSpinner.adapter = adapter
            matchServerOptionWithValue(
                selectedValue = serverOptionSpinner.adapter.getItem(0).toString()
            )
        }
    }

    private fun  spinnerSelectAction() {
        // Set an item selected listener on the spinner
        serverOptionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Get the selected item
                val selectedValue = parent.getItemAtPosition(position).toString()
                Log.d("App", "Selected Server: $selectedValue")
                matchServerOptionWithValue(
                    selectedValue = parent.getItemAtPosition(position).toString()
                )
                Log.d("App", "Selected Server Value: $serverValue")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
    }

    private fun matchServerOptionWithValue(selectedValue: String) {
        when(selectedValue) {
            "Server 1" -> {
                serverValue = AppConfig.Server.SERVER_ONE
            }
            "Server 2" -> {
                serverValue = AppConfig.Server.SERVER_TWO
            }
            "Server 3" -> {
                serverValue = AppConfig.Server.SERVER_THREE
            }
            "Server 4" -> {
                serverValue = AppConfig.Server.SERVER_FOUR
            }
            "Server 5" -> {
                serverValue = AppConfig.Server.SERVER_FIVE
            }
            "Server 6" -> {
                serverValue = AppConfig.Server.SERVER_SIX
            }
        }
    }

    private fun onSuccess() {
        PreferenceManager.getDefaultSharedPreferences(activity)
            .edit().putString(MainActivity.PREFERENCE_URL, serverValue).apply()
        activity.fragmentManager
            .beginTransaction().replace(android.R.id.content, MainFragment())
            .commitAllowingStateLoss()
    }

    private fun onError() {
        startButton.isEnabled = true
        val alertDialog = AlertDialog.Builder(activity).create()
        alertDialog.setMessage(getString(R.string.error_connection))
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(android.R.string.ok)) { dialog, _ -> dialog.dismiss() }
        alertDialog.show()
    }

    companion object {
        private val TAG = StartFragment::class.java.simpleName
        private const val MAX_REDIRECTS = 5
    }
}
